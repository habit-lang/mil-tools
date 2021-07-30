/*
    Copyright 2018-19 Mark P Jones, Portland State University

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

/** Base class for representing MIL code sequences. */
public abstract class Code {

  /** Test for a free occurrence of a particular variable. */
  abstract boolean contains(Temp w);

  /** Find the dependencies of this AST fragment. */
  abstract Defns dependencies(Defns ds);

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out, null);
    out.flush();
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  abstract void dump(PrintWriter out, Temps ts);

  /** Print an indent at the beginning of a line. */
  static final void indent(PrintWriter out) {
    out.print("  ");
  }

  /** Print a suitably indented string at the start of a line. */
  static final void indentln(PrintWriter out, String s) {
    indent(out);
    out.println(s);
  }

  /**
   * Apply a TempSubst to this Code sequence, forcing construction of a fresh copy of the input code
   * structure, including the introduction of new temporaries in place of any variables introduced
   * by Binds.
   */
  abstract Code apply(TempSubst s);

  /**
   * Return true if this code enters a non-productive black hole (i.e., immediately calls halt or
   * loop).
   */
  boolean blackholes() {
    return false;
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  abstract TVars tvars(TVars tvs);

  abstract Type inferType(Position pos) throws Failure;

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  abstract void generateCode(MachineBuilder builder, int o);

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  abstract Code deriveWithEnter(Atom[] iargs);

  /**
   * Determine whether it is possible to rewrite a code sequence of the form (ws <- bc; c)---where c
   * is this code sequence and ws and bc are passed as parameters---by deriving a new block with a
   * trailing enter.
   */
  Code enters(Temp[] ws, BlockCall bc) {
    return (ws.length == 1) ? enters(ws[0], bc) : null;
  }

  /**
   * Given an expression of the form (w <- b[..]; c), attempt to construct an equivalent code
   * sequence that instead calls a block whose code includes a trailing enter.
   */
  Code enters(Temp w, BlockCall bc) {
    return null;
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  abstract Code deriveWithCont(Atom cont);

  /**
   * Determine whether it is possible to rewrite a code sequence of the form (ws <- bc; c)---where c
   * is this code sequence and ws and bc are passed as parameters---by deriving a new block with a
   * continuation argument.
   */
  public Code casesOn(Temp[] ws, BlockCall bc) {
    return (ws.length == 1) ? casesOn(ws[0], bc) : null;
  }

  /**
   * Test to determine if this code is an expression of the form case v of alts where v is the
   * result of a preceding block call. If so, return a transformed version of the code that makes
   * use of a newly defined, known continuation that can subsequently be pushed in to the individual
   * branches of the case for further optimization.
   */
  public Code casesOn(Temp v, BlockCall bc) {
    return null;
  }

  Tail makeCont(Temp[] us) {
    // Build a block:  b[...] = c
    Code c = copy(); // make a copy of this code
    Temps vs = c.liveness(); // find the free variables
    Temp[] formals = Temps.toArray(vs); // create corresponding formal parameters
    Block b = new Block(BuiltinPosition.pos, formals, c); // TODO: different position?

    // Build a closure definition: k{...} us = b[...]
    Tail t = new BlockCall(b, formals);
    Temp[] stored = Temps.toArray(Temps.remove(us, vs));
    ClosureDefn k =
        new ClosureDefn(/*pos*/ null, stored, us, t); // define a new closure // TODO: fix position

    return new ClosAlloc(k).withArgs(stored);
  }

  boolean noCallsWithinSCC(DefnSCC scc) {
    return true;
  }

  abstract Code copy();

  /** Test for code that is guaranteed not to return. */
  abstract boolean doesntReturn();

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    return this;
  }

  /**
   * Test whether a given Code/Tail has the form return vs, with the specified Temp[] vs as its
   * argument list.
   */
  boolean isReturn(Temp[] vs) {
    return false;
  }

  boolean detectLoops(Block src, Blocks visited) {
    return false;
  }

  public static final int INLINE_ITER_LIMIT = 3;

  /**
   * Perform inlining on this Code sequence, looking for opportunities to: inline BlockCalls in both
   * Bind and Done nodes; and to skip goto blocks referenced from Case nodes. As a special case, we
   * do not apply inlining to the code of a block that contains a single Tail; any calls to that
   * block should be inlined anyway, at which point the block will become dead code. In addition,
   * expanding a single tail block like this could lead to code duplication, not least because we
   * also have another transformation (toBlockCall) that turns an expanded code sequence back into a
   * single block call, and we want to avoid oscillation back and forth between these two forms.
   */
  Code inlining(Block src) {
    return inlining(src, INLINE_ITER_LIMIT);
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  abstract Code inlining(Block src, int limit);

  Code prefixInline(TempSubst s, Temp[] us, Code d) {
    debug.Internal.error("This code cannot be inlined");
    return this;
  }

  int prefixInlineLength() {
    return prefixInlineLength(0);
  }

  int prefixInlineLength(int len) {
    return 0;
  }

  /**
   * Compute the length of this Code sequence for the purposes of prefix inlining. The returned
   * value is either the length of the code sequence (counting one for each Bind and Done node) or 0
   * if the code sequence ends with something other than Done. The argument should be initialized to
   * 0 for the first call.
   */
  abstract int suffixInlineLength(int len);

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  abstract boolean guarded(Block src);

  Tail forceToTail(Tail def) {
    return def;
  }

  /** Test to determine whether this Code is a Done. */
  public Tail isDone() {
    return null;
  }

  BlockCall isGoto(int numParams) {
    return null;
  }

  void liftAllocators() {
    /* Nothing to do in this case */
  }

  boolean liftAllocators(Bind parent) {
    return false;
  }

  /**
   * Find the list of variables that are used in this code sequence. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  abstract Temps usedVars();

  /** Optimize a Code block using a simple flow analysis. */
  public abstract Code flow(Defn d, Facts facts, TempSubst s);

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return null;
  }

  public abstract Code andThen(Temp[] vs, Code rest);

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  abstract Temps liveness();

  /**
   * Test to determine whether this Code/Tail value corresponds to a closure allocator, returning
   * either a ClosAlloc value, or else a null result.
   */
  ClosAlloc lookForClosAlloc() {
    return null;
  }

  /**
   * Test to see if this code is a Case that can be shorted out. Even if we find a Case, we still
   * need to check for a relevant item in the set of Facts (after applying a substitution that
   * captures the result of entering the block that starts with the Case). Again, if it turns out
   * that the optimization cannot be used, then we return null.
   */
  BlockCall shortCase(Temp[] params, Atom[] args, Facts facts) {
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
  abstract void calcSources(Defn d, Temp[] params, Temps rebound);

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  abstract int summary();

  /** Test to see if two Code sequences are alpha equivalent. */
  abstract boolean alphaCode(Temps thisvars, Code that, Temps thatvars);

  /** Test two items for alpha equivalence. */
  boolean alphaDone(Temps thisvars, Done that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaBind(Temps thisvars, Bind that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaAssert(Temps thisvars, Assert that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaIf(Temps thisvars, If that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaCase(Temps thisvars, Case that, Temps thatvars) {
    return false;
  }

  abstract void eliminateDuplicates();

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  /** Simplify uses of constructor functions in this code sequence. */
  abstract Code cfunSimplify();

  /** Generate a specialized version of this code sequence. */
  abstract Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env);

  abstract Code bitdataRewrite(BitdataMap m);

  abstract Code mergeRewrite(MergeMap mmap);

  abstract Code repTransform(RepTypeSet set, RepEnv env);

  /**
   * Return this code sequence as a Tail, generating a new block if necessary with the code as its
   * body.
   */
  public Tail forceTail(Position pos, Type type) {
    return new BlockCall(new lc.LCBlock(pos, type, this));
  }

  /** Find the argument variables that are used in this Code sequence. */
  public abstract Temps addArgs() throws Failure;

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  abstract void countCalls();

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  abstract Blocks identifyBlocks(Defn src, Blocks bs);

  /**
   * Determine whether this code sequence contains at most len Bind instructions before reaching a
   * Done. This is used as a heuristic for deciding whether it would be better to make a copy of the
   * code for a simple block than to incur the overhead of calling a single instance of the code as
   * a function.
   */
  boolean isSmall(int len) {
    return false;
  }

  /** Find the CFG successors for this MIL code fragment. */
  abstract Label[] findSuccs(CFG cfg, Node src);

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  abstract llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs);
}
