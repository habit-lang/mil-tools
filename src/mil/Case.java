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

/**
 * Represents a code sequence that implements a conditional jump, using the value in a to determine
 * which of the various alternatives in alts should be used, or taking the default branch, def, is
 * there is no matching alternative.
 */
public class Case extends Code {

  /** The discriminant for this Case. */
  private Atom a;

  /** A list of alternatives for this Case. */
  private Alts alts;

  /** Default constructor. */
  public Case(Atom a, Alts alts) {
    this.a = a;
    this.alts = alts;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return a == w || alts.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return a.dependencies(alts.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    indentln(out, "case " + a.toString(ts) + " of");
    alts.dump(out, ts);
  }

  /**
   * Apply a TempSubst to this Code sequence, forcing construction of a fresh copy of the input code
   * structure, including the introduction of new temporaries in place of any variables introduced
   * by Binds.
   */
  public Code apply(TempSubst s) {
    return new Case(a.apply(s), alts.apply(s));
  }

  private Type dom;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return alts.tvars(tvs);
  }

  Type inferType(Position pos) throws Failure { // case a of alts [; _ -> def]
    dom = a.instantiate();
    Type rng = new TVar(Tyvar.tuple);
    alts.checkTypeAlts(pos, dom, rng);
    return rng;
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    a.load(builder);
    alts.generateCode(builder, o);
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    return new Case(a, alts.deriveWithEnter(iargs));
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    return new Case(a, alts.deriveWithCont(cont));
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
      Tail cont = makeCont(new Temp[] {v});
      Temp w = new Temp();

      // Replace original code with call to a new derived block:
      return new Bind(w, cont, new Done(bc.deriveWithCont(w)));
    }
    return null;
  }

  Code copy() {
    return new Case(a, alts.copy());
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return alts.doesntReturn();
  }

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    BlockCall bc = alts.sameBlockCalls();
    if (bc != null) { // Rewrite a case in which all of the targets are the same
      MILProgram.report("eliminated a case with the same block in each branch in " + src);
      return new Done(bc);
    }
    return this;
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    alts.inlining();
    return this;
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
    return a.add(alts.usedVars(null));
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Defn d, Facts facts, TempSubst s) { // case a of alts; d
    // Look for an opportunity to short this Case
    a = a.apply(s);
    Tail t = a.lookupFact(facts);
    if (t != null) {
      BlockCall bc = t.shortCase(s, alts);
      if (bc != null) {
        return new Done(bc.rewriteBlockCall(d, facts, false));
      }
    }

    // Case will be preserved, but we still need to update using substitution s
    // and to compute the appropriate set of live variables.
    alts.flow(d, a, facts, s);
    return this;
  }

  public Code andThen(Temp[] vs, Code rest) {
    debug.Internal.error("andThen requires straight-line code");
    return this;
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() {
    a = a.shortTopLevel();
    return a.add(alts.liveness(null));
  }

  /**
   * Test to see if this code is a Case that can be shorted out. Even if we find a Case, we still
   * need to check for a relevant item in the set of Facts (after applying a substitution that
   * captures the result of entering the block that starts with the Case). Again, if it turns out
   * that the optimization cannot be used, then we return null.
   */
  BlockCall shortCase(Temp[] params, Atom[] args, Facts facts) {
    TempSubst s = TempSubst.extend(params, args, null);
    Tail t = a.apply(s).lookupFact(facts);
    return (t == null) ? null : t.shortCase(s, alts);
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
    alts.calcSources(d, params, rebound);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return alts.summary();
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaCase(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaCase(Temps thisvars, Case that, Temps thatvars) {
    return this.a.alphaAtom(thisvars, that.a, thatvars)
        && this.alts.alphaAlts(thisvars, that.alts, thatvars);
  }

  void eliminateDuplicates() {
    alts.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (dom != null) {
      dom = dom.canonType(set);
    }
    a.collect(set);
    alts.collect(set);
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    // If there are no alternatives, replace this Case with a Done:
    Tail t = alts.noCfunAltTail();
    if (t != null) { // no alternatives; use default
      MILProgram.report("eliminating case with no alternatives");
      return new Done(t);
    }

    // Determine which constructor numbers are covered by alts:
    Cfun[] used = alts.cfunsUsed();
    int count = 0;
    int notused = 0;
    for (int i = 0; i < used.length; i++) {
      if (used[i] != null) count++; // count this constructor as being used
      else notused = i; // record index of an unused constructor
    }

    // If all constructor numbers have been listed, then we can eliminate the default case and look
    // for a
    // possible newtype match:
    if (count == used.length) {
      if (count == 1) { // Look for a single constructor type:
        MILProgram.report("eliminating case on single constructor type");
        return new Done(alts.firstBlockCall());
      }
      alts = alts.elimDefAlt(); // Eliminate the default case
    } else if (count == used.length - 1) {
      // For better flow results, promote a default to a regular alternative with the unused
      // constructor:
      alts = alts.promoteDefault(used[(notused == 0) ? 1 : 0].getCfuns()[notused]);
    }
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Case(a.specializeAtom(spec, s, env), alts.specializeAlts(spec, s, env));
  }

  Code bitdataRewrite(BitdataMap m) {
    return new Case(a, alts.bitdataRewrite(m));
  }

  Code mergeRewrite(MergeMap mmap) {
    return new Case(a, alts.mergeRewrite(mmap));
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    BitdataType bt = dom.bitdataType();
    if (bt != null) {
      return new Done(
          alts.repTransformBitdataCase(set, env, bt.getPat().not(), a.repAtom(set, env)));
    } else if (dom.referenceType(null)) {
      return alts.repTransformPtrCase(set, env, a);
    } else {
      return new Case(a, alts.repTransform(set, env));
    }
  }

  /** Find the argument variables that are used in this Code sequence. */
  public Temps addArgs() throws Failure {
    return a.add(alts.addArgs());
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
    return alts.identifyBlocks(src, bs);
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return alts.findSuccs(cfg, src, 0);
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    int n = succs.length; // Determine number of successors
    if (n == 0) {
      debug.Internal.error("zero outdegree match");
    }
    String def = succs[--n].label(); // Use the last alternative as a default
    if (n == 0) { // Make a direct jump if there are no other alternatives
      return new llvm.Goto(def);
    } else {
      llvm.Value[] nums = new llvm.Value[n];
      String[] labs = new String[n];
      alts.collectAlts(nums, labs, succs, n - 1);
      llvm.Type dt = LLVMMap.tagType(); // the type of the tag
      llvm.Local tag = vm.reg(dt); // a register to hold the tag
      llvm.Type at = dt.ptr(); // the type of pointers to the tag
      llvm.Local addr = vm.reg(at); // a register that points to the tag
      return new llvm.CodeComment(
          "read the tag for a data object",
          new llvm.Op(
              addr,
              new llvm.Getelementptr(at, a.toLLVMAtom(lm, vm, s), llvm.Word.ZERO, llvm.Index.ZERO),
              new llvm.Op(
                  tag,
                  new llvm.Load(addr),
                  new llvm.CodeComment(
                      "branch based on the tag value", new llvm.Switch(tag, nums, labs, def)))));
    }
  }
}
