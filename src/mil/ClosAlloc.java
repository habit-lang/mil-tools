/*
    Copyright 2018 Mark P Jones, Portland State University

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
import compiler.Failure;
import compiler.Position;
import core.*;
import java.io.PrintWriter;

public class ClosAlloc extends Allocator {

  private ClosureDefn k;

  /** Default constructor. */
  public ClosAlloc(ClosureDefn k) {
    this.k = k;
  }

  /** Test if two Tail expressions are the same. */
  public boolean sameTail(Tail that) {
    return that.sameClosAlloc(this);
  }

  boolean sameClosAlloc(ClosAlloc that) {
    return this.k == that.k && this.sameArgs(that);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return k.dependencies(super.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    dump(out, k.toString(), "{", args, "}", ts);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new ClosAlloc(k).withArgs(args);
  }

  private AllocType type;

  /** The type tuple that describes the outputs of this Tail. */
  private Type outputs;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public Type resultType() {
    return outputs;
  }

  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    type = k.instantiate();
    return outputs = Type.tuple(type.alloc(pos, inputs));
  }

  void invokeCall(MachineBuilder builder, int o) {
    builder.alloc(k, args.length, o);
    builder.store(o);
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.sameClosAllocForm(this);
  }

  boolean sameClosAllocForm(ClosAlloc that) {
    return that.k == this.k;
  }

  ClosAlloc deriveWithKnownCons(Call[] calls) {
    // if (calls!=null) { return null; } // disable optimization
    if (calls.length != args.length) {
      debug.Internal.error("ClosAlloc argument list length mismatch in deriveWithKnownCons");
    }
    ClosureDefn nk = k.deriveWithKnownCons(calls);
    if (nk == null) {
      return null;
    } else {
      ClosAlloc ca = new ClosAlloc(nk);
      ca.withArgs(specializedArgs(calls));
      return ca;
    }
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return k.usedVars(args, vs);
  }

  Tail removeUnusedArgs() {
    Atom[] nargs = k.removeUnusedArgs(args);
    return (nargs == null) ? this : new ClosAlloc(k).withArgs(nargs);
  }

  public Code rewrite(Facts facts) {
    Call[] calls = k.collectCalls(args, facts);
    if (calls != null) {
      ClosAlloc ca = deriveWithKnownCons(calls);
      if (ca != null) {
        MILProgram.report("deriving specialized block for ClosAlloc block " + k.getId());
        return new Done(ca);
      }
    }
    return null;
  }

  /**
   * Test to determine whether this Code/Tail value corresponds to a closure allocator, returning
   * either a ClosAlloc value, or else a null result.
   */
  ClosAlloc lookForClosAlloc() {
    return this;
  }

  /**
   * Compute a Tail that gives the result of applying this ClosAlloc value to a specified argument
   * a.
   */
  Tail enterWith(Atom[] fargs) {
    return k.withArgs(args, fargs);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return summary(k.summary()) * 33 + 3;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaClosAlloc(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaClosAlloc(Temps thisvars, ClosAlloc that, Temps thatvars) {
    return this.k == that.k && this.alphaArgs(thisvars, that, thatvars);
  }

  void eliminateDuplicates() {
    ClosureDefn k1 = k.getReplaceWith();
    if (k1 != null) {
      k = k1;
    }
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonAllocType(set);
    }
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new ClosAlloc(spec.specializedClosureDefn(k, type.apply(s)));
  }

  /**
   * Find the argument variables that are used in this Tail, adding results to an accumulating list.
   * This is mostly just the same as adding the the variables defined in the Tail except that we
   * include updates in the cases for BlockCall and ClosAlloc if the argument lists are not already
   * known.
   */
  Temps addArgs(Temps vs) throws Failure {
    return (args == null) ? Temps.add(args = k.addArgs(), vs) : vs;
  }

  /**
   * Create a reference to a statically allocated data structure corresponding to this Allocator,
   * having already established that all of the components (if any) are statically known.
   */
  llvm.Value staticAlloc(LLVMMap lm, llvm.Program prog, llvm.Value[] vals) {
    vals[0] = lm.globalFor(k); // add code pointer to start of object
    return staticAlloc(prog, vals, lm.closureLayoutType(k), k.closurePtrType(lm));
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    debug.Internal.error("ClosAlloc does not return void");
    return c;
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    llvm.Type objt = lm.closureLayoutType(k).ptr(); // type of a pointer to a k object
    llvm.Local obj = vm.reg(objt); // a register to point to the new object
    return alloc(
        lm,
        vm,
        s,
        objt,
        obj,
        lm.globalFor(k),
        new llvm.Op(lhs, new llvm.Bitcast(obj, k.closurePtrType(lm)), c));
  }
}
