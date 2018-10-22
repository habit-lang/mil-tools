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
import compiler.BuiltinPosition;
import compiler.Failure;
import compiler.Position;
import core.*;
import java.io.PrintWriter;

/** Represents an alternative in a monadic Case. */
public class Alt {

  private Cfun cf;

  private BlockCall bc;

  /** Default constructor. */
  public Alt(Cfun cf, BlockCall bc) {
    this.cf = cf;
    this.bc = bc;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return bc.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return bc.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    out.print(cf.toString());
    out.print(" -> ");
    bc.displayln(out, ts);
  }

  /** Force the application of a TempSubst to this Alt. */
  public Alt forceApply(TempSubst s) {
    return new Alt(cf, bc.forceApplyBlockCall(s));
  }

  private AllocType type;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  void checkAltType(Position pos, Type dom, Type rng) throws Failure { // cf -> bc
    type = cf.instantiate(); // get a type for the constructor
    type.resultUnifiesWith(pos, dom); // check that cf is a possible constructor for dom
    bc.inferType(pos).unify(pos, rng);
  }

  /**
   * Generate code for an alternative, returning the address that needs to be backpatched with the
   * location that we should branch to if the specific match for this alternative does not succeed.
   */
  int generateAltCode(MachineBuilder builder, int o) {
    int addr = builder.jntag(cf.getNum(), 0); // 0 is a dummy address here
    bc.generateTailCode(builder, o);
    return addr;
  }

  /**
   * Generate a new version of this alternative that branches to a derived block with a trailing
   * enter.
   */
  public Alt deriveWithEnter(Atom[] iargs) {
    return new Alt(cf, bc.deriveWithEnter(iargs));
  }

  /**
   * Generate a new version of this alternative that branches to a derived block with a trailing
   * invocation of a continuation.
   */
  public Alt deriveWithCont(Atom cont) {
    return new Alt(cf, bc.deriveWithCont(cont));
  }

  static Alt[] copy(Alt[] alts) {
    if (alts == null) {
      return null;
    } else {
      Alt[] copied = new Alt[alts.length];
      for (int i = 0; i < alts.length; i++) {
        copied[i] = alts[i].copy();
      }
      return copied;
    }
  }

  Alt copy() {
    return new Alt(cf, bc);
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return bc.doesntReturn();
  }

  /**
   * If all of the block calls in a Case are the same, including the default, then we can replace
   * the Case with a simple tail call. This doesn't happen much in practice, but it showed up in
   * testing, and is easy to implement :-)
   */
  static BlockCall sameBlockCalls(BlockCall def, Alt[] alts) {
    BlockCall cand = def;
    for (int i = 0; i < alts.length; i++) {
      BlockCall bc = alts[i].bc;
      if (cand == null) {
        cand = bc;
      } else if (!cand.sameBlockCall(bc)) {
        return null;
      }
    }
    return cand;
  }

  void inlineAlt() {
    bc = bc.inlineBlockCall();
  }

  Temps usedVars(Temps vs) {
    return bc.usedVars(vs);
  }

  Alt removeUnusedArgs() {
    return new Alt(cf, bc.removeUnusedArgsBlockCall());
  }

  public void flow(Atom a, Facts facts, TempSubst s) { // cf -> bc
    // We know which constructor will be used for a, but not its arguments:
    bc = bc.forceApplyBlockCall(s).rewriteBlockCall(a.addFact(cf, facts));
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() { // cf -> bc
    return bc.liveness(null);
  }

  /**
   * Test to determine whether this alternative will match a data value constructed using a
   * specified constructor and argument list. The substitution captures the original instantiation
   * of the block as determined by the original BlockCall.
   */
  BlockCall shortCase(TempSubst s, Cfun cf, Atom[] args) {
    return (cf == this.cf) ? bc.applyBlockCall(s) : null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return 3 + 7 * bc.summary();
  }

  /** Test to see if two alternatives are alpha equivalent. */
  boolean alphaAlt(Temps thisvars, Alt that, Temps thatvars) {
    return this.cf == that.cf && this.bc.alphaBlockCall(thisvars, that.bc, thatvars);
  }

  void eliminateDuplicates() {
    bc.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonAllocType(set);
    }
    cf = cf.canonCfun(set);
    bc.collect(set);
  }

  /**
   * Extend a list of (n-1) alternatives for a type that has n constructors with an extra
   * alternative for the missing nth constructor to avoid the need for a default branch. We assume
   * there is at least one alternative in alts, and that the unused constructor is at position
   * notused in the underlying array of constructors.
   */
  static Alt[] extendAlts(Alt[] alts, int notused, BlockCall bc) {
    int lastIdx = alts.length; // index of last/new alternative
    Alt[] newAlts = new Alt[lastIdx + 1];
    for (int i = 0; i < lastIdx; i++) { // copy existing alternatives
      newAlts[i] = alts[i];
    }
    Cfun cf = alts[0].cf.getCfuns()[notused];
    newAlts[lastIdx] = new Alt(cf, bc);
    // MILProgram.report("Replacing default branch with match on " + cf.getId());
    return newAlts;
  }

  /**
   * Account for the constructor that is used in this alternative, setting the corresponding flag in
   * the argument array, which has one position for each constructor in the underlying type.
   */
  boolean[] cfunsUsed(boolean[] used) {
    if (used == null) { // Allocate a flag array based on the constrs array for c:
      used = new boolean[cf.getCfuns().length];
    }
    // Flag use of this constructor as having been mentioned:
    int num = cf.getNum(); // flag use of this constructor
    if (num >= 0 && num < used.length) {
      if (used[num]) {
        debug.Internal.error("multiple alternatives for " + cf);
      }
      used[num] = true;
    } else {
      debug.Internal.error("cfun index out of range");
    }
    return used;
  }

  /**
   * Return the block call for this Alt. Intended for use in simplifying case expressions for single
   * constructor datatypes where there is no need for a pattern match and the block call in this Alt
   * can be used in place of the Case that contains the Alt.
   */
  BlockCall getBlockCall() {
    return bc;
  }

  /** Generate a specialized version of this Alt. */
  Alt specializeAlt(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Alt(cf.specializeCfun(spec, type, s), bc.specializeBlockCall(spec, s, env));
  }

  Alt bitdataRewrite(BitdataMap m) {
    BitdataRep r = cf.findRep(m);
    return (r == null) ? this : new Alt(cf.bitdataRewrite(r), bc);
  }

  Alt repTransformAlt(RepTypeSet set, RepEnv env) {
    return new Alt(cf.canonCfun(set), bc.repTransformBlockCall(set, env));
  }

  static Code repTransformBitdataCase(
      RepTypeSet set, RepEnv env, BitdataType bt, Atom a, Alt[] alts, BlockCall def) {
    //  Find the last relevant alternative
    int last = 0;
    for (obdd.Pat pat = obdd.Pat.empty(bt.getPat().getWidth()); last < alts.length; last++) {
      pat = alts[last].cf.getPat().or(pat);
      if (pat.isAll()) { // No need to progress further once all bit patterns have been matched.
        break;
      }
    }

    // Construct the final else branch:
    BlockCall eb =
        (last < alts.length)
            ? alts[last].bc.repTransformBlockCall(set, env) // from last relevant Alt
            : (def != null)
                ? def.repTransformBlockCall(set, env) // the default branch
                : new BlockCall(MILProgram.abort, Atom.noAtoms); // run-time abort

    if (last <= 0) { // Didn't pass first alternative?
      return new Done(eb); // ... then just execute a direct jump
    } else {
      Atom[] as = a.repAtom(set, env); // Atoms corresponding to discriminant
      Temps ts = eb.add(null); // Free Temps in eb
      for (; ; ) {
        Temp t = new Temp(); // flag to hold result of mask test call
        BlockCall tb = alts[--last].bc.repTransformBlockCall(set, env); // True branch
        Block mt = alts[last].cf.maskTestBlock();
        Code code = new Bind(t, new BlockCall(mt, as), new If(t, tb, eb));
        if (last == 0) {
          return code;
        }
        ts = Temps.add(as, tb.add(ts)); // find free Temps in code
        Temp[] ps = Temps.toArray(ts); // turn in to an array
        Temp[] vs = Temp.makeTemps(ps.length); // fresh variables
        // TODO: do a better job finding a position here ...
        Block b = new Block(BuiltinPosition.pos, vs, code.apply(TempSubst.extend(ps, vs, null)));
        eb = new BlockCall(b, ps);
      }
    }
  }

  /** Find the argument variables that are used in this Code sequence. */
  Temps addArgs() throws Failure {
    return bc.addArgs(null);
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    bc.countAllCalls();
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return bc.identifyBlocks(src, bs);
  }

  /** Find the CFG successor for this item. */
  Label findSucc(CFG cfg, Node src) {
    return bc.findSucc(cfg, src);
  }

  /** Generate LLVM code for a Case. */
  static llvm.Code toLLVMCase(
      LLVMMap lm, VarMap vm, llvm.Value v, Alt[] alts, BlockCall def, Label[] succs) {
    int nalts = alts.length;
    if (def == null) { // if there is no default, then we assume the match is exhaustive
      if (nalts == 0) {
        debug.Internal.error("zero outdegree match");
      }
      def = alts[--nalts].bc; // use the last alternative as our default
    }
    if (nalts == 0) { // no other alternatives to consider
      return new llvm.Goto(succs[0].label());
    } else {
      llvm.Value[] nums = new llvm.Value[nalts];
      String[] bs = new String[nalts];
      for (int i = 0; i < nalts; i++) {
        nums[i] = new llvm.Word(alts[i].cf.getNum());
        bs[i] = succs[i].label();
      }
      llvm.Type dt = LLVMMap.tagType(); // the type of the tag
      llvm.Local tag = vm.reg(dt); // a register to hold the tag
      llvm.Type at = dt.ptr(); // the type of pointers to the tag
      llvm.Local addr = vm.reg(at); // a register that points to the tag
      return new llvm.CodeComment(
          "read the tag for a data object",
          new llvm.Op(
              addr,
              new llvm.Getelementptr(at, v, llvm.Word.ZERO, llvm.Word.ZERO),
              new llvm.Op(
                  tag,
                  new llvm.Load(addr),
                  new llvm.CodeComment(
                      "branch based on the tag value",
                      new llvm.Switch(tag, nums, bs, succs[nalts].label())))));
    }
  }
}
