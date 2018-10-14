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

public class Return extends Call {

  public Return() {
    // for creating Returns whose args will be filled in later ...
  }

  public Return(Atom a) {
    this.args = new Atom[] {a};
  }

  public Return(Atom[] as) {
    this.args = as;
  }

  /** Test to determine whether a given tail expression has no externally visible side effect. */
  public boolean hasNoEffect() {
    return true;
  }

  /** Test if two Tail expressions are the same. */
  public boolean sameTail(Tail that) {
    return that.sameReturn(this);
  }

  boolean sameReturn(Return that) {
    return Atom.sameAtoms(this.args, that.args);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    out.print("return ");
    Atom.displayTuple(out, args, ts);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new Return(args);
  }

  /** The type tuple that describes the outputs of this Tail. */
  private Type outputs;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return outputs.tvars(tvs);
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public Type resultType() {
    return outputs;
  }

  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    return outputs = Type.tuple(inputs);
  }

  void invokeCall(MachineBuilder builder, int o) {
    /* Arguments on stack; nothing further required. */
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.sameReturnForm(this);
  }

  boolean sameReturnForm(Call that) {
    return Atom.sameAtoms(that.args, this.args);
  }

  /**
   * Test whether a given Code/Tail value is an expression of the form return vs, with the specified
   * Temp[] vs as parameter. We also return a true result for a Tail of the form return _, where the
   * wildcard indicates that any return value is acceptable because the result will be ignored by
   * the caller. This allows us to turn more calls in to tail calls when they occur at the end of
   * "void functions" that do not return a useful result.
   */
  boolean isReturn(Temp[] vs) { // TODO: Refactor as separate sameTemps() method (viz sameAtoms)?
    if (vs.length != args.length) {
      return false;
    }
    for (int i = 0; i < vs.length; i++) {
      if (args[i]
          != vs[
              i]) { // TODO: is this equality enough?  (e.g., does it compare two TopDefs
                    // correctly?)
        return false;
      }
    }
    return true;
  }

  /**
   * Test to determine whether a given tail expression may be repeatable (i.e., whether the results
   * of a previous use of the same tail can be reused instead of repeating the tail). TODO: is there
   * a better name for this?
   */
  public boolean isRepeatable() {
    return true;
  }

  /**
   * Test to determine whether a given tail expression is pure (no externally visible side effects
   * and no dependence on other effects).
   */
  public boolean isPure() {
    return true;
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return args.length == 1 ? args[0].returnsFlag() : null;
  }

  Atom[] returnsAtom() {
    return args;
  }

  Atom shortTopLevel(Top d, int i) {
    if (i < 0 || i >= args.length) {
      debug.Internal.error("Index for shortTopLevel is out of bounds");
    }
    MILProgram.report("replacing reference to top level " + d + " with " + args[i]);
    return args[i];
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return summary(1);
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaReturn(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaReturn(Temps thisvars, Return that, Temps thatvars) {
    return this.alphaArgs(thisvars, that, thatvars);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) { // Sufficient for Return
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Return();
  }

  /**
   * Calculate an array of static values for this tail, or null if none of the results produced by
   * this tail have statically known values. (Either because they are truly not statically known, or
   * because we choose not to compute static values for certain forms of Tail.) If there are
   * multiple results, only some of which are statically known, then the array that is returned will
   * be non-null, but will have null values in places where static values are not known.
   */
  llvm.Value[] calcStaticValue(LLVMMap lm, llvm.Program prog) {
    llvm.Value vals[] = null;
    for (int i = 0; i < args.length; i++) {
      llvm.Value v = args[i].calcStaticValue();
      if (v != null) {
        if (vals == null) { // lazily allocate array, knowing now that it is needed
          vals = new llvm.Value[args.length];
        }
        vals[i] = v;
      }
    }
    return vals;
  }

  /**
   * Generate LLVM code for a Bind of the form (vs <- this; c). The isTail parameter should only be
   * true if c is return vs.
   */
  llvm.Code toLLVMBind(
      LLVMMap lm, VarMap vm, TempSubst s, Temp[] vs, boolean isTail, Code c, Label[] succs) {
    return c.toLLVMCode(lm, vm, TempSubst.extend(vs, args, s), succs);
  }

  /** Generate LLVM code to execute this Tail in tail call position (i.e., as part of a Done). */
  llvm.Code toLLVMDone(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    Atom[] nuargs = Atom.nonUnits(args);
    if (nuargs.length == 0) {
      return new llvm.RetVoid();
    } else if (nuargs.length == 1) {
      return new llvm.Ret(nuargs[0].toLLVMAtom(lm, vm, s));
    } else {
      llvm.Value[] vals = new llvm.Value[nuargs.length];
      for (int i = 0; i < nuargs.length; i++) {
        vals[i] = nuargs[i].toLLVMAtom(lm, vm, s);
      }
      return new llvm.Ret(new llvm.Struct(lm.toLLVM(outputs), vals));
    }
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    debug.Internal.error("A void Return should have been eliminated");
    return c; // Although this return value is actually correct for [] <- return []; c ...
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    debug.Internal.error("Return should have been eliminated");
    return c;
  }

  /**
   * Worker function for generateRevInitCode, called when we have established that this tail
   * expression, for the given TopLevel, should be executed during program initialization.
   */
  llvm.Code revInitTail(LLVMMap lm, InitVarMap ivm, TopLevel tl, TopLhs[] lhs, llvm.Code code) {
    // This is a special case for TopLevels that have a return expression for their tail
    // (essentially copying another
    // value that is defined at the top-level); we cannot use toLLVMBindVoid(), toLLVMBindCont(), or
    // toLLVMBindTail()
    // in this case.
    return tl.initLLVMTopLhs(lm, ivm, args, code);
  }
}
