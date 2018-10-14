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
 * Represents a code sequence that implements a conditional jump, using the value in a to determine
 * which of the various alternatives in alts should be used, or taking the default branch, def, is
 * there is no matching alternative.
 */
public class Case extends Code {

  /** The discriminant for this Case. */
  private Atom a;

  /** A list of alternatives for this Case. */
  private Alt[] alts;

  /** A default branch, if none of the alternatives apply. */
  private BlockCall def;

  /** Default constructor. */
  public Case(Atom a, Alt[] alts, BlockCall def) {
    this.a = a;
    this.alts = alts;
    this.def = def;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    if (a == w || (def != null && def.contains(w))) {
      return true;
    }
    for (int i = 0; i < alts.length; i++) {
      if (alts[i].contains(w)) {
        return true;
      }
    }
    return false;
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    if (def != null) {
      ds = def.dependencies(ds);
    }
    for (int i = 0; i < alts.length; i++) {
      ds = alts[i].dependencies(ds);
    }
    return a.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    indentln(out, "case " + a.toString(ts) + " of");
    for (int i = 0; i < alts.length; i++) {
      indent(out); // double indent
      indent(out);
      alts[i].dump(out, ts);
    }
    if (def != null) {
      indent(out); // double indent
      indent(out);
      out.print("_ -> ");
      def.displayln(out, ts);
    }
  }

  /**
   * Force the application of a TempSubst to this Code sequence, forcing construction of a fresh
   * copy of the input code structure, including the introduction of new temporaries in place of any
   * variables introduced by Binds.
   */
  public Code forceApply(TempSubst s) { // case a of alts; d
    Alt[] talts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      talts[i] = alts[i].forceApply(s);
    }
    BlockCall d = (def == null) ? null : def.forceApplyBlockCall(s);
    return new Case(a.apply(s), talts, d);
  }

  private Type dom;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    if (def != null) {
      tvs = def.tvars(tvs);
    }
    for (int i = 0; i < alts.length; i++) {
      tvs = alts[i].tvars(tvs);
    }
    return tvs;
  }

  Type inferType(Position pos) throws Failure { // case a of alts [; _ -> def]
    dom = a.instantiate();
    Type rng = new TVar(Tyvar.tuple);
    for (int i = 0; i < alts.length; i++) {
      alts[i].checkAltType(pos, dom, rng);
    }
    if (def != null) {
      def.inferType(pos).unify(pos, rng);
    }
    return rng;
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    a.load(builder);
    if (alts.length >= 1) {
      int iaddr = alts[0].generateAltCode(builder, o);
      for (int i = 1; i < alts.length; i++) {
        builder.patchToHere(iaddr); // fix previous test to branch to this case
        iaddr = alts[i].generateAltCode(builder, o);
      }
      builder.patchToHere(iaddr);
    }
    if (def != null) {
      def.generateTailCode(builder, o);
    } else {
      builder.stop();
    }
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    Alt[] nalts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      nalts[i] = alts[i].deriveWithEnter(iargs);
    }
    BlockCall d = (def == null) ? null : def.deriveWithEnter(iargs);
    return new Case(a, nalts, d);
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    Alt[] nalts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      nalts[i] = alts[i].deriveWithCont(cont);
    }
    BlockCall d = (def == null) ? null : def.deriveWithCont(cont);
    return new Case(a, nalts, d);
  }

  /**
   * Test to determine if this code is an expression of the form case v of alts where v is the
   * result of a preceding block call. If so, return a transformed version of the code that makes
   * use of a newly defined, known continuation that can subsequently be pushed in to the individual
   * branches of the case for further optimization.
   */
  public Code casesOn(Temp v, BlockCall bc) {
    if (a == v && bc.contCand()) {
      // Construct a continuation for the derived block:
      Tail cont = makeCont(v);
      Temp w = new Temp();

      // Replace original code with call to a new derived block:
      return new Bind(w, cont, new Done(bc.deriveWithCont(w)));
    }
    return null;
  }

  Code copy() {
    return new Case(a, Alt.copy(alts), def);
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    // If the default or any of the alternatives can return, then the Case might also be able to
    // return.
    if (def != null && !def.doesntReturn()) {
      return false;
    }
    for (int i = 0; i < alts.length; i++) {
      if (!alts[i].doesntReturn()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    BlockCall bc = Alt.sameBlockCalls(def, alts);
    if (bc != null) { // Rewrite a case in which all of the targets are the same
      MILProgram.report("eliminated a case with the same block in each branch in " + src.getId());
      return new Done(bc);
    }
    return this;
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    for (int i = 0; i < alts.length; i++) {
      alts[i].inlineAlt();
    }
    if (def != null) {
      def = def.inlineBlockCall();
    }
    // If the Case has no alternatives, then we can use the default directly.
    return (alts.length == 0) ? new Done(def).inlining(src, INLINE_ITER_LIMIT) : this;
    // TODO: is it appropriate to apply inlining to the def above?  If there is useful
    // work to be done, we'll catch it next time anyway ... won't we ... ?
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
    return true;
  }

  /**
   * Find the list of variables that are used in this code sequence. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  Temps usedVars() {
    Temps vs = (def == null) ? null : def.usedVars(null);
    for (int i = 0; i < alts.length; i++) {
      vs = alts[i].usedVars(vs);
    }
    return a.add(vs);
  }

  Code removeUnusedArgs() {
    BlockCall ndef = (def != null) ? def.removeUnusedArgsBlockCall() : null;
    Alt[] nalts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      nalts[i] = alts[i].removeUnusedArgs();
    }
    return new Case(a, nalts, ndef);
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Facts facts, TempSubst s) { // case a of alts; d
    // Look for an opportunity to short this Case
    a = a.apply(s);
    Tail t = a.lookupFact(facts);
    if (t != null) {
      BlockCall bc = t.shortCase(s, alts, def);
      if (bc != null) {
        return new Done(bc.rewriteBlockCall(facts));
      }
    }

    // Case will be preserved, but we still need to update using substitution s
    // and to compute the appropriate set of live variables.
    if (def != null) { // update the default branch
      def = def.applyBlockCall(s).rewriteBlockCall(facts);
    }
    // We do not need to kill facts about a here because we are not changing its value
    for (int i = 0; i < alts.length; i++) { // update regular branches
      alts[i].flow(a, facts, s);
    }
    return this;
  }

  public Code andThen(Temp[] vs, Code rest) {
    debug.Internal.error("andThen requires straightline code");
    return this;
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() {
    Temps vs = (def == null) ? null : def.liveness(null);
    for (int i = 0; i < alts.length; i++) {
      vs = Temps.add(alts[i].liveness(), vs);
    }
    a = a.shortTopLevel();
    return a.add(vs);
  }

  /**
   * Test to see if this code is Case that can be shorted out. Even If we find a Case, we still need
   * to check for a relevant item in the set of Facts (after applying a substitution that captures
   * the result of entering the block that starts with the Case). Again, if it turns out that the
   * optimization cannot be used, then we return null.
   */
  BlockCall shortCase(Temp[] params, Atom[] args, Facts facts) {
    TempSubst s = TempSubst.extend(params, args, null);
    Tail t = a.apply(s).lookupFact(facts);
    return (t == null) ? null : t.shortCase(s, alts, def);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    int sum = (def == null) ? 19 : def.summary();
    if (alts != null) {
      for (int i = 0; i < alts.length; i++) {
        sum = sum * 13 + alts[i].summary();
      }
    }
    return sum;
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaCase(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaCase(Temps thisvars, Case that, Temps thatvars) {
    if (!this.a.alphaAtom(thisvars, that.a, thatvars)) { // Compare discriminants:
      return false;
    }

    if (this.def == null) { // Compare default branches:
      if (that.def != null) {
        return false;
      }
    } else if (that.def == null || !this.def.alphaTail(thisvars, that.def, thatvars)) {
      return false;
    }

    if (this.alts == null) { // Compare alternatives:
      return that.alts == null;
    } else if (that.alts == null || this.alts.length != that.alts.length) {
      return false;
    }
    for (int i = 0; i < alts.length; i++) {
      if (!this.alts[i].alphaAlt(thisvars, that.alts[i], thatvars)) {
        return false;
      }
    }
    return true;
  }

  void eliminateDuplicates() {
    if (alts != null) {
      for (int i = 0; i < alts.length; i++) {
        alts[i].eliminateDuplicates();
      }
    }
    if (def != null) {
      def.eliminateDuplicates();
    }
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    a.collect(set);
    if (dom != null) {
      dom = dom.canonType(set);
    }
    for (int i = 0; i < alts.length; i++) {
      alts[i].collect(set);
    }
    if (def != null) {
      def.collect(set);
    }
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    // If there are no alternatives, replace this Case with a Done:
    if (alts.length == 0) { // no alternatives; use default
      MILProgram.report("eliminating case with no alternatives");
      return (def == null) ? new Done(Prim.halt.withArgs()) : new Done(def);
    }

    // Determine which constructor numbers are covered by alts:
    boolean[] used = null;
    for (int i = 0; i < alts.length; i++) {
      used = alts[i].cfunsUsed(used);
    }

    // Count to see if all constructor numbers are used:
    int count = 0;
    int notused = 0;
    for (int i = 0; i < used.length; i++) {
      if (used[i]) count++; // count this constructor as being used
      else notused = i; // record index of an unused constructor
    }

    // If all constructor numbers have been listed, then we can eliminate the default case and look
    // for a
    // possible newtype match:
    if (count == used.length) {
      if (count == 1) { // Look for a single constructor type:
        MILProgram.report("eliminating case on single constructor type");
        return new Done(alts[0].getBlockCall());
      }
      // TODO: if count==0, then we could introduce a halt(()) for unreachable code ...
      def = null; // Eliminate the default case
    } else if (count == used.length - 1 && def != null) {
      // Promote a default to a regular alternative for better flow results:
      alts = Alt.extendAlts(alts, notused, def); // Add new alternative
      def = null; // Eliminate default
    }
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    Alt[] salts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      salts[i] = alts[i].specializeAlt(spec, s, env);
    }
    BlockCall sdef = def == null ? null : def.specializeBlockCall(spec, s, env);
    return new Case(a.specializeAtom(spec, s, env), salts, sdef);
  }

  Code bitdataRewrite(BitdataMap m) {
    Alt[] nalts = new Alt[alts.length];
    for (int i = 0; i < alts.length; i++) {
      nalts[i] = alts[i].bitdataRewrite(m);
    }
    return new Case(a, nalts, def);
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    BitdataType bt = dom.bitdataType();
    if (bt != null) {
      return Alt.repTransformBitdataCase(set, env, bt, a, alts, def);
    } else {
      // We're assuming that cfunRewrite has already been applied; one consequence is that we don't
      // have to
      // deal with single constructor datatypes here.
      Alt[] nalts = new Alt[alts.length];
      for (int i = 0; i < alts.length; i++) {
        nalts[i] = alts[i].repTransformAlt(set, env);
      }
      BlockCall ndef = (def == null) ? null : def.repTransformBlockCall(set, env);
      return new Case(a, nalts, ndef);
    }
  }

  /** Find the argument variables that are used in this Code sequence. */
  Temps addArgs() throws Failure {
    Temps vs = (def == null) ? null : def.addArgs(null);
    for (int i = 0; i < alts.length; i++) {
      vs = Temps.add(alts[i].addArgs(), vs);
    }
    return a.add(vs);
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
    for (int i = 0; i < alts.length; i++) {
      alts[i].countAllCalls();
    }
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    for (int i = 0; i < alts.length; i++) {
      bs = alts[i].identifyBlocks(src, bs);
    }
    if (def != null) {
      bs = def.identifyBlocks(src, bs);
    }
    return bs;
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    Label[] succs = new Label[(def == null) ? alts.length : (alts.length + 1)];
    int i = 0;
    for (; i < alts.length; i++) {
      succs[i] = alts[i].findSucc(cfg, src);
    }
    if (def != null) {
      succs[i] = def.findSucc(cfg, src);
    }
    return succs;
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return Alt.toLLVMCase(lm, vm, a.toLLVMAtom(lm, vm, s), alts, def, succs);
  }
}
