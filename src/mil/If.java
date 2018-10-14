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
 * Represents a conditional test on the internal boolean type (as opposed to a boolean type built
 * using algebraic datatypes ...)
 */
public class If extends Code {

  /** The discriminant for this If. */
  private Atom a;

  /** The true branch. */
  private BlockCall ifTrue;

  /** The false branch. */
  private BlockCall ifFalse;

  /** Default constructor. */
  public If(Atom a, BlockCall ifTrue, BlockCall ifFalse) {
    this.a = a;
    this.ifTrue = ifTrue;
    this.ifFalse = ifFalse;
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return a == w || ifTrue.contains(w) || ifFalse.contains(w);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return a.dependencies(ifFalse.dependencies(ifTrue.dependencies(ds)));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    indentln(out, "if " + a.toString(ts));
    indent(out);
    out.print("  then ");
    ifTrue.displayln(out, ts);
    indent(out);
    out.print("  else ");
    ifFalse.displayln(out, ts);
  }

  /**
   * Force the application of a TempSubst to this Code sequence, forcing construction of a fresh
   * copy of the input code structure, including the introduction of new temporaries in place of any
   * variables introduced by Binds.
   */
  public Code forceApply(TempSubst s) {
    return new If(a.apply(s), ifTrue.forceApplyBlockCall(s), ifFalse.forceApplyBlockCall(s));
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return ifFalse.tvars(ifTrue.tvars(tvs));
  }

  Type inferType(Position pos) throws Failure { // if a then ifTrue else ifFalse
    a.instantiate().unify(pos, Tycon.flag.asType());
    Type t = ifTrue.inferType(pos);
    t.unify(pos, ifFalse.inferType(pos));
    return t;
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    a.load(builder);
    int iaddr = builder.jfalse(0);
    ifTrue.generateTailCode(builder, o);
    builder.patchToHere(iaddr);
    ifFalse.generateTailCode(builder, o);
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    return new If(a, ifTrue.deriveWithEnter(iargs), ifFalse.deriveWithEnter(iargs));
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    return new If(a, ifTrue.deriveWithCont(cont), ifFalse.deriveWithCont(cont));
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
    return new If(a, ifTrue, ifFalse);
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return ifTrue.doesntReturn() && ifFalse.doesntReturn();
  }

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    if (ifTrue.sameBlockCall(ifFalse)) { // Rewrite (if a then bc else bc) ==> bc
      MILProgram.report("eliminated an if with the same block in each branch in " + src.getId());
      return new Done(ifTrue);
    }
    return this;
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    ifTrue = ifTrue.inlineBlockCall();
    ifFalse = ifFalse.inlineBlockCall();
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
    return a.add(ifFalse.usedVars(ifTrue.usedVars(null)));
  }

  Code removeUnusedArgs() {
    return new If(a, ifTrue.removeUnusedArgsBlockCall(), ifFalse.removeUnusedArgsBlockCall());
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Facts facts, TempSubst s) { // if a then ifTrue else ifFalse
    a = a.apply(s);
    Flag c = a.isFlag(); // Does the argument hold a known constant?
    if (c != null) {
      BlockCall bc = c.getVal() ? ifTrue : ifFalse;
      return new Done(bc.forceApplyBlockCall(a.mapsTo(c, s)).rewriteBlockCall(facts));
    } else {
      Tail t = a.lookupFact(facts); // Look for a fact about a
      if (t != null) {
        // If a = bnot((n)), then an (if a then t else f) can be rewritten as
        // (if n then f else t), flipping the order of the true and false branches.
        Atom n = t.isBnot();
        if (n != null) {
          a = n; // change test to use n
          BlockCall bc = ifTrue; // swap the true and false branches
          ifTrue = ifFalse;
          ifFalse = bc;
        }
      }
    }
    ifTrue = ifTrue.applyBlockCall(a.mapsTo(Flag.True, s)).rewriteBlockCall(facts);
    ifFalse = ifFalse.applyBlockCall(a.mapsTo(Flag.False, s)).rewriteBlockCall(facts);
    Flag tc = ifTrue.returnsFlag();
    Flag fc = ifFalse.returnsFlag();
    if (tc != null && fc != null) {
      boolean tb = tc.getVal();
      boolean fb = fc.getVal();
      if (tb == fb) {
        MILProgram.report("eliminating if: branches both return " + tb);
        return new Done(ifTrue);
      } else if (tb && !fb) {
        MILProgram.report("eliminating if: branches true, false, resp.");
        return new Done(new Return(a));
      } else if (fb && !tb) {
        MILProgram.report("eliminating if: branches false, true, resp.");
        return new Done(Prim.bnot.withArgs(a));
      }
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
    return a.add(ifFalse.liveness(ifTrue.liveness(null)));
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return ifTrue.summary() * 3 + ifFalse.summary() * 23;
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaIf(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaIf(Temps thisvars, If that, Temps thatvars) {
    return this.a.alphaAtom(thisvars, that.a, thatvars)
        && this.ifTrue.alphaBlockCall(thisvars, that.ifTrue, thatvars)
        && this.ifFalse.alphaBlockCall(thisvars, that.ifFalse, thatvars);
  }

  void eliminateDuplicates() {
    ifTrue.eliminateDuplicates();
    ifFalse.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    a.collect(set);
    ifTrue.collect(set);
    ifFalse.collect(set);
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new If(
        a.specializeAtom(spec, s, env),
        ifTrue.specializeBlockCall(spec, s, env),
        ifFalse.specializeBlockCall(spec, s, env));
  }

  Code bitdataRewrite(BitdataMap m) {
    return this;
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    return new If(
        a, ifTrue.repTransformBlockCall(set, env), ifFalse.repTransformBlockCall(set, env));
  }

  /** Find the argument variables that are used in this Code sequence. */
  Temps addArgs() throws Failure {
    return a.add(ifFalse.addArgs(ifTrue.addArgs(null)));
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
    ifTrue.countAllCalls();
    ifFalse.countAllCalls();
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return ifFalse.identifyBlocks(src, ifTrue.identifyBlocks(src, bs));
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return new Label[] {ifTrue.findSucc(cfg, src), ifFalse.findSucc(cfg, src)};
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return new llvm.Cond(a.toLLVMAtom(lm, vm, s), succs[0].label(), succs[1].label());
  }
}
