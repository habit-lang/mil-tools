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
import java.io.PrintWriter;

public abstract class Tail {

  /** Find the dependencies of this AST fragment. */
  abstract Defns dependencies(Defns ds);

  /** Add the temporaries used in this node to the given list of Temps. */
  abstract Temps add(Temps vs);

  /** Display a printable representation of this code sequence on the specified PrintWriter. */
  abstract void dump(PrintWriter out, Temps ts);

  /** Display a Tail value and then move to the next line. */
  void displayln(PrintWriter out, Temps ts) {
    dump(out, ts);
    out.println();
  }

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out, null);
    out.flush();
  }

  /** Test if two Tail expressions are the same. */
  abstract boolean sameTail(Tail that);

  /** Test whether this Tail is the same as the given Sel. */
  boolean sameSel(Sel that) {
    return false;
  }

  /** Test whether this Tail is the same as the given Return. */
  boolean sameReturn(Return that) {
    return false;
  }

  /** Test whether this Tail is the same as the given Enter. */
  boolean sameEnter(Enter that) {
    return false;
  }

  /** Test whether this Tail is the same as the given BlockCall. */
  boolean sameBlockCall(BlockCall that) {
    return false;
  }

  /** Test whether this Tail is the same as the given PrimCall. */
  boolean samePrimCall(PrimCall that) {
    return false;
  }

  /** Test whether this Tail is the same as the given DataAlloc. */
  boolean sameDataAlloc(DataAlloc that) {
    return false;
  }

  /** Test whether this Tail is the same as the given ClosAlloc. */
  boolean sameClosAlloc(ClosAlloc that) {
    return false;
  }

  /**
   * Apply a TempSubst to this Tail, forcing construction of a new Tail, even if the substitution is
   * empty.
   */
  abstract Tail apply(TempSubst s);

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  abstract TVars tvars(TVars tvs);

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  abstract Type resultType();

  /** Infer a type for this Tail expression. */
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

  /** Test if this Tail expression includes a free occurrence of a particular variable. */
  abstract boolean contains(Temp w);

  /**
   * Test if this Tail expression includes an occurrence of any of the variables listed in the given
   * array.
   */
  abstract boolean contains(Temp[] ws);

  /** Test whether this Tail is an BlockCall. */
  public BlockCall isBlockCall() {
    return null;
  }

  /**
   * Test to determine if this Tail is an expression of the form (w @ a1,...,an) for some given w,
   * and any a1,...,an (so long as they do not include w), returning the argument list a1,...,an as
   * a result.
   */
  Atom[] enters(Temp w) {
    return null;
  }

  boolean noCallsWithinSCC(DefnSCC scc) {
    return true;
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

  /**
   * Test whether a given Code/Tail has the form return vs, with the specified Temp[] vs as its
   * argument list.
   */
  boolean isReturn(Temp[] vs) {
    return false;
  }

  /**
   * Determine if a given tail expression has no externally visible side effects. This is true for
   * returns, allocators, and selections, and for calls to primitives that are not impure.
   */
  boolean hasNoEffect() {
    return false;
  }

  /**
   * Return true if this code enters a non-productive black hole (i.e., if it immediately calls halt
   * or loop).
   */
  boolean blackholes() {
    return false;
  }

  boolean detectLoops(Block src, Blocks visited) {
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
  Code suffixInline(Block d) {
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

  /** Test whether this Tail is an Allocator. */
  public Allocator isAllocator() {
    return null;
  }

  /** Lift a static allocator in this Tail to a top-level definition. */
  Tail liftStaticAllocator(Position pos) {
    return this;
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  abstract Temps usedVars(Temps vs);

  /**
   * Test whether a given tail expression may be repeatable (i.e., whether the results of a previous
   * use of the same tail can be reused instead of repeating the tail). TODO: is there a better name
   * for this?
   */
  public boolean isRepeatable() {
    // In general, Tails are not repeatable.
    return false;
  }

  /**
   * Test whether a given tail expression is pure (no externally visible side effects and no
   * dependence on other effects).
   */
  public boolean isPure() {
    // In general, Tails are not pure.
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
    return returnsFlag(3); // A small bound should be sufficient
  }

  /**
   * A simple test for MIL code fragments that return a known Flag; this is a helper for the method
   * without an argument that adds a counter that is decreased for every BlockCall encountered to
   * avoid triggering an unbounded loop in pathological corner cases.
   */
  Flag returnsFlag(int n) {
    return null;
  }

  Atom[] returnsAtom() {
    return null;
  }

  Atom isBnot() {
    return null;
  }

  public Code rewrite(Defn d, Facts facts) {
    // default behavior when no other case applies
    return null;
  }

  Tail rewriteTail(Defn d, Facts facts, boolean allowWord) {
    return this;
  }

  /** Liveness analysis. TODO: finish this comment. */
  abstract Temps liveness(Temps vs);

  Atom shortTopLevel(Top d, int i) {
    return d;
  }

  /**
   * Test if this Code/Tail value corresponds to a closure allocator, returning either a ClosAlloc
   * value, or else a null result.
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
  BlockCall shortCase(TempSubst s, Alts alts) {
    return null;
  }

  /**
   * Test whether this Tail value corresponds to a data allocator, returning either a DataAlloc
   * value, or else a null result.
   */
  DataAlloc lookForDataAlloc() {
    return null;
  }

  /**
   * Traverse abstract syntax in the context of a definition d with the given parameters as
   * contributions to calculating initial values for all Block and ClosureDefns in the program. We
   * use the rebound list to indicate when Temp values appearing in the parameters have been reused,
   * effectively shadowing the original parameter binding. In practice, we expect such uses to be
   * rare, and so the rebound list will usually be empty. But it is still important to check for
   * rebound parameters, just in case!
   */
  void calcSources(Defn d, Temp[] params, Temps rebound) {
    /* nothing to do */
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
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
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

  /**
   * Determine whether we should consider identifying TopLevels with this tail. We always avoid
   * identification for Returns because they will be shorted out where possible, but otherwise we
   * allow any pure tail.
   */
  boolean topLevelMayCombine() {
    return isPure();
  }

  Tail eliminateDuplicates() {
    return this;
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

  /** Rewrite this tail to use the bitdata representations provided by BitdataMap. */
  Tail bitdataRewrite(BitdataMap m) {
    return this;
  }

  /**
   * Generate a new version of this Tail to update references to constructor functions consistent
   * with the datatype equalities in the given MergeMap.
   */
  Tail mergeRewrite(MergeMap mmap) {
    return this;
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

  /** Return a closure k{}, where k{} [x1,...,xn] = this. */
  Tail constClosure(Position pos, int n) {
    return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(n), this)).withArgs();
  }

  /** Variant of constClosure that specifies the types of the (ignored) arguments. */
  Tail constClosure(Position pos, Type[] ts) {
    return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(ts), this)).withArgs();
  }

  /**
   * Find the argument variables that are used in this Tail, adding results to an accumulating list.
   * This is mostly just the same as adding the the variables defined in the Tail except that we
   * include updates in the cases for BlockCall and ClosAlloc if the argument lists are not already
   * known.
   */
  public Temps addArgs(Temps vs) throws Failure {
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
    // handles Enter, BlockCall, PrimCall, Sel (optimizer should remove
    // static Sels), ...
    return null;
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* no non-tail calls here */
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
    if (nuvs.length == 0) {
      // Tail does not return any results
      // ... so just execute the tail, and then continue
      return toLLVMBindVoid(lm, vm, s, isTail, code);
    } else {
      llvm.Local lhs;
      if (nuvs.length == 1) {
        // Just one result?
        // ... save result directly
        lhs = vm.lookup(lm, nuvs[0]);
      } else {
        // Multiple results?
        // ... then find a register for the structure
        lhs = vm.reg(lm.toLLVM(resultType()));
        // ... and use a sequence of extractvalues to access components
        for (int n = nuvs.length; --n >= 0; ) {
          code = new llvm.Op(vm.lookup(lm, nuvs[n]), new llvm.ExtractValue(lhs, n), code);
        }
      }
      // ... execute tail, capture result, and continue
      return toLLVMBindCont(lm, vm, s, false, lhs, code);
    }
  }

  /** Generate LLVM code to execute this Tail in tail call position (i.e., as part of a Done). */
  llvm.Code toLLVMDone(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    // Find the type of the values that will be returned:
    llvm.Type ty = lm.toLLVM(resultType());
    if (ty == llvm.Type.vd) {
      // Tail does not return any results, so execute the tail and
      // return without a result:
      return this.toLLVMBindVoid(lm, vm, s, true, new llvm.RetVoid());
    } else {
      // Otherwise execute tail, capture result in lhs and then return
      // that result:
      llvm.Local lhs = vm.reg(ty);
      return this.toLLVMBindCont(lm, vm, s, true, lhs, new llvm.Ret(lhs));
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
  llvm.Code revInitTail(LLVMMap lm, InitVarMap ivm, TopLevel tl, TopLhs[] lhs, llvm.Code edoc) {
    // This is the general case for any form of Tail other than a Return.
    Temp[] vs = new Temp[lhs.length];
    for (int i = 0; i < lhs.length; i++) {
      vs[i] = lhs[i].makeTemp();
    }
    edoc = llvm.Code.reverseOnto(toLLVMBindTail(lm, ivm, null, vs, false, null), edoc);
    return tl.initLLVMTopLhs(lm, ivm, vs, edoc);
  }
}
