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

public class BlockCall extends Call {

  private final Block b;

  /** Default constructor. */
  public BlockCall(final Block b) {
    this.b = b;
    b.calledFrom(this);
  }

  /**
   * Specify block and arguments in a single constructor call (for situations where use of
   * withArgs() would loose type information).
   */
  public BlockCall(Block b, Atom[] args) {
    this(b);
    this.args = args;
  }

  /** Test if two Tail expressions are the same. */
  boolean sameTail(Tail that) {
    return that.sameBlockCall(this);
  }

  boolean sameBlockCall(BlockCall that) {
    return this.b == that.b && this.sameArgs(that);
  }

  /** Find the dependencies of this AST fragment. */
  Defns dependencies(Defns ds) {
    return b.dependencies(super.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  void dump(PrintWriter out, Temps ts) {
    dump(out, b.toString(), "[", args, "]", ts);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new BlockCall(b, args);
  }

  /**
   * A special version of apply that works only on BlockCalls; used in places where type
   * preservation of a BlockCall argument is required. In particular, we don't use withArgs here
   * because that loses type information, producing a Body from a BlockCall input.
   */
  BlockCall applyBlockCall(TempSubst s) {
    return new BlockCall(b, TempSubst.apply(args, s));
  }

  private BlockType type;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  Type resultType() {
    return type.rngType();
  }

  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    type = b.instantiate();
    return type.apply(pos, inputs);
  }

  void invokeCall(MachineBuilder builder, int o) {
    builder.call(o, b);
  }

  /**
   * Generate code for a Tail that appears in tail position (i.e., at the end of a code sequence).
   * The parameter o specifies the offset of the next unused location in the current frame. For
   * BlockCall and Enter, in particular, we can jump to the next function instead of doing a call
   * followed by a return.
   */
  void generateTailCode(MachineBuilder builder, int o) {
    generateTailArgs(builder);
    builder.jump(b);
  }

  /** Test to determine if this Tail is a BlockCall. */
  public BlockCall isBlockCall() {
    return this;
  }

  /**
   * Generate a new version of this block call by passing the specified argument to a derived block
   * with a trailing enter.
   */
  BlockCall deriveWithEnter(Atom[] iargs) {
    return new BlockCall(b.deriveWithEnter(iargs.length), Atom.append(args, iargs));
  }

  /**
   * Generate a new version of this block call by passing the specified continuation argument to a
   * derived block with a trailing continuation invocation.
   */
  public BlockCall deriveWithCont(Atom cont) {
    int l = args.length;
    Atom[] nargs = new Atom[l + 1]; // extend args with arg
    for (int i = 0; i < l; i++) {
      nargs[i] = args[i];
    }
    nargs[l] = cont;
    return new BlockCall(b.deriveWithCont(), nargs);
  }

  /**
   * Heuristic to determine if this block is a good candidate for the casesOn(). TODO: investigate
   * better functions for finding candidates!
   */
  boolean contCand() {
    return b.contCand();
  }

  /**
   * Test to see if a (non tail) block call in the code for the given src block could be interpreted
   * as an entry to a local loop.
   */
  Code localLoop(Block src, Temp[] vs, Code c) {
    DefnSCC srcScc = src.getScc();
    DefnSCC scc = b.getScc();
    if (srcScc != null
        && scc != null
        && srcScc != scc
        && b.getOccurs() == 1
        && scc.localLoopSCC()) {
      Temp t = new Temp();
      return new Bind(t, c.makeCont(vs), new Done(this.deriveWithCont(t)));
    }
    return null;
  }

  boolean noCallsWithinSCC(DefnSCC scc) {
    return b.getScc() != scc;
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.sameBlockCallForm(this);
  }

  boolean sameBlockCallForm(BlockCall that) {
    return that.b == this.b;
  }

  BlockCall deriveWithKnownCons(Defn d, Call[] calls) {
    if (calls.length != args.length) {
      debug.Internal.error("BlockCall argument list length mismatch in deriveWithKnownCons");
    }
    Block nb = b.deriveWithKnownCons(calls);
    return (nb == null) ? null : new BlockCall(nb, specializedArgs(calls));
  }

  /** Generate a new version of a block call that omits duplicate arguments. */
  public BlockCall deriveWithDuplicateArgs(int[] dups) {
    if (dups.length != args.length) {
      debug.Internal.error("argument list length mismatch in deriveWithDuplicateArgs");
    }
    Block nb = b.deriveWithDuplicateArgs(dups);
    if (nb == null) {
      return null;
    } else {
      return new BlockCall(nb, removeDuplicateArgs(dups));
    }
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return b.doesntReturn();
  }

  boolean detectLoops(
      Block src, Blocks visited) { // Keep searching while we're still in the same SCC
    return (src.getScc() == b.getScc()) && b.detectLoops(new Blocks(src, visited));
  }

  /**
   * Return the number of occurrences of the block mentioned in this call; if the result is one,
   * then this must be the only call to the block in this program.
   */
  int getOccurs() {
    return b.getOccurs();
  }

  Code prefixInline(Block src, Temp[] rs, Code c) {
    return b.prefixInline(src, args, rs, c);
  }

  /**
   * Perform suffix inlining on this tail, which either replaces a block call with an appropriately
   * renamed copy of the block's body, or else returns null if the tail is either not a block call,
   * or if the code of the block is not suitable for inlining.
   */
  Code suffixInline(Block d) {
    return b.suffixInline(d, args);
  }

  Code forceSuffixInline(Block d) {
    MILProgram.report("forced suffixInline for call to block " + b + " from block " + d);
    return b.forceSuffixInline(args);
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return b.guarded(src);
  }

  /**
   * Skip goto blocks in a Tail (for a ClosureDefn or TopLevel). TODO: can this be simplified now
   * that ClosureDefns hold Tails rather than Calls?
   */
  public Tail inlineTail() {
    BlockCall bc = this.inlineBlockCall();
    Tail tail = bc.b.inlineTail(bc.args);
    return (tail == null) ? bc : tail;
  }

  /**
   * Inlining for BlockCalls, replacing a call to a "goto" block with a direct call to the target of
   * that goto, or else returning the original block call unchanged.
   */
  public BlockCall inlineBlockCall() {
    BlockCall bc = b.bypassGotoBlockCall(args);
    return (bc != null) ? bc : this;
  }

  BlockCall bypassGotoBlockCall() {
    return b.bypassGotoBlockCall(args);
  }

  BlockCall isGoto(int numParams) {
    return (numParams > 0 || args.length == 0) ? this : null;
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return b.usedVars(args, vs);
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return args.length == 0 ? b.returnsFlag() : null;
  }

  public Code rewrite(Defn d, Facts facts) {
    BlockCall bc = rewriteBlockCall(d, facts, false);
    return (bc == this) ? null : new Done(bc); // TODO: worried about this == test
  }

  Tail rewriteTail(Defn d, Facts facts, boolean allowWord) {
    return this.rewriteBlockCall(d, facts, allowWord);
  }

  BlockCall rewriteBlockCall(Defn d, Facts facts, boolean allowWord) {
    // Look for an opportunity to short out a Case if this block branches to a Case for a variable
    // that has a known DataAlloc value in the current set of facts.
    BlockCall bc = this.shortCase(facts);
    bc = (bc == null) ? this : bc.inlineBlockCall();

    // Look for an opportunity to specialize on known constructors:
    // TODO: When we find, b16[t109, id, t110]{-, k35{}, -}, it isn't necessarily a good idea to
    // create a specialized block if the id <- k35{} line appears in this block (i.e., if id is
    // local, not a top level value) and id is used elsewhere in the block.
    Call[] calls = bc.b.collectCalls(bc.args, facts, allowWord);
    if (calls != null) {
      BlockCall bc1 = bc.deriveWithKnownCons(d, calls);
      if (bc1 != null) {
        MILProgram.report(
            "deriving specialized block " + bc1.b + " for BlockCall to block " + bc.b);
        bc = bc1;
      }
    }

    // Look for an opportunity to simplify a BlockCall with duplicate arguments.
    int[] dups = bc.hasDuplicateArgs();
    if (dups != null) {
      BlockCall bc1 = bc.deriveWithDuplicateArgs(dups);
      if (bc1 != null) {
        bc = bc1;
        MILProgram.report("eliminating duplicate args in call within " + b);
      }
    }

    return bc;
  }

  BlockCall shortCase(Facts facts) {
    return b.shortCase(args, facts);
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
    updateSources(d, params, rebound, b);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return summary(b.summary()) * 33;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaBlockCall(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaBlockCall(Temps thisvars, BlockCall that, Temps thatvars) {
    return this.b == that.b && this.alphaArgs(thisvars, that, thatvars);
  }

  Tail eliminateDuplicates() {
    return eliminateDuplicatesBlockCall();
  }

  BlockCall eliminateDuplicatesBlockCall() {
    Block b1 = b.getReplaceWith();
    return (b1 == null) ? this : new BlockCall(b1, args);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonBlockType(set);
    }
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new BlockCall(spec.specializedBlock(b, type.apply(s)));
  }

  /**
   * Generate a specialized version of this BlockCall, with a type to guarantee that the result will
   * also be a BlockCall rather than just an arbitrary Tail.
   */
  BlockCall specializeBlockCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new BlockCall(
        spec.specializedBlock(b, type.apply(s)), Atom.specialize(spec, s, env, args));
  }

  BlockCall repTransformBlockCall(RepTypeSet set, RepEnv env) {
    return new BlockCall(b, Atom.repArgs(set, env, args));
  }

  /**
   * Find the argument variables that are used in this Tail, adding results to an accumulating list.
   * This is mostly just the same as adding the the variables defined in the Tail except that we
   * include updates in the cases for BlockCall and ClosAlloc if the argument lists are not already
   * known.
   */
  public Temps addArgs(Temps vs) throws Failure {
    return (args == null) ? Temps.add(args = b.addArgs(), vs) : vs;
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    b.called();
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    // Don't add to the list if this block is already included, or if the block that is called here
    // is in
    // a different scc and is the target of a (non tail) call elsewhere in the program.
    if (Blocks.isIn(b, bs) // Don't add if this block is already included
        || (b.getScc() != src.getScc() //  or if it is in a different SCC
                && b.getNumberCalls() != 0) //        that is called elsewhere as a regular function
            && !b.isSmall()) { //        and that is not small (i.e., it does nontrivial work)
      return bs;
    }
    return b.identifyBlocks(new Blocks(b, bs));
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return new Label[] {this.findSucc(cfg, src)};
  }

  /** Find the CFG successor for this item. */
  Label findSucc(CFG cfg, Node src) {
    return cfg.edge(src, b, Atom.nonUnits(args));
  }

  /** Generate LLVM code to execute this Tail in tail call position (i.e., as part of a Done). */
  llvm.Code toLLVMDone(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return new llvm.Goto(succs[0].label());
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    return new llvm.CallVoid(isTail, lm.globalFor(b), Atom.toLLVMValues(lm, vm, s, args), c);
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    return new llvm.Op(
        lhs,
        new llvm.Call(isTail, lhs.getType(), lm.globalFor(b), Atom.toLLVMValues(lm, vm, s, args)),
        c);
  }
}
