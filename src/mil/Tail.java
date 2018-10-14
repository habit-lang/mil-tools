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

public abstract class Tail {

  /** Test to determine whether a given tail expression has no externally visible side effect. */
  public boolean hasNoEffect() {
    return false;
  }

  /** Test if this Tail expression includes a free occurrence of a particular variable. */
  public abstract boolean contains(Temp w);

  /**
   * Test if this Tail expression includes an occurrence of any of the variables listed in the given
   * array.
   */
  public abstract boolean contains(Temp[] ws);

  /** Add the variables mentioned in this tail to the given list of variables. */
  public abstract Temps add(Temps vs);

  /** Test if two Tail expressions are the same. */
  public abstract boolean sameTail(Tail that);

  boolean sameSel(Sel that) {
    return false;
  }

  boolean sameReturn(Return that) {
    return false;
  }

  boolean sameEnter(Enter that) {
    return false;
  }

  boolean sameBlockCall(BlockCall that) {
    return false;
  }

  boolean samePrimCall(PrimCall that) {
    return false;
  }

  boolean sameDataAlloc(DataAlloc that) {
    return false;
  }

  boolean sameClosAlloc(ClosAlloc that) {
    return false;
  }

  /** Find the dependencies of this AST fragment. */
  public abstract Defns dependencies(Defns ds);

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out, null);
    out.flush();
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public abstract void dump(PrintWriter out, Temps ts);

  /** Display a Tail value and then move to the next line. */
  public void displayln(PrintWriter out, Temps ts) {
    dump(out, ts);
    out.println();
  }

  /**
   * Apply a TempSubst to this Tail. As an optimization, we skip the operation if the substitution
   * is empty.
   */
  public Tail apply(TempSubst s) {
    return (s == null) ? this : forceApply(s);
  }

  /**
   * Apply a TempSubst to this Tail. A call to this method, even if the substitution is empty, will
   * force the construction of a new Tail.
   */
  public abstract Tail forceApply(TempSubst s);

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  abstract TVars tvars(TVars tvs);

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public abstract Type resultType();

  abstract Type inferType(Position pos) throws Failure;

  /**
   * Generate code for a Tail that appears as a regular call (i.e., in the initial part of a code
   * sequence). The parameter o specifies the offset for the next unused location in the current
   * frame; this will also be the first location where we can store arguments and results.
   */
  abstract void generateCallCode(MachineBuilder builder, int o);

  /**
   * Generate code for a Tail that appears in tail position (i.e., at the end of a code sequence).
   * The parameter o specifies the offset of the next unused location in the current frame. For
   * BlockCall and Enter, in particular, we can jump to the next function instead of doing a call
   * followed by a return.
   */
  abstract void generateTailCode(MachineBuilder builder, int o);

  /** Test to determine if this Tail is a BlockCall. */
  public BlockCall isBlockCall() {
    return null;
  }

  /**
   * Test to determine if this Tail is an expression of the form (w @ a1,...,an) for some given w,
   * and any a1,...,an (so long as they do not include w), returning the argument list a1,...,an as
   * a result.
   */
  public Atom[] enters(Temp w) {
    return null;
  }

  /**
   * Return the associated constructor function if this is a data allocator without any arguments.
   */
  Cfun cfunNoArgs() {
    return null;
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return false;
  }

  boolean detectLoops(Block src, Blocks visited) {
    return false;
  }

  /**
   * Return true if this code enters a non-productive black hole (i.e., immediately calls halt or
   * loop).
   */
  boolean blackholes() {
    return false;
  }

  /**
   * Test whether a given Code/Tail value is an expression of the form return vs, with the specified
   * Temp[] vs as parameter. We also return a true result for a Tail of the form return _, where the
   * wildcard indicates that any return value is acceptable because the result will be ignored by
   * the caller. This allows us to turn more calls in to tail calls when they occur at the end of
   * "void functions" that do not return a useful result.
   */
  boolean isReturn(Temp[] vs) {
    return false;
  }

  Code prefixInline(Block src, Temp[] rs, Code c) {
    return null;
  }

  /**
   * Perform suffix inlining on this tail, which either replaces a block call with an appropriately
   * renamed copy of the block's body, or else returns null if the tail is either not a block call,
   * or if the code of the block is not suitable for inlining.
   */
  Code suffixInline(Block src) {
    return null;
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return true;
  }

  boolean noinline() {
    return false;
  }

  /**
   * Captures a recurring pattern: t.thisUnless(tr) just returns this unless a non-null replacement
   * Tail, tr, is provided.
   */
  public Tail thisUnless(Tail t) {
    return (t == null) ? this : t;
  }

  /**
   * Skip goto blocks in a Tail (for a ClosureDefn or TopLevel). TODO: can this be simplified now
   * that ClosureDefns hold Tails rather than Calls?
   */
  public Tail inlineTail() {
    return this;
  }

  BlockCall bypassGotoBlockCall() {
    return null;
  }

  BlockCall isGoto(int numParams) {
    return null;
  }

  public Allocator isAllocator() {
    return null;
  }

  Tail liftStaticAllocator() {
    return this;
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  abstract Temps usedVars(Temps vs);

  Tail removeUnusedArgs() {
    return this;
  }

  /**
   * Test to determine whether a given tail expression may be repeatable (i.e., whether the results
   * of a previous use of the same tail can be reused instead of repeating the tail). TODO: is there
   * a better name for this?
   */
  public boolean isRepeatable() {
    return false;
  }

  /**
   * Test to determine whether a given tail expression is pure (no externally visible side effects
   * and no dependence on other effects).
   */
  public boolean isPure() {
    return false;
  }

  public Tail lookupFact(TopLevel tl) {
    return null;
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return null;
  }

  Atom[] returnsAtom() {
    return null;
  }

  Atom isBnot() {
    return null;
  }

  public Code rewrite(Facts facts) {
    return null;
  }

  Tail rewriteTail(Facts facts) {
    return this;
  }

  /** Liveness analysis. TODO: finish this comment. */
  abstract Temps liveness(Temps vs);

  Atom shortTopLevel(Top d, int i) {
    return d;
  }

  /**
   * Test to determine whether this Code/Tail value corresponds to a closure allocator, returning
   * either a ClosAlloc value, or else a null result.
   */
  ClosAlloc lookForClosAlloc() {
    return null;
  }

  /**
   * Figure out the BlockCall that will be used in place of the original after shorting out a Case.
   * Note that we require a DataAlloc fact for this to be possible (closures and monadic thunks
   * shouldn't show up here if the program is well-typed, but we'll check for this just in case).
   * Once we've established an appropriate DataAlloc, we can start testing each of the alternatives
   * to find a matching constructor, falling back on the default branch if no other option is
   * available.
   */
  BlockCall shortCase(TempSubst s, Alt[] alts, BlockCall d) {
    return null;
  }

  /**
   * Test to determine whether this Tail value corresponds to a data allocator, returning either a
   * DataAlloc value, or else a null result.
   */
  DataAlloc lookForDataAlloc() {
    return null;
  }

  /**
   * Test to see if this tail expression is a call to a specific primitive, returning null in the
   * (most likely) case that it is not.
   */
  Atom[] isPrim(Prim p) {
    return null;
  }

  /** Look for a rewrite of bnot(x) where x is the result of this Tail. */
  Code bnotRewrite() {
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  abstract int summary();

  /** Test to see if two Tail expressions are alpha equivalent. */
  abstract boolean alphaTail(Temps thisvars, Tail that, Temps thatvars);

  /** Test two items for alpha equivalence. */
  boolean alphaSel(Temps thisvars, Sel that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaReturn(Temps thisvars, Return that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaEnter(Temps thisvars, Enter that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaBlockCall(Temps thisvars, BlockCall that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaPrimCall(Temps thisvars, PrimCall that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaDataAlloc(Temps thisvars, DataAlloc that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaClosAlloc(Temps thisvars, ClosAlloc that, Temps thatvars) {
    return false;
  }

  void eliminateDuplicates() {
    /* nothing to do in most cases */
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  /**
   * Eliminate a call to a newtype constructor or selector in this Tail by replacing it with a tail
   * that simply returns the original argument of the constructor or selector.
   */
  Tail removeNewtypeCfun() { // Default case: return the original tail
    return this;
  }

  /** Generate a specialized version of this Tail. */
  abstract Tail specializeTail(MILSpec spec, TVarSubst s, SpecEnv env);

  Tail bitdataRewrite(BitdataMap m) {
    return this;
  }

  /** Return a closure k{}, where k{} [x1,...,xn] = this. */
  public Tail constClosure(Position pos, int n) {
    return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(n), this)).withArgs();
  }

  abstract Tail repTransform(RepTypeSet set, RepEnv env);

  /**
   * Apply a representation transformation to this Tail value in a context where the result of the
   * tail should be bound to the variables vs before continuing to execute the code in c. For the
   * most part, this just requires the construction of a new Bind object. Special treatment,
   * however, is required for Selectors that access data fields whose representation has been spread
   * over multiple locations. This requires some intricate matching to check for selectors using
   * bitdata names or layouts, neither of which require this special treatment.
   */
  Code repTransform(RepTypeSet set, RepEnv env, Temp[] vs, Code c) {
    return new Bind(vs, this.repTransform(set, env), c);
  }

  /**
   * Find the argument variables that are used in this Tail, adding results to an accumulating list.
   * This is mostly just the same as adding the the variables defined in the Tail except that we
   * include updates in the cases for BlockCall and ClosAlloc if the argument lists are not already
   * known.
   */
  Temps addArgs(Temps vs) throws Failure {
    return this.add(vs);
  }

  /**
   * Calculate an array of static values for this tail, or null if none of the results produced by
   * this tail have statically known values. (Either because they are truly not statically known, or
   * because we choose not to compute static values for certain forms of Tail.) If there are
   * multiple results, only some of which are statically known, then the array that is returned will
   * be non-null, but will have null values in places where static values are not known.
   */
  llvm.Value[] calcStaticValue(LLVMMap lm, llvm.Program prog) {
    // handles Enter, BlockCall, PrimCall, Sel (optimizer should remove static Sels), ...
    return null;
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* no non-tail calls here */
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    /* default is to do nothing: BlockCalls are the only Tails that contain calls. */
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return bs;
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return Label.noLabels;
  }

  /**
   * Generate LLVM code for a Bind of the form (vs <- this; c). The isTail parameter should only be
   * true if c is return vs.
   */
  llvm.Code toLLVMBind(
      LLVMMap lm, VarMap vm, TempSubst s, Temp[] vs, boolean isTail, Code c, Label[] succs) {
    return toLLVMBindTail(lm, vm, s, vs, isTail, c.toLLVMCode(lm, vm, s, succs));
  }

  /**
   * Generate LLVM code to evaluate this Tail (which must not be a Return), bind the results to the
   * variables in vs, and then execute the specified code. The isTail parameter should onlu be true
   * if the code is an immediate return.
   */
  llvm.Code toLLVMBindTail(
      LLVMMap lm, VarMap vm, TempSubst s, Temp[] vs, boolean isTail, llvm.Code code) {
    Temp[] nuvs = Temp.nonUnits(vs);
    if (nuvs.length == 0) { // Tail does not return any results
      return toLLVMBindVoid(
          lm, vm, s, isTail, code); // ... so just execute the tail, and then continue
    } else {
      llvm.Local lhs;
      if (nuvs.length == 1) { // Just one result?
        lhs = vm.lookup(lm, nuvs[0]); // ... save result directly
      } else { // Multiple results?
        lhs = vm.reg(lm.toLLVM(resultType())); // ... a register to hold the structure
        for (int n = nuvs.length;
            --n >= 0; ) { // ... and a sequence of extractvalues to access components
          code = new llvm.Op(vm.lookup(lm, nuvs[n]), new llvm.ExtractValue(lhs, n), code);
        }
      }
      return toLLVMBindCont(
          lm, vm, s, false, lhs, code); // ... execute tail, capture result, and continue
    }
  }

  /** Generate LLVM code to execute this Tail in tail call position (i.e., as part of a Done). */
  llvm.Code toLLVMDone(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    llvm.Type ty = lm.toLLVM(resultType()); // Find the type of the values that will be returned
    if (ty == llvm.Type.vd) { // Tail does not return any results
      return this.toLLVMBindVoid(
          lm,
          vm,
          s,
          true, // ... so execute the tail
          new llvm.RetVoid()); // ... and return without a result
    } else {
      llvm.Local lhs = vm.reg(ty);
      return this.toLLVMBindCont(
          lm,
          vm,
          s,
          true,
          lhs, // ... execute tail, capture result in lhs
          new llvm.Ret(lhs)); // ... and then return that result
    }
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  abstract llvm.Code toLLVMBindVoid(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c);

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  abstract llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c);

  /**
   * Worker function for generateRevInitCode, called when we have established that this tail
   * expression, for the given TopLevel, should be executed during program initialization.
   */
  llvm.Code revInitTail(LLVMMap lm, InitVarMap ivm, TopLevel tl, TopLhs[] lhs, llvm.Code code) {
    // This is the general case for any form of Tail other than a Return.
    Temp[] vs = new Temp[lhs.length];
    for (int i = 0; i < lhs.length; i++) {
      vs[i] = lhs[i].makeTemp();
    }
    code = llvm.Code.reverseOnto(toLLVMBindTail(lm, ivm, null, vs, false, null), code);
    return tl.initLLVMTopLhs(lm, ivm, vs, code);
  }
}
