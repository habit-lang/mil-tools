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

/**
 * Represents a code sequence that begins with an assertion that the specified atom has been
 * constructed using the given constructor function.
 */
public class Assert extends Code {

  /** An atom (presumably a variable). */
  private Atom a;

  /** The constructor function used to build a. */
  private Cfun cf;

  /** The rest of the code sequence. */
  private Code c;

  /** Default constructor. */
  public Assert(Atom a, Cfun cf, Code c) {
    this.a = a;
    this.cf = cf;
    this.c = c;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return a == w || c.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return c.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    indent(out);
    out.println("assert " + a.toString(ts) + " " + cf);
    c.dump(out, ts);
  }

  /**
   * Force the application of a TempSubst to this Code sequence, forcing construction of a fresh
   * copy of the input code structure, including the introduction of new temporaries in place of any
   * variables introduced by Binds.
   */
  public Code forceApply(TempSubst s) {
    return new Assert(a.apply(s), cf, c.forceApply(s));
  }

  private AllocType type;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  Type inferType(Position pos) throws Failure { // assert a cf; c
    type = cf.instantiate(); // get a type for the constructor
    type.resultUnifiesWith(pos, a.instantiate()); // check that it is compatible with a
    return c.inferType(pos);
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    c.generateCode(builder, o); // asserts are treated as nops
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    return new Assert(a, cf, c.deriveWithEnter(iargs));
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    return new Assert(a, cf, c.deriveWithCont(cont));
  }

  Code copy() {
    return new Assert(a, cf, c.copy());
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return c.doesntReturn();
  }

  boolean detectLoops(Block src, Blocks visited) {
    return c.detectLoops(src, visited);
  }

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    c = c.cleanup(src);
    if (!a.isLive()) { // Rewrite (assert cf _; c) ==> c
      MILProgram.report("eliminated an unused assertion in " + src.getId());
      return c;
    }
    return this;
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    c = c.inlining(src);
    return this;
  }

  Code prefixInline(TempSubst s, Temp[] us, Code d) {
    return new Assert(a.apply(s), cf, c.prefixInline(s, us, d));
  }

  int prefixInlineLength(int len) {
    return c.prefixInlineLength(len);
  }

  /**
   * Compute the length of this Code sequence for the purposes of prefix inlining. The returned
   * value is either the length of the code sequence (counting one for each Bind and Done node) or 0
   * if the code sequence ends with something other than Done. The argument should be initialized to
   * 0 for the first call.
   */
  int suffixInlineLength(int len) {
    return c.suffixInlineLength(len);
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return c.guarded(src);
  }

  /**
   * Find the list of variables that are used in this code sequence. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  Temps usedVars() {
    return c.usedVars();
  }

  Code removeUnusedArgs() {
    return new Assert(a, cf, c.removeUnusedArgs());
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Facts facts, TempSubst s) {
    Tail t = a.lookupFact(facts);
    // If we already have a fact in place for the asserted atom, then we
    // will assume that this assert is satisfied and is no longer needed.
    // e.g.,  x <- MkInt(2); assert MkInt x; c   ==>   x <- MkInt(2); c
    // TODO: should we check that t is a DataAlloc with constructor function cf?
    if (t != null) {
      return c.flow(facts, s);
    }
    c = c.flow(a.addCfunFact(cf, facts), s);
    return this;
  }

  public Code andThen(Temp[] vs, Code rest) {
    c = c.andThen(vs, rest);
    return this;
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() {
    Temps us = c.liveness();
    if (a.isLive() && !a.isIn(us)) {
      MILProgram.report("liveness replaced " + a + " in assertion with a wildcard");
      a = a.notLive();
    }
    return us;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return cf.summary() * 7 + c.summary() * 13 + 257;
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaAssert(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaAssert(Temps thisvars, Assert that, Temps thatvars) {
    return (this.cf == that.cf)
        && this.a.alphaAtom(thisvars, that.a, thatvars)
        && this.c.alphaCode(thisvars, that.c, thatvars);
  }

  void eliminateDuplicates() {
    c.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonAllocType(set);
    }
    cf = cf.canonCfun(set);
    a.collect(set);
    c.collect(set);
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    c = c.cfunSimplify(); // Simplify rest of code
    if (cf.isSingleConstructor()) { // Eliminate assert for a single constructor type
      MILProgram.report("eliminating assert for singleton constructor " + cf.getId());
      return c;
    }
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Assert(
        a.specializeAtom(spec, s, env),
        cf.specializeCfun(spec, type, s),
        c.specializeCode(spec, s, env));
  }

  Code bitdataRewrite(BitdataMap m) {
    BitdataRep r = cf.findRep(m);
    return (r == null) ? this : new Assert(a, cf.bitdataRewrite(r), c.bitdataRewrite(m));
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    return cf.repTransformAssert(set, a, c.repTransform(set, env));
  }

  /** Find the argument variables that are used in this Code sequence. */
  Temps addArgs() throws Failure {
    return a.add(c.addArgs());
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    c.countCalls();
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    c.countAllCalls();
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return c.identifyBlocks(src, bs);
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return c.findSuccs(cfg, src);
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return c.toLLVMCode(lm, vm, s, succs);
  }
}
