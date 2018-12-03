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

/** Represents a branch that attempts to match a specific constructor function. */
public class CfunAlt extends Alts {

  private Cfun cf;

  private BlockCall bc;

  private Alts next;

  /** Default constructor. */
  public CfunAlt(Cfun cf, BlockCall bc, Alts next) {
    this.cf = cf;
    this.bc = bc;
    this.next = next;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return bc.contains(w) || next.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return bc.dependencies(next.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    Code.indent(out); // double indent
    Code.indent(out);
    out.print(cf.toString());
    out.print(" -> ");
    bc.displayln(out, ts);
    next.dump(out, ts);
  }

  /** Force the application of a TempSubst to this list of alternatives. */
  public Alts forceApply(TempSubst s) {
    return new CfunAlt(cf, bc.forceApplyBlockCall(s), next.forceApply(s));
  }

  private AllocType type;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return next.tvars(type.tvars(tvs));
  }

  void checkTypeAlts(Position pos, Type dom, Type rng) throws Failure {
    type = cf.instantiate(); // get a type for the constructor
    type.resultUnifiesWith(pos, dom); // check that cf is a possible constructor for dom
    bc.inferType(pos).unify(pos, rng);
    next.checkTypeAlts(pos, dom, rng);
  }

  /** Generate code for this list of alternatives. */
  void generateCode(MachineBuilder builder, int o) {
    int patchAddr = builder.jntag(cf.getNum(), 0); // 0 is a dummy address here
    bc.generateTailCode(builder, o);
    next.generateCode(builder, o, patchAddr);
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing enter.
   */
  Alts deriveWithEnter(Atom[] iargs) {
    return new CfunAlt(cf, bc.deriveWithEnter(iargs), next.deriveWithEnter(iargs));
  }

  /**
   * Generate a new version of this list of alternatives that branches to derived blocks with a
   * trailing invocation of a continuation.
   */
  Alts deriveWithCont(Atom cont) {
    return new CfunAlt(cf, bc.deriveWithCont(cont), next.deriveWithCont(cont));
  }

  Alts copy() {
    return new CfunAlt(cf, bc, next.copy());
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return bc.doesntReturn() && next.doesntReturn();
  }

  /**
   * If all of the block calls in a Case are the same, including the default, then we can replace
   * the Case with a simple tail call. As a special case, it can also be used to eliminate Case
   * constructs that have a default but no alternatives.
   */
  BlockCall sameBlockCalls() {
    return next.sameBlockCalls(bc) ? bc : null;
  }

  boolean sameBlockCalls(BlockCall cand) {
    return cand.sameBlockCall(bc) && next.sameBlockCalls(cand);
  }

  void inlining() {
    bc = bc.inlineBlockCall();
    next.inlining();
  }

  Temps usedVars(Temps vs) {
    return next.usedVars(bc.usedVars(vs));
  }

  Alts removeUnusedArgs() {
    return new CfunAlt(cf, bc.removeUnusedArgsBlockCall(), next.removeUnusedArgs());
  }

  public void flow(Atom a, Facts facts, TempSubst s) {
    // We know which constructor will be used for a, but not its arguments:
    bc = bc.forceApplyBlockCall(s).rewriteBlockCall(a.addFact(cf, facts));
    next.flow(a, facts, s);
  }

  Temps liveness(Temps vs) {
    return next.liveness(bc.liveness(vs));
  }

  /** Find the BlockCall for the given constructor in this list of alternatives, if any. */
  BlockCall blockCallFor(Cfun cf) {
    return (this.cf == cf) ? bc : next.blockCallFor(cf);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return (3 + 7 * bc.summary()) + 13 * next.summary();
  }

  /** Test to see if two Alts sequences are alpha equivalent. */
  boolean alphaAlts(Temps thisvars, Alts that, Temps thatvars) {
    return that.alphaCfunAlt(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaCfunAlt(Temps thisvars, CfunAlt that, Temps thatvars) {
    return this.cf == that.cf
        && this.bc.alphaBlockCall(thisvars, that.bc, thatvars)
        && this.next.alphaAlts(thisvars, that.next, thatvars);
  }

  void eliminateDuplicates() {
    bc.eliminateDuplicates();
    next.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonAllocType(set);
    }
    cf = cf.canonCfun(set);
    bc.collect(set);
    next.collect(set);
  }

  /**
   * Return a simple tail to execute in place of this list of Alts if the list does not contain any
   * CfunAlt cases.
   */
  Tail noCfunAltTail() {
    return null;
  }

  /**
   * Calculate an array of Cfuns with one entry for each constructor that is used in this list of
   * Alts.
   */
  Cfun[] cfunsUsed() {
    return cfunsUsed(new Cfun[cf.getCfuns().length]);
  }

  Cfun[] cfunsUsed(Cfun[] used) {
    // Flag use of this constructor as having been mentioned:
    int num = cf.getNum(); // flag use of this constructor
    if (num >= 0 && num < used.length) {
      if (used[num] != null) {
        debug.Internal.error("multiple alternatives for " + cf);
      }
      used[num] = cf;
    } else {
      debug.Internal.error("cfun index out of range");
    }
    return next.cfunsUsed(used);
  }

  /** Return the BlockCall for the first CfunAlt in this list, or null if there is no CfunAlt. */
  Tail firstBlockCall() {
    return bc;
  }

  /**
   * Eliminate the default at the end of a list of alternatives, replacing it with the unreachable
   * FailAlt; this is valid if we know that the list of alternatives includes a test for all of the
   * constructors of the corresponding type.
   */
  Alts elimDefAlt() {
    next = next.elimDefAlt();
    return this;
  }

  Alts promoteDefault(Cfun cf) {
    next = next.promoteDefault(cf);
    return this;
  }

  /** Generate a specialized version of this Alt. */
  Alts specializeAlts(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new CfunAlt(
        cf.specializeCfun(spec, type, s),
        bc.specializeBlockCall(spec, s, env),
        next.specializeAlts(spec, s, env));
  }

  Alts bitdataRewrite(BitdataMap m) {
    BitdataRep r = cf.findRep(m);
    Cfun ncf = (r == null) ? cf : cf.bitdataRewrite(r);
    return new CfunAlt(ncf, bc, next.bitdataRewrite(m));
  }

  Alts mergeRewrite(MergeMap mmap) {
    return new CfunAlt(cf.lookup(mmap), bc, next.mergeRewrite(mmap));
  }

  Alts repTransform(RepTypeSet set, RepEnv env) {
    return new CfunAlt(
        cf.canonCfun(set), bc.repTransformBlockCall(set, env), next.repTransform(set, env));
  }

  BlockCall repTransformBitdataCase(RepTypeSet set, RepEnv env, obdd.Pat pat, Atom[] as) {
    pat = cf.getPat().or(pat); // Add in bit patterns for this constructor
    if (pat.isAll()) { // No need to progress further once all bit patterns have been matched
      return bc.repTransformBlockCall(set, env);
    }
    BlockCall tbc = bc.repTransformBlockCall(set, env); // BlockCall for then branch
    BlockCall ebc = next.repTransformBitdataCase(set, env, pat, as); // BlockCall for else branch
    Temp t = new Temp(); // Flag to hold mask test result
    Code code = new Bind(t, new BlockCall(cf.maskTestBlock(), as), new If(t, tbc, ebc));
    Temps ts = Temps.add(as, tbc.add(ebc.add(null))); // Find free Temps in code
    Temp[] ps = Temps.toArray(ts); // Turn into an array
    Temp[] vs = Temp.makeTemps(ps.length); // Make fresh vars for parameters
    // TODO: do a better job finding a position here ...
    Block b = new Block(BuiltinPosition.pos, vs, code.apply(TempSubst.extend(ps, vs, null)));
    return new BlockCall(b, ps);
  }

  /** Find the argument variables that are used in this Code sequence. */
  public Temps addArgs() throws Failure {
    return Temps.add(bc.addArgs(null), next.addArgs());
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return next.identifyBlocks(src, bc.identifyBlocks(src, bs));
  }

  /**
   * Find the CFG successors for this list of alternatives, having already found n earlier
   * alternatives.
   */
  Label[] findSuccs(CFG cfg, Node src, int n) {
    Label[] succs = next.findSuccs(cfg, src, n + 1);
    succs[n] = bc.findSucc(cfg, src);
    return succs;
  }

  /**
   * Collect the values and labels for each of the items in this list of alternatives in the
   * specified arrays (counting down, although the order shouldn't really matter here).
   */
  void collectAlts(llvm.Value[] nums, String[] labs, Label[] succs, int i) {
    if (i >= 0) {
      nums[i] = new llvm.Word(cf.getNum());
      labs[i] = succs[i].label();
      next.collectAlts(nums, labs, succs, i - 1);
    }
  }
}
