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

/** Represents (what should be) an unreachable alternative. */
public class FailAlt extends Alts {

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return false;
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return ds;
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    /* do nothing */
  }

  /** Force the application of a TempSubst to this list of alternatives. */
  public Alts apply(TempSubst s) {
    return this;
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return tvs;
  }

  void checkTypeAlts(Position pos, Type dom, Type rng) throws Failure {
    /* nothing to do here */
  }

  /** Generate code for this list of alternatives. */
  void generateCode(MachineBuilder builder, int o) {
    builder.stop();
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing enter.
   */
  Alts deriveWithEnter(Atom[] iargs) {
    return this;
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing invocation of a continuation.
   */
  Alts deriveWithCont(Atom cont) {
    return this;
  }

  Alts copy() {
    return this;
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return true;
  }

  /**
   * If all of the block calls in a Case are the same, including the default, then we can replace
   * the Case with a simple tail call. As a special case, it can also be used to eliminate Case
   * constructs that have a default but no alternatives.
   */
  BlockCall sameBlockCalls() {
    return null;
  }

  boolean sameBlockCalls(BlockCall cand) {
    return false;
  }

  void inlining() {
    /* nothing to do here */
  }

  Temps usedVars(Temps vs) {
    return vs;
  }

  public void flow(Defn d, Atom a, Facts facts, TempSubst s) {
    /* nothing to do */
  }

  Temps liveness(Temps vs) {
    return vs;
  }

  /** Find the BlockCall for the given constructor in this list of alternatives, if any. */
  BlockCall blockCallFor(Cfun cf) {
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return 19;
  }

  /** Test to see if two Alts sequences are alpha equivalent. */
  boolean alphaAlts(Temps thisvars, Alts that, Temps thatvars) {
    return that.alphaFailAlt(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaFailAlt(Temps thisvars, FailAlt that, Temps thatvars) {
    return true;
  }

  void eliminateDuplicates() {
    /* Nothing to do here */
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    /* nothing to do here */
  }

  /**
   * Return a simple tail to execute in place of this list of Alts if the list does not contain any
   * CfunAlt cases.
   */
  Tail noCfunAltTail() {
    return Prim.halt.withArgs();
  }

  /**
   * Eliminate the default at the end of a list of alternatives, replacing it with the unreachable
   * FailAlt; this is valid if we know that the list of alternatives includes a test for all of the
   * constructors of the corresponding type.
   */
  Alts elimDefAlt() {
    return this;
  }

  Alts promoteDefault(Cfun cf) {
    return this;
  }

  /** Generate a specialized version of this Alt. */
  Alts specializeAlts(MILSpec spec, TVarSubst s, SpecEnv env) {
    return this;
  }

  Alts repTransform(RepTypeSet set, RepEnv env) {
    return this;
  }

  BlockCall repTransformBitdataCase(RepTypeSet set, RepEnv env, obdd.Pat pat, Atom[] as) {
    return new BlockCall(MILProgram.abort, Atom.noAtoms);
  }

  /** Find the argument variables that are used in this Code sequence. */
  public Temps addArgs() throws Failure {
    return null;
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return bs;
  }

  /**
   * Find the CFG successors for this list of alternatives, having already found n earlier
   * alternatives.
   */
  Label[] findSuccs(CFG cfg, Node src, int n) {
    return new Label[n];
  }
}
