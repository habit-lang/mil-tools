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
import core.*;
import java.io.PrintWriter;

/** Represents a code sequence that just executes a single Tail. */
public class Done extends Code {

  /** The tail to be executed. */
  private Tail t;

  /** Default constructor. */
  public Done(Tail t) {
    this.t = t;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return t.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return t.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    indent(out);
    t.displayln(out, ts);
  }

  /**
   * Apply a TempSubst to this Code sequence, forcing construction of a fresh copy of the input code
   * structure, including the introduction of new temporaries in place of any variables introduced
   * by Binds.
   */
  public Code apply(TempSubst s) {
    return new Done(t.apply(s));
  }

  /**
   * Return true if this code enters a non-productive black hole (i.e., immediately calls halt or
   * loop).
   */
  boolean blackholes() {
    return t.blackholes();
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return t.tvars(tvs);
  }

  Type inferType(Position pos) throws Failure { // t
    return t.inferType(pos);
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    t.generateTailCode(builder, o);
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    Temp f = new Temp();
    return new Bind(f, t, new Done(new Enter(f, iargs)));
  }

  /**
   * Given an expression of the form (w <- b[..]; c), attempt to construct an equivalent code
   * sequence that instead calls a block whose code includes a trailing enter.
   */
  public Code enters(Temp w, BlockCall bc) {
    Atom[] iargs = t.enters(w);
    return (iargs != null) ? new Done(bc.deriveWithEnter(iargs)) : null;
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    Temp v = new Temp();
    return new Bind(v, t, new Done(new Enter(cont, v)));
  }

  Code copy() {
    return new Done(t);
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return t.doesntReturn();
  }

  /**
   * Test whether a given Code/Tail has the form return vs, with the specified Temp[] vs as its
   * argument list.
   */
  boolean isReturn(Temp[] vs) {
    return t.isReturn(vs);
  }

  boolean detectLoops(Block src, Blocks visited) { // look for src[x] = b[x]
    return t.detectLoops(src, visited);
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    BlockCall bc = t.bypassGotoBlockCall();
    if (bc != null) {
      t = bc;
    }
    if (limit > 0) { // Is this an opportunity for suffix inlining?
      Code ic = t.suffixInline(src);
      if (ic != null) {
        return ic.inlining(src, limit - 1);
      }
    }
    return this;
  }

  Code prefixInline(TempSubst s, Temp[] us, Code d) {
    Tail nt = t.apply(s);
    return nt.blackholes() ? new Done(nt) : new Bind(us, nt, d);
  }

  int prefixInlineLength() {
    return 1;
  }

  int prefixInlineLength(int len) {
    return t.blackholes() ? 0 : (len + 1);
  }

  /**
   * Compute the length of this Code sequence for the purposes of prefix inlining. The returned
   * value is either the length of the code sequence (counting one for each Bind and Done node) or 0
   * if the code sequence ends with something other than Done. The argument should be initialized to
   * 0 for the first call.
   */
  int suffixInlineLength(int len) {
    return len + 1;
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return t.guarded(src);
  }

  Tail forceToTail(Tail def) {
    return t;
  }

  /** Test to determine whether this Code is a Done. */
  public Tail isDone() {
    return t;
  }

  BlockCall isGoto(int numParams) {
    return t.isGoto(numParams);
  }

  void liftAllocators() {
    t = t.liftStaticAllocator();
  }

  boolean liftAllocators(Bind parent) {
    t = t.liftStaticAllocator();
    return false;
  }

  /**
   * Find the list of variables that are used in this code sequence. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  Temps usedVars() {
    return t.usedVars(null);
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Defn d, Facts facts, TempSubst s) {
    t = t.apply(s);
    Code nc = t.rewrite(d, facts);
    return (nc == null) ? this : nc.flow(d, facts, s);
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return t.returnsFlag();
  }

  public Code andThen(Temp[] vs, Code rest) {
    return new Bind(vs, t, rest);
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() {
    return t.liveness(null);
  }

  /**
   * Test to determine whether this Code/Tail value corresponds to a closure allocator, returning
   * either a ClosAlloc value, or else a null result.
   */
  ClosAlloc lookForClosAlloc() {
    return t.lookForClosAlloc();
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
    t.calcSources(d, params, rebound);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return t.summary() * 17 + 3;
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaDone(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaDone(Temps thisvars, Done that, Temps thatvars) {
    return this.t.alphaTail(thisvars, that.t, thatvars);
  }

  void eliminateDuplicates() {
    t = t.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    t.collect(set);
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    t = t.removeNewtypeCfun();
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Done(t.specializeTail(spec, s, env));
  }

  Code bitdataRewrite(BitdataMap m) {
    return new Done(t.bitdataRewrite(m));
  }

  Code mergeRewrite(MergeMap mmap) {
    return new Done(t.mergeRewrite(mmap));
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    return new Done(t.repTransform(set, env));
  }

  /**
   * Return this code sequence as a Tail, generating a new block if necessary with the code as its
   * body.
   */
  public Tail forceTail(Position pos, Type type) {
    return t;
  }

  /** Find the argument variables that are used in this Code sequence. */
  public Temps addArgs() throws Failure {
    return t.addArgs(null);
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
    return t.identifyBlocks(src, bs);
  }

  /**
   * Determine whether this code sequence contains at most len Bind instructions before reaching a
   * Done. This is used as a heuristic for deciding whether it would be better to make a copy of the
   * code for a simple block than to incur the overhead of calling a single instance of the code as
   * a function.
   */
  boolean isSmall(int len) {
    return true;
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return t.findSuccs(cfg, src);
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return t.toLLVMDone(lm, vm, s, succs);
  }
}
