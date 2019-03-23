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

/** Represents a list of alternatives in a Case. */
public abstract class Alts {

  /** Test for a free occurrence of a particular variable. */
  public abstract boolean contains(Temp w);

  /** Find the dependencies of this AST fragment. */
  public abstract Defns dependencies(Defns ds);

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public abstract void dump(PrintWriter out, Temps ts);

  /** Force the application of a TempSubst to this list of alternatives. */
  public abstract Alts apply(TempSubst s);

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  abstract TVars tvars(TVars tvs);

  abstract void checkTypeAlts(Position pos, Type dom, Type rng) throws Failure;

  /** Generate code for this list of alternatives. */
  abstract void generateCode(MachineBuilder builder, int o);

  /**
   * Generate code for this list of alternatives in a situation where there is a previous jump at
   * the specified patchAddr that needs to be backpatched to point to the start of the instruction
   * sequence generated here.
   */
  void generateCode(MachineBuilder builder, int o, int patchAddr) {
    builder.patchToHere(patchAddr); // fix previous test to branch to this case
    generateCode(builder, o);
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing enter.
   */
  abstract Alts deriveWithEnter(Atom[] iargs);

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing invocation of a continuation.
   */
  abstract Alts deriveWithCont(Atom cont);

  abstract Alts copy();

  /** Test for code that is guaranteed not to return. */
  abstract boolean doesntReturn();

  /**
   * If all of the block calls in a Case are the same, including the default, then we can replace
   * the Case with a simple tail call. As a special case, it can also be used to eliminate Case
   * constructs that have a default but no alternatives.
   */
  abstract BlockCall sameBlockCalls();

  /** Test to see if all of the block calls in this case are the same as a given candidate. */
  abstract boolean sameBlockCalls(BlockCall cand);

  abstract void inlining();

  abstract Temps usedVars(Temps vs);

  public abstract void flow(Defn d, Atom a, Facts facts, TempSubst s);

  abstract Temps liveness(Temps vs);

  /** Find the BlockCall for the given constructor in this list of alternatives, if any. */
  abstract BlockCall blockCallFor(Cfun cf);

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
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  abstract int summary();

  /** Test to see if two Alts sequences are alpha equivalent. */
  abstract boolean alphaAlts(Temps thisvars, Alts that, Temps thatvars);

  /** Test two items for alpha equivalence. */
  boolean alphaCfunAlt(Temps thisvars, CfunAlt that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaDefAlt(Temps thisvars, DefAlt that, Temps thatvars) {
    return false;
  }

  /** Test two items for alpha equivalence. */
  boolean alphaFailAlt(Temps thisvars, FailAlt that, Temps thatvars) {
    return false;
  }

  abstract void eliminateDuplicates();

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  /**
   * Return a simple tail to execute in place of this list of Alts if the list does not contain any
   * CfunAlt cases.
   */
  abstract Tail noCfunAltTail();

  /**
   * Calculate an array of Cfuns with one entry for each constructor that is used in this list of
   * Alts.
   */
  Cfun[] cfunsUsed() {
    return null;
  }

  Cfun[] cfunsUsed(Cfun[] used) {
    return used;
  }

  /** Return the BlockCall for the first CfunAlt in this list, or null if there is no CfunAlt. */
  Tail firstBlockCall() {
    return null;
  }

  /**
   * Eliminate the default at the end of a list of alternatives, replacing it with the unreachable
   * FailAlt; this is valid if we know that the list of alternatives includes a test for all of the
   * constructors of the corresponding type.
   */
  abstract Alts elimDefAlt();

  abstract Alts promoteDefault(Cfun cf);

  /** Generate a specialized version of this Alt. */
  abstract Alts specializeAlts(MILSpec spec, TVarSubst s, SpecEnv env);

  Alts bitdataRewrite(BitdataMap m) {
    return this;
  }

  Alts mergeRewrite(MergeMap mmap) {
    return this;
  }

  abstract Alts repTransform(RepTypeSet set, RepEnv env);

  Code repTransformPtrCase(RepTypeSet set, RepEnv env, Atom a) {
    Atom[] ar = a.repAtom(set, env); // Find the atom to test
    if (ar == null || ar.length != 1) {
      debug.Internal.error("Unexpected atom in Ptr pattern match");
    }
    BlockCall ifNull = blockCallFor(Cfun.Null);
    BlockCall ifRef = blockCallFor(Cfun.Ref);
    if (ifNull == null || ifRef == null) {
      debug.Internal.error("Alternatives do not match pointer values");
    }
    Temp t = new Temp();
    return new Bind(
        t,
        Prim.eq.withArgs(ar[0], Word.Zero),
        new If(t, ifNull.repTransformBlockCall(set, env), ifRef.repTransformBlockCall(set, env)));
  }

  abstract BlockCall repTransformBitdataCase(RepTypeSet set, RepEnv env, obdd.Pat pat, Atom[] as);

  /** Find the argument variables that are used in this Code sequence. */
  public abstract Temps addArgs() throws Failure;

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  abstract Blocks identifyBlocks(Defn src, Blocks bs);

  /**
   * Find the CFG successors for this list of alternatives, having already found n earlier
   * alternatives.
   */
  abstract Label[] findSuccs(CFG cfg, Node src, int n);

  /**
   * Collect the values and labels for each of the items in this list of alternatives in the
   * specified arrays (counting down, although the order shouldn't really matter here).
   */
  void collectAlts(llvm.Value[] nums, String[] labs, Label[] succs, int i) {
    /* nothing to do */
  }
}
