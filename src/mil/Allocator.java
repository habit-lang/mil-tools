/*
    Copyright 2018-26 Mark P Jones, Portland State University

    This file is part of mil-tools.

    mil-tools is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    mil-tools is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with mil-tools.  If not, see <https://www.gnu.org/licenses/>.
*/
package mil;

import compiler.*;
import core.*;

/** Represents a Call that allocates data in the heap. */
public abstract class Allocator extends Call {

  /**
   * Determine if a given tail expression has no externally visible side effects. This is true for
   * returns, allocators, and selections, and for calls to primitives that are not impure.
   */
  boolean hasNoEffect() {
    return true;
  }

  /** Test whether this Tail is an Allocator. */
  public Allocator isAllocator() {
    return this;
  }

  /** Lift a static allocator in this Tail to a top-level definition. */
  Tail liftStaticAllocator(Position pos) {
    for (int i = 0; i < args.length; i++) {
      if (!args[i].isStatic()) {
        // Rewrite does not apply if even one argument is not static
        return this;
      }
    }
    TopLevel topLevel = new TopLevel(pos, new TopLhs(), this);
    MILProgram.report("lifting static allocator to top-level " + topLevel);
    return new Return(new TopDef(topLevel, 0));
  }

  /**
   * Test whether a given tail expression may be repeatable (i.e., whether the results of a previous
   * use of the same tail can be reused instead of repeating the tail). TODO: is there a better name
   * for this?
   */
  public boolean isRepeatable() {
    // This is an example of Tail that is repeatable.
    return true;
  }

  /**
   * Test whether a given tail expression is pure (no externally visible side effects and no
   * dependence on other effects).
   */
  public boolean isPure() {
    // This is an example of a Tail that is pure.
    return true;
  }

  public Tail lookupFact(TopLevel tl) {
    this.tl = tl;
    return this;
  }

  protected TopLevel tl = null;

  public TopLevel getTopLevel() {
    return tl;
  }

  abstract boolean isRecursive();

  /**
   * Calculate an array of static values for this tail, or null if none of the results produced by
   * this tail have statically known values. (Either because they are truly not statically known, or
   * because we choose not to compute static values for certain forms of Tail.) If there are
   * multiple results, only some of which are statically known, then the array that is returned will
   * be non-null, but will have null values in places where static values are not known.
   */
  llvm.Value[] calcStaticValue(LLVMMap lm, llvm.Program prog) {
    llvm.Value[] comps = null; // lazily allocated array of components
    Atom[] nuargs = Atom.nonUnits(args);
    int n = nuargs.length;
    if (n <= 0) {
      comps = new llvm.Value[1]; // leave space for the tag at index 0
    } else {
      llvm.Value v = nuargs[0].calcStaticValue();
      if (v == null) {
        // if any component is unknown, then so is the full allocator
        return null;
      }
      comps = new llvm.Value[1 + n]; // again, allow for tag at index 0
      comps[1] = v;
      for (int i = 1; i < n; i++) {
        if ((comps[1 + i] = nuargs[i].calcStaticValue()) == null) {
          return null;
        }
      }
    }
    return new llvm.Value[] {staticAlloc(lm, prog, comps)};
  }

  /**
   * Create a reference to a statically allocated data structure corresponding to this Allocator,
   * having already established that all of the components (if any) are statically known.
   */
  abstract llvm.Value staticAlloc(LLVMMap lm, llvm.Program prog, llvm.Value[] vals);

  /**
   * Create a reference to a statically allocated data structure with the given layout and object
   * types and array of component (statically known) values.
   */
  llvm.Value staticAlloc(llvm.Program prog, llvm.Value[] vals, llvm.Type layoutType) {
    // Create a constant containing all the fields for this object:
    String valueName = prog.freshName("val");
    prog.add(new llvm.Constant(llvm.Mods.INTERNAL, valueName, new llvm.Struct(layoutType, vals)));

    /** Return a llvm global with a general pointer type for this value */
    return new llvm.Global(llvm.Type.ptr, valueName);
  }

  /**
   * Generate code to allocate space for an object of type objt, set obj to point to the start of
   * the (uninitialized) memory, fill in the tag and fields, and then continue with the code in c.
   */
  llvm.Code alloc(
      LLVMMap lm,
      VarMap vm,
      TempSubst s,
      llvm.Type objt,
      llvm.Local obj,
      llvm.Value tag,
      llvm.Code c) {
    // NOTE: The following steps build up the desired code in reverse order
    // of execution
    Atom[] nuargs = Atom.nonUnits(args);

    // - Save the fields in the object allocated at the address in obj:
    int n = nuargs.length;
    if (n > 0) {
      while (--n >= 0) {
        c = storeField(vm, s, objt, obj, n + 1, nuargs[n].toLLVMAtom(lm, vm, s), c);
      }
      c = new llvm.CodeComment("initialize other fields", c);
    }

    // - Save the object tag:
    c = new llvm.CodeComment("set the tag", storeField(vm, s, objt, obj, 0, tag, c));

    // - Allocate space for a new object:
    // pointer to first address past a c object starting at 0
    llvm.Local past = vm.reg(llvm.Type.ptr);
    // integer holding the size of a c object
    llvm.Local size = vm.reg(llvm.Type.word());
    llvm.Rhs call = new llvm.Call(llvm.Type.ptr, lm.allocFuncGlobal(), new llvm.Value[] {size});

    return new llvm.CodeComment(
        "calculate the number of bytes that we need to allocate",
        new llvm.Op(
            past,
            new llvm.Getelementptr(objt, new llvm.Null(), new llvm.Word(1)),
            new llvm.Op(
                size,
                new llvm.PtrToInt(past, size.getType()),
                new llvm.CodeComment(
                    "allocate memory for the object", new llvm.Op(obj, call, c)))));
  }

  /** Generate code to execute lhs[n] = v; c */
  static llvm.Code storeField(
      VarMap vm, TempSubst s, llvm.Type objt, llvm.Value lhs, int n, llvm.Value v, llvm.Code c) {
    llvm.Local addr = vm.reg(llvm.Type.ptr);
    return new llvm.Op(
        addr,
        new llvm.Getelementptr(objt, lhs, llvm.Word.ZERO, new llvm.Index(n)),
        new llvm.Store(v, addr, c));
  }
}
