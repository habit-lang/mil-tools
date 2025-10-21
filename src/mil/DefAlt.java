/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

/** Represents a default branch, if none of the previous alternatives apply. */
public class DefAlt extends Alts {

  private BlockCall bc;

  /** Default constructor. */
  public DefAlt(BlockCall bc) {
    this.bc = bc;
  }

  /** Test for a free occurrence of a particular variable. */
  boolean contains(Temp w) {
    return bc.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  Defns dependencies(Defns ds) {
    return bc.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  void dump(PrintWriter out, Temps ts) {
    Code.indent(out); // double indent
    Code.indent(out);
    out.print("_ -> ");
    bc.displayln(out, ts);
  }

  /** Force the application of a TempSubst to this list of alternatives. */
  Alts apply(TempSubst s) {
    return new DefAlt(bc.applyBlockCall(s));
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return bc.tvars(tvs);
  }

  void checkTypeAlts(Position pos, Type dom, Type rng) throws Failure {
    bc.inferType(pos).unify(pos, rng);
  }

  /** Generate code for this list of alternatives. */
  void generateCode(MachineBuilder builder, int o) {
    bc.generateTailCode(builder, o);
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing enter.
   */
  Alts deriveWithEnter(Atom[] iargs) {
    return new DefAlt(bc.deriveWithEnter(iargs));
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing invocation of a continuation.
   */
  Alts deriveWithCont(Atom cont) {
    return new DefAlt(bc.deriveWithCont(cont));
  }

  Alts copy() {
    return new DefAlt(bc);
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return bc.doesntReturn();
  }

  /**
   * If all of the block calls in a Case are the same, including the default, then we can replace
   * the Case with a simple tail call. As a special case, it can also be used to eliminate Case
   * constructs that have a default but no alternatives.
   */
  BlockCall sameBlockCalls() {
    return bc;
  }

  /** Test to see if all of the block calls in this case are the same as a given candidate. */
  boolean sameBlockCalls(BlockCall cand) {
    return cand.sameBlockCall(bc);
  }

  void inlining() {
    bc = bc.inlineBlockCall();
  }

  Temps usedVars(Temps vs) {
    return bc.usedVars(vs);
  }

  public void flow(Defn d, Atom a, Facts facts, TempSubst s) {
    bc = bc.applyBlockCall(s).rewriteBlockCall(d, facts, false);
  }

  Temps liveness(Temps vs) {
    return bc.liveness(vs);
  }

  /** Find the BlockCall for the given constructor in this list of alternatives, if any. */
  BlockCall blockCallFor(Cfun cf) {
    return bc;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return bc.summary();
  }

  /** Test to see if two Alts sequences are alpha equivalent. */
  boolean alphaAlts(Temps thisvars, Alts that, Temps thatvars) {
    return that.alphaDefAlt(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaDefAlt(Temps thisvars, DefAlt that, Temps thatvars) {
    return this.bc.alphaBlockCall(thisvars, that.bc, thatvars);
  }

  void eliminateDuplicates() {
    bc = bc.eliminateDuplicatesBlockCall();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    bc.collect(set);
  }

  /**
   * Return a simple tail to execute in place of this list of Alts if the list does not contain any
   * CfunAlt cases.
   */
  Tail noCfunAltTail() {
    return bc;
  }

  /**
   * Eliminate the default at the end of a list of alternatives, replacing it with the unreachable
   * FailAlt; this is valid if we know that the list of alternatives includes a test for all of the
   * constructors of the corresponding type.
   */
  Alts elimDefAlt() {
    return new FailAlt();
  }

  Alts promoteDefault(Cfun cf) {
    return new CfunAlt(cf, bc, new FailAlt());
  }

  /** Generate a specialized version of this Alt. */
  Alts specializeAlts(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new DefAlt(bc.specializeBlockCall(spec, s, env));
  }

  Alts repTransform(RepTypeSet set, RepEnv env) {
    return new DefAlt(bc.repTransformBlockCall(set, env));
  }

  BlockCall repTransformBitdataCase(RepTypeSet set, RepEnv env, obdd.Pat pat, Atom[] as) {
    return bc.repTransformBlockCall(set, env);
  }

  /** Find the argument variables that are used in this Code sequence. */
  public Temps addArgs() throws Failure {
    return bc.addArgs(null);
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return bc.identifyBlocks(src, bs);
  }

  /**
   * Find the CFG successors for this list of alternatives, having already found n earlier
   * alternatives.
   */
  Label[] findSuccs(CFG cfg, Node src, int n) {
    Label[] succs = new Label[n + 1];
    succs[n] = bc.findSucc(cfg, src);
    return succs;
  }
}
