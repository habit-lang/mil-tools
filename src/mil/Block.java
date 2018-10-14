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
import compiler.Handler;
import compiler.Position;
import core.*;
import java.io.PrintWriter;

public class Block extends Defn {

  private String id;

  private Temp[] params;

  private Code code;

  /** Default constructor. */
  public Block(Position pos, String id, Temp[] params, Code code) {
    super(pos);
    this.id = id;
    this.params = params;
    this.code = code;
  }

  private static int count = 0;

  public Block(Position pos, Temp[] params, Code code) {
    this(pos, "b" + count++, params, code);
  }

  public Block(Position pos, Code code) {
    this(pos, (Temp[]) null, code);
  }

  /**
   * Set the code field for this block; this is intended to be used in situations where we are
   * generating code for recursively defined blocks whose Code cannot be constucted until the Block
   * itself has been defined.
   */
  public void setCode(Code code) {
    this.code = code;
  }

  private BlockType declared;

  private BlockType defining;

  /** Get the declared type, or null if no type has been set. */
  public BlockType getDeclared() {
    return declared;
  }

  /** Set the declared type. */
  public void setDeclared(BlockType declared) {
    this.declared = declared;
  }

  /** Return the identifier that is associated with this definition. */
  public String getId() {
    return id;
  }

  public String toString() {
    return id;
  }

  /** Find the list of Defns that this Defn depends on. */
  public Defns dependencies() {
    return code.dependencies(null);
  }

  String dotAttrs() {
    return "style=filled, fillcolor=lightblue";
  }

  /** Display a printable representation of this definition on the specified PrintWriter. */
  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    if (declared != null) {
      if (isEntrypoint) {
        out.print("entrypoint ");
      }
      out.println(id + " :: " + declared);
    }

    Temps ts = renameTemps ? Temps.push(params, null) : null;
    Call.dump(out, id, "[", params, "]", ts);
    out.println(" =");
    if (code == null) {
      Code.indent(out);
      out.println("null");
    } else {
      code.dump(out, ts);
    }
  }

  BlockType instantiate() {
    return (declared != null) ? declared.instantiate() : defining;
  }

  /**
   * Set the initial type for this definition by instantiating the declared type, if present, or
   * using type variables to create a suitable skeleton. Also sets the types of bound variables.
   */
  void setInitialType() throws Failure {
    // Rename to ensure that every block has a distinct set of temporaries:
    Temp[] old = params;
    params = Temp.makeTemps(old.length);
    code = code.forceApply(TempSubst.extend(old, params, null));

    // Set initial types for temporaries:
    Type dom = Type.tuple(Type.freshTypes(params));
    if (declared == null) {
      Type rng = new TVar(Tyvar.tuple);
      defining = new BlockType(dom, rng);
    } else {
      defining = declared.instantiate();
      defining.domUnifiesWith(pos, dom);
    }
  }

  /**
   * Type check the body of this definition, but reporting rather than throwing an exception error
   * if the given handler is not null.
   */
  void checkBody(Handler handler) throws Failure {
    try {
      checkBody(pos);
    } catch (Failure f) {
      // We can recover from a type error in this definition (at least for long enough to type
      // check other definitions) if the types are all declared (and there is a handler).
      if (allTypesDeclared() && handler != null) {
        handler.report(f); // Of course, we still need to report the failure
        defining = null; // Mark this definition as having failed to check
      } else {
        throw f;
      }
    }
  }

  /** Type check the body of this definition, throwing an exception if there is an error. */
  void checkBody(Position pos) throws Failure {
    code.inferType(pos).unify(pos, defining.rngType());
  }

  /** Check that there are declared types for all of the items defined here. */
  boolean allTypesDeclared() {
    return declared != null;
  }

  /** Lists the generic type variables for this definition. */
  protected TVar[] generics = TVar.noTVars;

  void generalizeType(Handler handler) throws Failure {
    if (defining != null) {
      TVars gens = defining.tvars();
      generics = TVar.generics(gens, null);
      BlockType inferred = defining.generalize(generics);
      debug.Log.println("Inferred " + id + " :: " + inferred);
      if (declared != null && !declared.alphaEquiv(inferred)) {
        throw new Failure(
            pos,
            "Declared type \""
                + declared
                + "\" for \""
                + id
                + "\" is more general than inferred type \""
                + inferred
                + "\"");
      } else {
        declared = inferred;
      }
      findAmbigTVars(handler, gens); // search for ambiguous type variables ...
    }
  }

  void findAmbigTVars(Handler handler, TVars gens) {
    String extras = TVars.listAmbigTVars(code.tvars(gens), gens);
    if (extras != null) {
      // TODO: do we need to apply a skeleton() method to defining?
      // handler.report(new Failure(pos,  ...));
      debug.Log.println( // TODO: replace this call with the handler above ...
          "Block \""
              + id
              + "\" used at type "
              + defining
              + " with ambiguous type variables "
              + extras);
    }
  }

  /** Generate code to invoke the main definition, if it is a block with no parameters. */
  void callMain(MachineBuilder builder) {
    if (params.length == 0) {
      builder.call(0, this);
    }
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    /* skip these definitions on first pass */
  }

  /** Second pass code generation: produce code for block and closure definitions. */
  void generateFunctions(MachineBuilder builder) {
    builder.resetFrame();
    builder.setAddr(this, builder.getNextAddr());
    builder.extend(params, 0);
    code.generateCode(builder, params.length);
  }

  /** Stores the list of blocks that have been derived from this block. */
  private Blocks derived = null;

  /**
   * Derive a new version of this block using a code sequence that applies its final result to a
   * specifed argument value instead of returning that value (presumably a closure) to the calling
   * code where it is immediately entered and then discarded. The parameter m determines the number
   * of additional arguments that will eventually be passed when the closure is entered.
   */
  public Block deriveWithEnter(int m) {
    // Look to see if we have already derived a suitable version of this block:
    for (Blocks bs = derived; bs != null; bs = bs.next) {
      if (bs.head instanceof BlockWithEnter) {
        return bs.head;
      }
    }

    // Generate a fresh block; we have to make sure that the new block is added to the derived list
    // before we begin
    // generating code to ensure that we do not end up with multiple (or potentially, infinitely
    // many copies of the
    // new block).
    Temp[] iargs = Temp.makeTemps(m); // temps for extra args
    Temp[] nps = Temp.append(params, iargs); // added to original params
    Block b = new BlockWithEnter(pos, nps, null);
    derived = new Blocks(b, derived);
    b.code = code.deriveWithEnter(iargs);
    return b;
  }

  /**
   * Derive a new version of this block using a code sequence that passes its final result to a
   * specifed continuation function instead of returning that value to the calling code.
   */
  public Block deriveWithCont() {
    // Look to see if we have already derived a suitable version of this block:
    for (Blocks bs = derived; bs != null; bs = bs.next) {
      if (bs.head instanceof BlockWithCont) {
        return bs.head;
      }
    }

    // Generate a fresh block; we have to make sure that the new block is added to the derived list
    // before we
    // begin generating code to ensure that we do not end up with multiple (or potentially,
    // infinitely many
    // copies of the new block).

    Temp arg = new Temp(); // represents continuation
    int l = params.length; // extend params with arg
    Temp[] nps = new Temp[l + 1];
    nps[l] = arg;
    for (int i = 0; i < l; i++) {
      nps[i] = params[i];
    }
    Block b = new BlockWithCont(pos, nps, null);
    derived = new Blocks(b, derived);
    b.code = code.deriveWithCont(arg);
    return b;
  }

  /**
   * Heuristic to determine if this block is a good candidate for the casesOn(). TODO: investigate
   * better functions for finding candidates!
   */
  boolean contCand() {
    // Warning: an scc for this block may not be known if this block has only just been created.
    DefnSCC scc = getScc();
    return code.isDone() == null
        // && !(this instanceof BlockWithKnownCons)
        && scc != null
        && scc.contCand();
  }

  public Block deriveWithKnownCons(Call[] calls) {

    // Do not create a specialized version of a simple block (i.e., a block that contains only a
    // single Done):
    //    if (code instanceof Done) {
    // System.out.println("Will not specialize this block: code is a single tail");
    //      return null;
    //  }

    // TODO: this test is disabled, which results in more aggressive inlining that, so
    // far, appears to be a good thing :-)  Consider removing this test completely ... ?
    //  if (this instanceof BlockWithKnownCons) {
    //      return null;
    //  }

    // Look to see if we have already derived a suitable version of this block:
    for (Blocks bs = derived; bs != null; bs = bs.next) {
      if (bs.head.hasKnownCons(calls)) {
        // Return pointer to previous occurrence, or decline the request to specialize
        // the block if the original block already has the requested allocator pattern.
        return (this == bs.head) ? null : bs.head;
      }
    }

    // Generate a fresh block; unlike the case for trailing Enter, we're only going to create one
    // block here
    // whose code is the same as the original block except that it adds a group of one or more
    // initializers.
    // Our first step is to initialize the block:
    Block b = new BlockWithKnownCons(pos, null, calls);
    derived = new Blocks(b, derived);

    // Next we pick temporary variables for new parameters:
    Temp[][] tss = Call.makeTempsFor(calls);

    // Combine old parameters and new temporaries to make new parameter list:
    if (tss == null) {
      b.params = params; // TODO: safe to reuse params, or should we make a copy?
      b.derived = new Blocks(b, b.derived);
    } else {
      b.params = mergeParams(tss, params);
    }

    // Fill in the code for the new block by prepending some initializers:
    b.code = addInitializers(calls, params, tss, code.copy());
    b.flow(); // perform an initial flow analysis to inline initializers.

    return b;
  }

  boolean hasKnownCons(Call[] calls) {
    return false;
  }

  public Block deriveWithDuplicateArgs(int[] dups) {
    if (dups == null) {
      debug.Internal.error("null argument for deriveWithDuplicateArgs");
    }

    // Look to see if we have already derived a suitable version of this block:
    for (Blocks bs = derived; bs != null; bs = bs.next) {
      if (bs.head.hasDuplicateArgs(dups)) {
        // Return pointer to previous occurrence:
        return bs.head;
      }
    }

    // Count the number of duplicate params to remove so that we can determine
    // how many formal parameters the derived block should have.
    int numDups = 0;
    for (int i = 0; i < dups.length; i++) {
      if (dups[i] != 0) {
        numDups++;
      }
    }
    if (numDups == 0) {
      debug.Internal.error("no duplicates found for deriveWithDuplicateArgs");
    } else if (numDups >= params.length) {
      debug.Internal.error("too many duplicates in deriveWithDuplicateArgs");
    }

    // Create a new list of params (a subsequence of the original list) and build a substitution to
    // describe
    // what will happen to params that are eliminated as duplicates.
    Temp[] nps = Temp.makeTemps(params.length - numDups);
    int j = 0;
    TempSubst s = null;
    for (int i = 0; i < dups.length; i++) {
      if (dups[i] == 0) { // Not a duplicated parameter:
        s = params[i].mapsTo(nps[j++], s); // - map old to new
      } else { // Duplicated parameter:
        s = params[i].mapsTo(params[dups[i] - 1].apply(s), s); // - map to where original went
      }
    }

    Block b = new BlockWithDuplicateArgs(pos, nps, code.forceApply(s), dups);
    // TODO: should we set a declared type for b if this block has one?
    derived = new Blocks(b, derived);
    return b;
  }

  /**
   * Check to see if this is a derived version of a block with duplicate arguments that matches the
   * given pattern.
   */
  boolean hasDuplicateArgs(int[] dups) {
    return false;
  }

  /**
   * Flag to identify blocks that "do not return". In other words, if the value of this flag for a
   * given block b is true, then we can be sure that (x <- b[args]; c) == b[args] for any
   * appropriate set of arguments args and any valid code sequence c. There are two situations that
   * can cause a block to "not return". The first is when the block enters an infinite loop; such
   * blocks may still be productive (such as the block defined by b[x] = (_ <- print((1)); b[x])),
   * so we cannot assume that they will be eliminated by eliminateLoops(). The second is when the
   * block's code sequence makes a call to a primitive call that does not return.
   */
  private boolean doesntReturn = false;

  /**
   * Return flag, computed by previous dataflow analysis, to indicate if this block does not return.
   */
  boolean doesntReturn() {
    return doesntReturn;
  }

  /**
   * Reset the doesntReturn flag, if there is one, for this definition ahead of a returnAnalysis().
   * For this analysis, we use true as the initial value, reducing it to false if we find a path
   * that allows a block's code to return.
   */
  void resetDoesntReturn() {
    this.doesntReturn = true;
  }

  /**
   * Apply return analysis to this definition, returning true if this results in a change from the
   * previously computed value.
   */
  boolean returnAnalysis() {
    boolean newDoesntReturn = code.doesntReturn();
    if (newDoesntReturn != doesntReturn) {
      doesntReturn = newDoesntReturn;
      return true; // signal that a change was detected
    }
    return false; // no change
  }

  boolean detectLoops(Blocks visited) {
    // Check to see if this block calls code for an already visited block:
    if (Blocks.isIn(this, visited) || code.detectLoops(this, visited)) {
      MILProgram.report("detected an infinite loop in block " + getId());
      code = new Done(Prim.loop.withArgs());
      return true;
    }
    return false;
  }

  /** Perform pre-inlining cleanup on each Block in this SCC. */
  void cleanup() {
    code = code.cleanup(this);
  }

  /** Apply inlining. */
  public void inlining() {
    if (isGotoBlock() == null || isEntrypoint) { // TODO: consider replacing with code.isDone()
      code = code.inlining(this);
    }
  }

  public static final int INLINE_LINES_LIMIT = 6;

  boolean canPrefixInline(Block src) {
    if (this.getScc() != src.getScc()) { // Restrict to different SCCs
      int n = code.prefixInlineLength();
      return n > 0 && (occurs == 1 || n <= INLINE_LINES_LIMIT);
    }
    return false;
  }

  /**
   * Attempt to inline the code for this block onto the front of another block of code. Assumes that
   * the final result computed by this block will be bound to the variables in rs, and that the
   * computation will proceed with the code specified by rest. The src value specifies the block in
   * which the original BlockCall appeared while args specifies the set of arguments that were
   * passed in at that call. A null return indicates that no inlining was performed.
   */
  Code prefixInline(Block src, Atom[] args, Temp[] rs, Code rest) {
    if (canPrefixInline(src)) {
      MILProgram.report(
          "prefixInline succeeded for call to block " + getId() + " from block " + src.getId());
      return code.prefixInline(TempSubst.extend(params, args, null), rs, rest);
    }
    return null;
  }

  /**
   * Attempt to construct an inlined version of the code in this block that can be placed at the end
   * of a Code sequence. Assumes that a BlockCall to this block with the given set of arguments was
   * included in the specified src Block. A null return indicates that no inlining was performed.
   */
  Code suffixInline(Block src, Atom[] args) {
    if (canSuffixInline(src)) {
      MILProgram.report(
          "suffixInline succeeded for call to block " + getId() + " from block " + src.getId());
      return code.apply(TempSubst.extend(params, args, null));
    }
    return null;
  }

  /**
   * We allow a block to be inlined if the original call is in a different block, the code for the
   * block ends with a Done, and either there is only one reference to the block in the whole
   * program, or else the length of the code sequence is at most INLINE_LINES_LIMIT lines long.
   */
  boolean canSuffixInline(Block src) {
    if (doesntReturn && getScc().isRecursive()) { // Avoid loopy code that doesn't return
      return false;
    } else if (occurs == 1 || code.isDone() != null) { // Inline single occurrences and trivial
      return true; // blocks (safe, as a result of removing loops)
    } else if (!this.guarded(src)) { // Don't inline if not guarded.
      return false;
    } else {
      int n = code.suffixInlineLength(0); // Inline code blocks that are short
      return n > 0 && n <= INLINE_LINES_LIMIT;
    }
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return (this != src) && (this.getScc() != src.getScc() || code.guarded(src));
  }

  /** Test to see if a call to this block with specific arguments can be replaced with a Call. */
  public Tail inlineTail(Atom[] args) {
    Tail tail = code.isDone();
    if (tail != null) {
      tail = tail.forceApply(TempSubst.extend(params, args, null));
    }
    return tail;
  }

  /**
   * Rewrite a call to a goto Block as a new BlockCall that bypasses the goto. In other words, if
   * b[p1,...] = b'[a1, ...] then we can rewrite a call of the form b[x1,...] as a call to
   * [x1/p1,...]b'[a1,...]. If the block b is not a goto block, or if we call this method on a tail
   * that is not a block call, then a null value will be returned.
   */
  BlockCall bypassGotoBlockCall(Atom[] args) {
    BlockCall bc = this.isGotoBlock();
    if (bc != null) {
      MILProgram.report("elided call to goto block " + getId());
      return bc.forceApplyBlockCall(TempSubst.extend(params, args, null));
    }
    return null;
  }

  /**
   * Test to determine whether this Block is a "goto" block, meaning that: a) its body is an
   * immediate call to another block; and b) either this block has parameters or else the block in
   * its body has no arguments. (A block defined by b[] = b'[a1,...] is considered to be an "entry"
   * block rather than a "goto" block.)
   */
  BlockCall isGotoBlock() {
    return code.isGoto(params.length);
  }

  void liftAllocators() {
    code.liftAllocators();
  }

  /**
   * A bitmap that identifies the used arguments of this definition. The base case, with no used
   * arguments, can be represented by a null array. Otherwise, it will be a non null array, the same
   * length as the list of parameters, with true in positions corresponding to arguments that are
   * known to be used and false in all other positions.
   */
  private boolean[] usedArgs = null;

  /**
   * Counts the total number of used arguments in this definition; this should match the number of
   * true values in the usedArgs array.
   */
  private int numUsedArgs = 0;

  /** Reset the bitmap and count for the used arguments of this definition, where relevant. */
  void clearUsedArgsInfo() {
    usedArgs = null;
    numUsedArgs = 0;
  }

  /**
   * Count the number of unused arguments for this definition using the current unusedArgs
   * information for any other items that it references.
   */
  int countUnusedArgs() {
    return countUnusedArgs(params);
  }

  /**
   * Count the number of unused arguments for this definition. A zero count indicates that all
   * arguments are used.
   */
  int countUnusedArgs(Temp[] dst) {
    if (isEntrypoint) { // treat all entrypoint arguments as used
      // We don't have to set numUsedArgs and usedArgs because all uses are guarded by isEntrypoint
      // tests
      return 0;
    } else {
      int unused = dst.length - numUsedArgs; // count # of unused args
      if (unused > 0) { // skip if no unused args
        usedVars = usedVars(); // find vars used in body
        for (int i = 0; i < dst.length; i++) { // scan argument list
          if (usedArgs == null || !usedArgs[i]) { // skip if already known to be used
            if (dst[i].isIn(usedVars) && !duplicated(i, dst)) {
              if (usedArgs == null) { // initialize usedArgs for first use
                usedArgs = new boolean[dst.length];
              }
              usedArgs[i] = true; // mark this argument as used
              numUsedArgs++; // update counts
              unused--;
            }
          }
        }
      }
      return unused;
    }
  }

  private Temps usedVars;

  /**
   * A utility function that returns true if the variable at position i in the given array also
   * appears in some earlier position in the array. (If this condition applies, then we can mark the
   * later occurrence as unused; there is no need to pass the same variable twice.)
   */
  private static boolean duplicated(int i, Temp[] dst) {
    // Did this variable appear in an earlier position?
    for (int j = 0; j < i; j++) {
      if (dst[j] == dst[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Find the list of variables that are used in this definition. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  Temps usedVars() {
    return code.usedVars();
  }

  /**
   * Find the list of variables that are used in a call to this definition, taking account of the
   * usedArgs setting so that we only include variables appearing in argument positions that are
   * known to be used.
   */
  Temps usedVars(Atom[] args, Temps vs) {
    if (isEntrypoint) { // treat all entrypoint arguments as used
      for (int i = 0; i < args.length; i++) {
        vs = args[i].add(vs);
      }
    } else if (usedArgs != null) { // ignore this call if no args are used
      for (int i = 0; i < args.length; i++) {
        if (usedArgs[i]) { // ignore this argument if the flag is not set
          vs = args[i].add(vs);
        }
      }
    }
    return vs;
  }

  /**
   * Use information about which and how many argument positions are used to trim down an array of
   * destinations (specifically, the formal parameters of a Block or a ClosureDefn).
   */
  Temp[] removeUnusedTemps(Temp[] dsts) {
    if (!isEntrypoint && numUsedArgs < dsts.length) { // Found some new, unused args
      Temp[] newTemps = new Temp[numUsedArgs];
      int j = 0;
      for (int i = 0; i < dsts.length; i++) {
        if (usedArgs != null && usedArgs[i]) {
          newTemps[j++] = dsts[i];
        } else {
          MILProgram.report("removing unused argument " + dsts[i] + " from " + getId());
        }
      }
      return newTemps;
    }
    return dsts; // No newly discovered unused arguments
  }

  /**
   * Update an argument list by removing unused arguments, or return null if no change is required.
   */
  Atom[] removeUnusedArgs(Atom[] args) {
    if (!isEntrypoint
        && numUsedArgs < args.length) { // Only rewrite if we have found some new unused arguments
      Atom[] newArgs = new Atom[numUsedArgs];
      int j = 0;
      for (int i = 0; i < args.length; i++) {
        if ((usedArgs != null && usedArgs[i])) {
          newArgs[j++] = args[i];
        }
      }
      return newArgs;
    }
    return null; // The argument list should not be changed
  }

  /** Rewrite this program to remove unused arguments in block calls. */
  void removeUnusedArgs() {
    if (!isEntrypoint && numUsedArgs < params.length) {
      MILProgram.report(
          "Rewrote block "
              + getId()
              + " to eliminate "
              + (params.length - numUsedArgs)
              + " unused parameters");
      params = removeUnusedTemps(params); // remove unused formal parameters
      if (declared != null) {
        declared = declared.removeArgs(numUsedArgs, usedArgs);
      }
    }
    code = code.removeUnusedArgs(); // update calls in code sequence
  }

  public void flow() {
    code = code.flow(null /*facts*/, null /*substitution*/);
    code.liveness();
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return code.returnsFlag();
  }

  /**
   * Test to determine whether there is a way to short out a Case from a call to this block with the
   * specified arguments, and given the set of facts that have been computed. We start by querying
   * the code in the Block to determine if it starts with a Case; if not, then the optimization will
   * not apply and a null result is returned.
   */
  BlockCall shortCase(Atom[] args, Facts facts) {
    return code.shortCase(params, args, facts);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return id.hashCode();
  }

  /** Test to see if two Block values are alpha equivalent. */
  boolean alphaBlock(Block that) {
    // Check for same number of parameters:
    if (this.params.length != that.params.length) {
      return false;
    }

    // Build lists of parameters:
    Temps thisvars = null;
    Temps thatvars = null;
    for (int i = 0; i < this.params.length; i++) {
      thisvars = this.params[i].add(thisvars);
      thatvars = that.params[i].add(thatvars);
    }

    // Check bodies for alpha equivalence:
    return this.code.alphaCode(thisvars, that.code, thatvars);
  }

  /** Holds the most recently computed summary value for this definition. */
  private int summary;

  /**
   * Points to a different definition with equivalent code, if one has been identified. A null value
   * indicates that there is no replacement.
   */
  private Block replaceWith = null;

  Block getReplaceWith() {
    return replaceWith;
  }

  /**
   * Look for a previously summarized version of this definition, returning true iff a duplicate was
   * found.
   */
  boolean findIn(Blocks[] table) {
    summary = code.summary();
    int idx = this.summary % table.length;
    if (idx < 0) {
      idx += table.length;
    }

    for (Blocks ds = table[idx]; ds != null; ds = ds.next) {
      if (ds.head.summary == this.summary && ds.head.alphaBlock(this)) {
        if (isEntrypoint) { // Cannot replace an entrypoint, even though a replacement is available
          return false;
        } else if (ds.head.declared == null
            || (this.declared != null && ds.head.declared.alphaEquiv(this.declared))) {
          MILProgram.report("Replacing " + this.getId() + " with " + ds.head.getId());
          this.replaceWith = ds.head;
          return true;
        }
      }
    }

    // First sighting of this definition, add to the table:
    this.replaceWith = null; // There is no replacement for this definition (yet)
    table[idx] = new Blocks(this, table[idx]);
    return false;
  }

  /**
   * Compute a summary for this definition (if it is a block, top-level, or closure) and then look
   * for a previously encountered item with the same code in the given table. Return true if a
   * duplicate was found.
   */
  boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels, ClosureDefns[] closures) {
    return findIn(blocks);
  }

  void eliminateDuplicates() {
    code.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (declared != null) {
      declared = declared.canonBlockType(set);
    }
    if (defining != null) {
      defining = defining.canonBlockType(set);
    }
    Atom.collect(params, set);
    code.collect(set);
  }

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    code = code.cfunSimplify();
  }

  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  Block isBlockOfType(BlockType inst) {
    return declared.alphaEquiv(inst) ? this : null;
  }

  Block(Block b, int num) {
    this(b.pos, mkid(b.id, num), null, null);
  }

  /** Fill in the body of this definition as a specialized version of the given block. */
  void specialize(MILSpec spec, Block borig) {
    TVarSubst s = borig.declared.specializingSubst(borig.generics, this.declared);
    debug.Log.println(
        "Block specialize: "
            + borig.getId()
            + " :: "
            + borig.declared
            + "  ~~>  "
            + this.getId()
            + " :: "
            + this.declared
            + ", generics="
            + TVar.show(borig.generics)
            + ", substitution="
            + s);
    this.params = Temp.specialize(s, borig.params);
    SpecEnv env = new SpecEnv(borig.params, this.params, null);
    this.code = borig.code.specializeCode(spec, s, env);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    BlockType bt = declared.isMonomorphic();
    if (bt != null) {
      Block b = spec.specializedBlock(this, bt);
      b.id = this.id; // use the same name as in the original program
      return b;
    }
    throw new PolymorphicEntrypointFailure("block", this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonBlockType(spec);
  }

  void bitdataRewrite(BitdataMap m) {
    code = code.bitdataRewrite(m);
  }

  void setParams(Temp[] params) {
    this.params = params;
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    Temp[][] npss = Temp.reps(params); // analyze params
    RepEnv env = Temp.extend(params, npss, null); // environment for params
    params = Temp.repParams(params, npss);
    code = code.repTransform(set, env);
    declared = declared.canonBlockType(set);
  }

  Tail makeTail() throws Failure {
    return (params.length == 0) ? new BlockCall(this, Atom.noAtoms) : super.makeTail();
  }

  public static Block returnTrue = atomBlock("returnTrue", Flag.True);

  public static Block returnFalse = atomBlock("returnFalse", Flag.False);

  /**
   * Make a block of the following form that immediately returns the atom a, which could be an Word
   * or a Top, but not a Temp (because that would be out of scope). b :: [] >>= [t] b[] = return a
   */
  public static Block atomBlock(String name, Atom a) {
    return new Block(BuiltinPosition.pos, name, Temp.noTemps, new Done(new Return(a)));
  }

  /**
   * Perform scope analysis on a definition of this block, creating a new temporary for each of the
   * input parameters and checking that all identifiers used in the given code sequence have a
   * corresponding binding.
   */
  public void inScopeOf(Handler handler, MILEnv milenv, String[] ids, CodeExp cexp) throws Failure {
    Temp[] ps = Temp.makeTemps(ids.length);
    this.params = ps;
    this.code = cexp.inScopeOf(handler, milenv, new TempEnv(ids, ps, null));
  }

  /** Add this exported definition to the specified MIL environment. */
  void addExport(MILEnv exports) {
    exports.addBlock(id, this);
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  Temp[] addArgs() throws Failure {
    if (params == null) { // compute formal params on first visit
      params = Temps.toArray(code.addArgs());
    }
    return params;
  }

  /** Returns the LLVM type for value that is returned by a function. */
  llvm.Type retType(LLVMMap lm) {
    return declared.retType(lm);
  }

  llvm.Global blockGlobalCalc(LLVMMap lm) {
    return new llvm.Global(declared.toLLVM(lm), label());
  }

  int numberCalls;

  public int getNumberCalls() {
    return numberCalls;
  }

  /**
   * Reset the counter for the number of non-tail calls to this definition. This is only useful for
   * Blocks: we have to generate an LLVM function for every reachable closure definition anyway, but
   * we only need to do this for Blocks that are either listed as entrypoints or that are accessed
   * via a non-tail call somewhere in the reachable program.
   */
  void resetCallCounts() {
    numberCalls = 0;
  }

  /** Mark this definition as having been called. */
  void called() {
    numberCalls++;
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    code.countCalls();
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    code.countAllCalls();
  }

  /**
   * Identify the set of blocks that should be included in the function that is generated for this
   * definition. A block call in the tail for a TopLevel is considered a regular call (it will
   * likely be called from the initialization code), but a block call in the tail for a ClosureDefn
   * is considered a genuine tail call. For a Block, we only search for the components of the
   * corresponding function if the block is the target of a call.
   */
  Blocks identifyBlocks() {
    return (numberCalls == 0) ? null : code.identifyBlocks(this, new Blocks(this, null));
  }

  Blocks identifyBlocks(Blocks bs) {
    return code.identifyBlocks(this, bs);
  }

  /**
   * Determine if this block is "small", which is intended to capture the intuition that this block
   * will not do a lot of work before returning. In concrete, albeit somewhat arbitrary terms, we
   * define this to mean that the block is not part of a recursive scc, and that it does at most
   * SMALL_STEPS Bind instructions before reaching a Done.
   */
  boolean isSmall() {
    return !getScc().isRecursive() && code.isSmall(SMALL_STEPS);
  }

  private static final int SMALL_STEPS = 4;

  /** Return a string label that can be used to identify this node. */
  String label() {
    return isEntrypoint ? id : ("func_" + id);
  }

  CFG makeCFG() {
    if (numberCalls == 0) {
      return null;
    } else {
      // The entry point and the first block should use different parameter names (but the number
      // and type of the parameters should be the same):
      Temp[] nparams = new Temp[params.length];
      for (int i = 0; i < params.length; i++) {
        nparams[i] = params[i].newParam();
      }
      BlockCFG cfg = new BlockCFG(this, Temp.nonUnits(nparams));
      cfg.initCFG();
      return cfg;
    }
  }

  /** Find the CFG successors for this definition. */
  Label[] findSuccs(CFG cfg, Node src) {
    return code.findSuccs(cfg, src);
  }

  TempSubst mapParams(Atom[] args, TempSubst s) {
    return TempSubst.extend(Temp.nonUnits(params), TempSubst.apply(args, s), s);
  }

  /**
   * Construct a function definition with the given formal parameters and code, filling in an
   * appropriate code sequence for the entry block in cs[0], and setting the appropriate type and
   * internal flag values.
   */
  llvm.FuncDefn toLLVMFuncDefn(
      LLVMMap lm,
      DefnVarMap dvm,
      TempSubst s,
      llvm.Local[] formals,
      String[] ss,
      llvm.Code[] cs,
      Label[] succs) {
    cs[0] = dvm.loadGlobals(new llvm.Goto(succs[0].label()));
    return new llvm.FuncDefn(llvm.Mods.entry(isEntrypoint), retType(lm), label(), formals, ss, cs);
  }

  /**
   * Generate LLVM code for this Block suitable for use as a labeled block inside a function
   * definition.
   */
  llvm.Code toLLVMBlock(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return code.toLLVMCode(lm, vm, s, succs);
  }

  Temp[] getParams() {
    return params;
  }

  /**
   * Calculate the LLVM return type that will be produced by the code in the main Block of a
   * program, if one has been specified.
   */
  llvm.Type initType(LLVMMap lm) throws Failure {
    return retType(lm);
  }

  /** Generate an LLVM code sequence from the main Block in a program, if one has been specified. */
  llvm.Code initCode(LLVMMap lm, InitVarMap ivm) throws Failure {
    return (params.length != 0)
        ? super.initCode(lm, ivm)
        : code.toLLVMCode(lm, ivm, null, Label.noLabels);
  }
}
