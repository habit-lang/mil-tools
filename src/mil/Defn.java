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

public abstract class Defn {

  protected Position pos;

  /** Default constructor. */
  public Defn(Position pos) {
    this.pos = pos;
  }

  public Position getPos() {
    return pos;
  }

  public abstract String toString();

  /**
   * Track whether this definition is considered an entrypoint to the program; it may be necessary
   * to consider this information during program optimization (e.g., the definition of an entrypoint
   * should not be modified in a way that changes its external interface, such as by deleting unused
   * parameters), or during code generation (where entrypoints may need to be marked in a different
   * way from parts of the code that are intended only for internal use).
   */
  protected boolean isEntrypoint = false;

  /** Return the flag to indicate whether this definition is considered to be an entrypoint. */
  public boolean isEntrypoint() {
    return isEntrypoint;
  }

  /** Set the flag to indicate whether this definition is considered to be an entrypoint. */
  public void setIsEntrypoint(boolean b) {
    isEntrypoint = b;
  }

  /** Records the successors/callees of this node. */
  private Defns callees = null;

  /** Records the predecessors/callers of this node. */
  private Defns callers = null;

  /** Update callees/callers information with dependencies. */
  public void calls(Defns xs) {
    for (callees = xs; xs != null; xs = xs.next) {
      xs.head.callers = new Defns(this, xs.head.callers);
    }
  }

  /**
   * Flag to indicate that this node has been visited during the depth-first search of the forward
   * dependency graph.
   */
  private boolean visited = false;

  /** Visit this X during a depth-first search of the forward dependency graph. */
  Defns forwardVisit(Defns result) {
    if (!this.visited) {
      this.visited = true;
      return new Defns(this, Defns.searchForward(this.callees, result));
    }
    return result;
  }

  /**
   * Records the binding scc in which this binding has been placed. This field is initialized to
   * null but is set to the appropriate binding scc during dependency analysis.
   */
  private DefnSCC scc = null;

  /** Return the binding scc that contains this binding. */
  public DefnSCC getScc() {
    return scc;
  }

  /**
   * Visit this binding during a depth-first search of the reverse dependency graph. The scc
   * parameter is the binding scc in which all unvisited bindings that we find should be placed.
   */
  void reverseVisit(DefnSCC scc) {
    if (this.scc == null) {
      // If we arrive at a binding that hasn't been allocated to any SCC,
      // then we should put it in this SCC.
      this.scc = scc;
      scc.add(this);
      for (Defns callers = this.callers; callers != null; callers = callers.next) {
        callers.head.reverseVisit(scc);
      }
    } else if (this.scc == scc) {
      // If we arrive at a binding that has the same binding scc
      // as the one we're building, then we know that it is recursive.
      scc.setRecursive();
      return;
    } else {
      // The only remaining possibility is that we've strayed outside
      // the binding scc we're building to a scc that *depends on*
      // the one we're building.  In other words, we've found a binding
      // scc dependency from this.scc to scc.
      DefnSCC.addDependency(this.scc, scc);
    }
  }

  private static int dfsNum = 0;

  private int visitNum = 0;

  public static void newDFS() { // Begin a new depth-first search
    dfsNum++;
  }

  protected int occurs;

  public int getOccurs() {
    return occurs;
  }

  /**
   * Visit this Defn as part of a depth first search, and build a list of Defn nodes that can be
   * used to compute strongly-connected components.
   */
  Defns visitDepends(Defns defns) {
    if (visitNum == dfsNum) { // Repeat visit to this Defn?
      occurs++;
    } else { // First time at this Defn
      // Mark this Defn as visited, and initialize fields
      visitNum = dfsNum;
      occurs = 1;
      scc = null;
      callers = null;
      callees = null;

      // Find immediate dependencies
      Defns deps = dependencies();

      // Visit all the immediate dependencies
      for (; deps != null; deps = deps.next) {
        defns = deps.head.visitDepends(defns);
        if (!Defns.isIn(deps.head, callees)) {
          callees = new Defns(deps.head, callees);
        }
      }

      // Add the information about this node's callers/callees
      this.calls(callees);
      // And add it to the list of all definitions.
      defns = new Defns(this, defns);
    }
    return defns;
  }

  /** Find the list of Defns that this Defn depends on. */
  public abstract Defns dependencies();

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return Defns.isIn(this, ds) ? ds : new Defns(this, ds);
  }

  String dotAttrs() {
    return "style=filled, fillcolor=white";
  }

  boolean dotInclude() {
    return true;
  }

  public Defns getCallers() {
    return callers;
  }

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Display a printable representation of this object on the specified PrintWriter. */
  public void dump(PrintWriter out) {
    dump(out, isEntrypoint);
  }

  public static boolean renameTemps = true;

  /** Display a printable representation of this definition on the specified PrintWriter. */
  abstract void dump(PrintWriter out, boolean isEntrypoint);

  void limitRecursion() throws Failure {
    /* do nothing */
  }

  /**
   * Set the initial type for this definition by instantiating the declared type, if present, or
   * using type variables to create a suitable skeleton. Also sets the types of bound variables.
   */
  abstract void setInitialType() throws Failure;

  /**
   * Type check the body of this definition, but reporting rather than throwing an exception error
   * if the given handler is not null.
   */
  abstract void checkBody(Handler handler) throws Failure;

  /**
   * Calculate a generalized type for this binding, adding universal quantifiers for any unbound
   * type variable in the inferred type. (There are no "fixed" type variables here because all mil
   * definitions are at the top level.)
   */
  abstract void generalizeType(Handler handler) throws Failure;

  abstract void findAmbigTVars(Handler handler, TVars gens);

  void extendAddrMap(HashAddrMap addrMap, int addr) {
    addrMap.addCodeLabel(addr, toString());
  }

  /** Generate code to invoke the main definition, if it is a block with no parameters. */
  void callMain(MachineBuilder builder) {
    /* Ignore if main is not a Block */
  }

  /** First pass code generation: produce code for top-level definitions. */
  abstract void generateMain(Handler handler, MachineBuilder builder);

  /** Second pass code generation: produce code for block and closure definitions. */
  abstract void generateFunctions(MachineBuilder builder);

  boolean localLoopSCC(DefnSCC scc) {
    return false;
  }

  protected static Temp[] mergeParams(Temp[][] tss, Temp[] params) {
    // Calculate total number of new parameters:
    int len = 0;
    for (int i = 0; i < tss.length; i++) {
      len += (tss[i] == null ? 1 : tss[i].length);
    }
    // Collapse new parameters into a single array:
    Temp[] nps = new Temp[len];
    int pos = 0;
    for (int i = 0; i < tss.length; i++) {
      if (tss[i] == null) { // no new params
        nps[pos++] = params[i]; // save formal parameter
      } else { // otherwise replace original formal
        int l = tss[i].length; // parameter with the new list
        for (int j = 0; j < l; j++) {
          nps[pos++] = tss[i][j];
        }
      }
    }
    return nps;
  }

  protected static Code addInitializers(Call[] calls, Temp[] params, Temp[][] tss, Code code) {
    for (int i = 0; i < calls.length; i++) {
      if (calls[i] != null) {
        Allocator alloc = calls[i].isAllocator();
        if (alloc != null) {
          Cfun cf = alloc.cfunNoArgs();
          if (cf != null) {
            code = new Assert(params[i], cf, code);
          } else {
            TopLevel tl = alloc.getTopLevel();
            // We are only matching on the outermost constructor in each allocator, so we should
            // only use the
            // top-level version if there are no arguments.  This still works nicely for examples
            // like True,
            // False, Nil, Nothing, etc...
            Tail t =
                (tl != null && tss[i].length == 0)
                    ? new Return(new TopDef(tl, 0))
                    : alloc.callDup(tss[i]);
            code = new Bind(params[i], t, code);
          }
        } else {
          // Assumes that calls[i] is a return of a known value:
          code = new Bind(params[i], calls[i], code);
        }
      }
    }
    return code;
  }

  /**
   * Reset the doesntReturn flag, if there is one, for this definition ahead of a returnAnalysis().
   * For this analysis, we use true as the initial value, reducing it to false if we find a path
   * that allows a block's code to return.
   */
  void resetDoesntReturn() {
    /* Nothing to do in this case */
  }

  /**
   * Apply return analysis to this definition, returning true if this results in a change from the
   * previously computed value.
   */
  boolean returnAnalysis() {
    return false;
  }

  boolean detectLoops(Blocks visited) {
    return false;
  }

  /** Perform pre-inlining cleanup on each Block in this SCC. */
  void cleanup() {
    /* Nothing to do here */
  }

  /** Apply inlining. */
  public abstract void inlining();

  void liftAllocators() {
    /* Nothing to do */
  }

  /** Reset the bitmap and count for the used arguments of this definition, where relevant. */
  void clearUsedArgsInfo() {
    /* The default is to do nothing */
  }

  /**
   * Count the number of unused arguments for this definition using the current unusedArgs
   * information for any other items that it references.
   */
  abstract int countUnusedArgs();

  protected static Temps useAllArgs(Atom[] args, Temps vs) {
    for (int i = 0; i < args.length; i++) {
      vs = args[i].add(vs);
    }
    return vs;
  }

  /** Remove unused arguments from block calls and closure definitions. */
  void removeUnusedArgs() {
    /* Nothing to do here */
  }

  /** Perform flow analysis on this definition. */
  public abstract void flow();

  Call[] collectCalls(Atom[] args, Facts facts) {
    int len = args.length;
    Call[] calls = null;
    for (int i = 0; i < len; i++) {
      Tail t = args[i].lookupFact(facts);
      if (t != null) {
        Allocator alloc = t.isAllocator(); // We're only going to keep info about Allocators
        if (alloc != null && (isInvariant(i) || !alloc.isRecursive())) {
          if (calls == null) {
            calls = new Call[len];
          }
          calls[i] = alloc;
          continue;
        }
      }
      Atom a = args[i].isKnown(); // Otherwise look for a known argument ...
      if (a != null) {
        if (calls == null) {
          calls = new Call[len];
        }
        calls[i] = new Return(a);
      }
    }
    return calls;
  }

  boolean isRecursive() {
    return scc != null && scc.isRecursive();
  }

  void initSources() {
    /* nothing to do */
  }

  void dumpSources(PrintWriter out) {
    /* nothing to do */
  }

  static void dumpSources(PrintWriter out, String lab, String open, String close, Src[] sources) {
    if (sources != null) {
      out.print(lab);
      out.print(open);
      for (int i = 0; i < sources.length; i++) {
        if (i > 0) {
          out.print(", ");
        }
        out.print(sources[i].toString());
      }
      out.println(close);
    }
  }

  /**
   * Traverse the abstract syntax tree to calculate initial values for the sources of the parameters
   * of each Block and Closure definition.
   */
  abstract void calcSources();

  void updateSources(int i, Defn d, int j) {
    debug.Internal.error("updateSources should not be called for " + this);
  }

  void updateSources(int i, Defn d, int j, Join js) {
    debug.Internal.error("updateSources should not be called for " + this);
  }

  void setSource(int i, Src src) {
    debug.Internal.error("setSource should not be called for " + this);
  }

  boolean propagateSources() {
    return false;
  }

  Src propagate(int i, Src src) {
    debug.Internal.error("propagate should not be called for " + this);
    return src;
  }

  /**
   * Use results of invariant analysis to determine whether the specified parameter of this
   * definition is invariant in its defining SCC.
   */
  boolean isInvariant(int i) {
    return false;
  }

  /**
   * Compute a summary for this definition (if it is a block, top-level, or closure) and then look
   * for a previously encountered item with the same code in the given table. Return true if a
   * duplicate was found.
   */
  abstract boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels, ClosureDefns[] closures);

  abstract void eliminateDuplicates();

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  /** Apply constructor function simplifications to this program. */
  abstract void cfunSimplify();

  abstract void printlnSig(PrintWriter out);

  /** Test to determine if this is an appropriate definition to match the given type. */
  Block isBlockOfType(BlockType inst) {
    return null;
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  ClosureDefn isClosureDefnOfType(AllocType inst) {
    return null;
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  TopLevel isTopLevelOfType(Scheme inst) {
    return null;
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  External isExternalOfType(Scheme inst) {
    return null;
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  MemArea isMemAreaOfType(Scheme inst) {
    return null;
  }

  protected static String mkid(String id, int num) {
    return (num == 0) ? id : (id + num);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  abstract Defn specializeEntry(MILSpec spec) throws Failure;

  /** Update all declared types with canonical versions. */
  abstract void canonDeclared(MILSpec spec);

  abstract void bitdataRewrite(BitdataMap m);

  abstract void mergeRewrite(MergeMap mmap);

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    /* do nothing */
  }

  /**
   * Rewrite this definition, replacing TopLevels that introduce curried function values with
   * corresponding uncurried blocks. No changes are made to other forms of definition.
   */
  Defn makeEntryBlock() {
    return this;
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  abstract void repTransform(Handler handler, RepTypeSet set);

  Tail makeTail() throws Failure {
    throw new Failure("Unable to use \"" + this + "\" as a main function");
  }

  /** Add this exported definition to the specified MIL environment. */
  abstract void addExport(MILEnv exports);

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  public abstract Temp[] addArgs() throws Failure;

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    /* Nothing to do */
  }

  /** Reset the static value field for this definition. */
  void resetStaticValues() {
    /* Nothing to do */
  }

  public int getNumberCalls() {
    return 0;
  }

  /**
   * Reset the counter for the number of non-tail calls to this definition. This is only useful for
   * Blocks: we have to generate an LLVM function for every reachable closure definition anyway, but
   * we only need to do this for Blocks that are either listed as entrypoints or that are accessed
   * via a non-tail call somewhere in the reachable program.
   */
  void resetCallCounts() {
    /* do nothing */
  }

  /** Mark this definition as having been called. */
  void called() {
    /* do nothing */
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  abstract void countCalls();

  /**
   * Identify the set of blocks that should be included in the function that is generated for this
   * definition. A block call in the tail for a TopLevel is considered a regular call (it will
   * likely be called from the initialization code), but a block call in the tail for a ClosureDefn
   * is considered a genuine tail call. For a Block, we only search for the components of the
   * corresponding function if the block is the target of a call.
   */
  Blocks identifyBlocks() {
    return null;
  }

  CFG makeCFG() {
    return null;
  }

  /**
   * Find the main block for this program. If no main symbol has been specified, then we generate a
   * null main block. If the main symbol has been defined but does not correspond to a nullary
   * block, then we report an error.
   */
  Block getMainBlock() throws Failure {
    return mainBlockFailure();
  }

  Block mainBlockFailure() throws Failure {
    throw new Failure(
        "Cannot use \"" + this + "\" as a main function (requires zero parameter block)");
  }

  /**
   * Generate code (in reverse) to initialize each TopLevel (unless all of the components are
   * statically known).
   */
  llvm.Code addRevInitCode(LLVMMap lm, InitVarMap ivm, llvm.Code edoc) {
    return edoc;
  }
}
