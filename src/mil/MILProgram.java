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
import core.*;
import java.io.PrintWriter;

/** Provides a representation for MIL programs. */
public class MILProgram {

  /** The main definition for this program, if specified. */
  private Defn main = null;

  public void setMain(Defn main) {
    this.main = main;
  }

  /** Stores a list of the entry points for this program. */
  private Defns entries = null;

  /** Add an entry point for this program, if it is not already included. */
  public void addEntry(Defn defn) {
    if (!Defns.isIn(defn, entries)) {
      entries = new Defns(defn, entries);
      defn.setIsEntrypoint(true);
    }
  }

  /** Report whether this program is empty or not (i.e., whether it has any entrypoints). */
  public boolean isEmpty() {
    return (main == null) && (entries == null);
  }

  /** Record the list of strongly connected components in this program. */
  private DefnSCCs sccs;

  /** Compute the list of definitions for the reachable portion of the input graph. */
  public Defns reachable() {
    Defn.newDFS(); // Begin a new depth-first search
    Defns defns = null; // Compute a list of reachable Defns
    for (Defns ds = entries; ds != null; ds = ds.next) {
      defns = ds.head.visitDepends(defns);
    }
    if (main != null) {
      defns = main.visitDepends(defns);
    }
    return defns;
  }

  /**
   * Perform tree shaking on this program, computing strongly-connected components for the reachable
   * portion of the input graph.
   */
  public void shake() {
    sccs = Defns.searchReverse(reachable()); // Compute the strongly-connected components
  }

  /** Generate a dot description of this program's call graph on the specified PrintWriter. */
  public void toDot(PrintWriter out) {
    out.println("digraph MIL {");
    for (Defns ds = reachable(); ds != null; ds = ds.next) {
      if (ds.head.dotInclude()) {
        String dlab = ds.head.toString();
        out.println("  \"" + dlab + "\"[" + ds.head.dotAttrs() + "];");
        for (Defns cs = ds.head.getCallers(); cs != null; cs = cs.next) {
          String clab = cs.head.toString();
          out.println("  \"" + clab + "\" -> \"" + dlab + "\";");
        }
      }
    }
    out.println("}");
  }

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Display a printable representation of this object on the specified PrintWriter. */
  public void dump(PrintWriter out) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.dump(out);
    }
    out.println("-----------------------------------------");
    out.print("-- Entrypoints:");
    for (Defns es = entries; es != null; es = es.next) {
      out.print(" " + es.head);
    }
    out.println();
  }

  /** Add a special block for aborting the program. */
  public static final Block abort =
      new Block(BuiltinPosition.pos, Temp.noTemps, new Done(Prim.halt.withArgs()));

  public void typeChecking(Handler handler) throws Failure {
    shake();
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.inferTypes(handler);
    }
    handler.abortOnFailures();
  }

  public MachineBuilder generateMachineBuilder(Handler handler) {
    MachineBuilder builder = new MachineBuilder();

    // In the first pass over the MIL program, we generate code for the main function, starting at
    // address 0,
    // which will execute the tails associated with top level definitions and store the results as
    // globals.
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.generateMain(handler, builder);
    }
    if (main != null) {
      main.callMain(builder);
    }
    builder.stop();

    // In the second pass over the MIL program, we generate code for the block
    // and closure definitions that are required for the program.
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.generateFunctions(builder);
    }
    return builder;
  }

  public static int count = 0;

  public static void report(String msg) {
    debug.Log.println(msg);
    count++;
  }

  /**
   * Limit the maximum number of optimization passes that will be performed on any MIL program. The
   * choice is somewhat arbitrary; a higher value might allow more aggressive optimization in some
   * cases, but might also result in larger output programs as a result of over-specialization,
   * unrolling, or inlining.
   */
  public static final int MAX_OPTIMIZE_PASSES = 42;

  /** Run the optimizer on this program. */
  public void optimize() {
    int totalCount = 0;
    count = 1;
    for (int i = 0; i < MAX_OPTIMIZE_PASSES && count > 0; i++) {
      debug.Log.println("-------------------------");
      count = 0;
      inlining();
      debug.Log.println("Inlining pass finished, running shake.");
      shake();
      liftAllocators(); // TODO: Is this the right position for liftAllocators?
      eliminateUnusedArgs();
      shake();
      flow();
      debug.Log.println("Flow pass finished, running shake.");
      shake();
      debug.Log.println("Steps performed = " + count);
      totalCount += count;
    }

    // Final cleanup: look for opportunities to collapse duplicated definitions:
    count = 1;
    for (int i = 0; i < MAX_OPTIMIZE_PASSES && count > 0 && count != 0; i++) {
      debug.Log.println("-------------------------");
      count = 0;
      collapse(); // TODO: move inside loop?
      //    collect();
      shake();
      inlining();
      shake();
      flow();
      shake(); // restore SCCs
      debug.Log.println("Cleanup steps performed = " + count);
      totalCount += count;
    }
    debug.Log.println("TOTAL steps performed = " + totalCount);
  }

  /**
   * Run an inlining pass over this program, assuming a preceding call to shake() to compute call
   * graph information. Starts by performing a "return analysis" (to detect blocks that are
   * guaranteed not to return); uses the results to perform some initial cleanup; and then performs
   * simple loop detection to identify code that loops with no observable effect and would cause the
   * inliner to enter an infinite loop. With those preliminaries out of the way, we then invoke the
   * main inliner!
   */
  public void inlining() {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      DefnSCC scc = dsccs.head;
      scc.returnAnalysis(); // Identify blocks that are guaranteed not to return
      scc.cleanup(); // Use results of return analysis to clean up code
      scc
          .detectLoops(); // Rewrite block definitions that could send the inliner into an infinite
                          // loop
      scc.inlining(); // Perform inlining on the definitions inside this scc
    }
  }

  /**
   * Run a liftAllocators pass over this program, assuming a previous call to shake() to compute
   * SCCs.
   */
  public void liftAllocators() {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.liftAllocators();
      }
    }
  }

  /** Analyze and rewrite this program to remove unused Block and ClosureDefn arguments. */
  void eliminateUnusedArgs() {
    int totalUnused = 0;

    // Phase 1: Calculate unused argument information for every Block and ClosureDefn:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      // Initialize used argument information for each definition
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.clearUsedArgsInfo();
      }

      // Calculate the number of unused arguments for the definitions in this strongly-connected
      // component, iterating until either there are no unused arguments, or else until we reach
      // a fixed point (i.e., we get the same number of unused args on two successive passes).
      int lastUnused = 0;
      int unused = 0;
      do {
        lastUnused = unused;
        unused = 0;
        for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
          unused += ds.head.countUnusedArgs();
        }
      } while (unused > 0 && unused != lastUnused);
      totalUnused += unused;
    }

    // Phase 2: Rewrite the program if there are unused arguments:
    if (totalUnused > 0) {
      for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
        for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
          ds.head.removeUnusedArgs();
        }
      }
    }
  }

  /** Run a flow pass over this program. */
  public void flow() {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.flow();
      }
    }
  }

  public void collapse() {
    final int SIZE = 251;
    Blocks[] blocks = new Blocks[SIZE];
    TopLevels[] topLevels = new TopLevels[SIZE];
    ClosureDefns[] closures = new ClosureDefns[SIZE];
    boolean found = false;

    // Visit each definition to compute summaries and populate the tables:

    // Start with entrypoints so that they are available to use as replacements for non-entrypoints.
    for (Defns ds = entries; ds != null; ds = ds.next) {
      found |= ds.head.summarizeDefns(blocks, topLevels, closures);
    }

    // Visit definitions that are not entrypoints:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        if (!ds.head.isEntrypoint()) {
          found |= ds.head.summarizeDefns(blocks, topLevels, closures);
        }
      }
    }

    // Update the program to eliminate duplicate blocks:
    if (found) {
      for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
        for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
          ds.head.eliminateDuplicates();
        }
      }
    }
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  public void collect(TypeSet set) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.collect(set);
    }
  }

  public void cfunRewrite() {
    TypeSet set = new NewtypeTypeSet();
    collect(set);
    cfunSimplify();
  }

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.cfunSimplify();
      }
    }
  }

  /** Generate a new, monomorphically typed version of this program using type specialization. */
  public MILSpec specialize(Handler handler) throws Failure {
    MILSpec spec =
        new MILSpec(); // Used to record information about generated/requested specializations

    // Step 1: Generate specialized versions of each entry point, and a specialized main if
    // necessary:
    for (Defns es = entries; es != null; es = es.next) {
      spec.addEntry(handler, es.head);
    }
    spec.addMain(handler, main);
    handler.abortOnFailures();

    // Step 2: Generate specialized versions of all reachable definitions:
    spec.generate();

    return spec;
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.canonDeclared(spec);
      }
    }
  }

  public void bitdataRewrite() {
    TypeSet set = new TypeSet();
    collect(set);
    DataTypes cands = set.bitdataCandidates();
    if (cands != null) {
      BitdataMap m = new BitdataMap();
      if (m.addMappings(cands) > 0) { // If any mappings were found, then rewrite the program
        for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
          for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
            ds.head.bitdataRewrite(m);
          }
        }
        collect(m); // Update types and any constructors/datatypes/etc. that were not rewritten
      }
    }
  }

  /**
   * Replace any MIL entrypoints in this program that have (possibly curried) function types
   * involving at least one use of (->>) with a block that implements the same function as an
   * uncurried block.
   */
  void makeEntryBlocks() {
    for (Defns es = entries;
        es != null;
        es = es.next) { // Make entry blocks for entrypoints, where possible
      es.head = es.head.makeEntryBlock();
    }
    Defns prev = null; // Filter entrypoints that are no longer marked as such
    for (Defns es = entries; es != null; ) {
      if (es.head.isEntrypoint) {
        prev = es;
        es = es.next;
      } else if (prev != null) {
        prev.next = es = es.next;
      } else {
        entries = es = es.next;
      }
    }
    if (main != null) { // Make an entrypoint for main, if defined
      main = main.makeEntryBlock();
    }
  }

  public RepTypeSet repTransform(Handler handler) throws Failure {
    RepTypeSet set = new RepTypeSet();
    collect(set);
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      // Rewrite the left hand side of any top level definitions in this SCC:
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.topLevelRepTransform(handler, set);
      }

      // Rewrite the remaining portions of any definitions in this SCC:
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.repTransform(handler, set);
      }
    }
    handler.abortOnFailures();
    makeEntryBlocks();
    main = set.makeMain(main);
    return set;
  }

  public void addArgs() throws Failure {
    for (Defns ds = reachable(); ds != null; ds = ds.next) {
      ds.head.addArgs();
    }
  }

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      // First reset all static values in this SCC to null
      // TODO: Is this necessary? Why would they contain non-null values?
      // TODO: Is this sufficient to produce the correct results for mutually recursive definitions?
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.resetStaticValues();
      }
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.calcStaticValues(lm, prog);
      }
    }
  }

  void analyzeCalls() {
    // Reset call counts for all blocks in this program:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.resetCallCounts();
      }
    }

    // Mark all blocks that are listed as entry points as being "called"
    for (Defns es = entries; es != null; es = es.next) {
      es.head.called();
    }
    if (main != null) {
      main.countAllCalls();
    }

    // Scan the full program to register any additional non-tail calls:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.countCalls();
      }
    }
  }

  /** Generate an LLVM implementation of this MIL program. */
  public llvm.Program toLLVM() throws Failure {
    llvm.Type.setWord(Word.size());
    analyzeCalls();
    CFGs cfgs = null;
    llvm.Program prog = new llvm.Program();
    LLVMMap lm = new LLVMMap(prog);
    calcStaticValues(lm, prog);
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        CFG cfg = ds.head.makeCFG();
        if (cfg != null) {
          TempSubst s = cfg.paramElim();
          // System.out.println(TempSubst.toString(s));

          DefnVarMap dvm = new DefnVarMap();
          prog.add(cfg.toLLVMFuncDefn(lm, dvm, s));

          // TODO: revert to adding each new CFG to the front of the list
          // cfgs = new CFGs(cfg, cfgs);
          if (cfgs == null) {
            cfgs = new CFGs(cfg, null);
          } else {
            CFGs prev = cfgs;
            while (prev.next != null) {
              prev = prev.next;
            }
            prev.next = new CFGs(cfg, null);
          }
        }
      }
    }
    generateInitFunction(lm, prog);
    //  CFGs.toDot("cfgs.dot", cfgs);       // TODO: should not generate this unless requested
    return prog;
  }

  /**
   * Generate LLVM code to initialize all TopLevels in this program that do not have static values.
   */
  void generateInitFunction(LLVMMap lm, llvm.Program prog) throws Failure {
    llvm.Code code = null;
    InitVarMap ivm = new InitVarMap();
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        code = ds.head.addRevInitCode(lm, ivm, code);
      }
    }
    if (!llvm.FuncDefn.mainFunctionName.equals("")) {
      prog.add(
          new llvm.FuncDefn(
              llvm.Mods.NONE,
              initType(lm),
              llvm.FuncDefn.mainFunctionName,
              new llvm.Local[0],
              new String[] {"entry"},
              new llvm.Code[] {llvm.Code.reverseOnto(code, initCode(lm, ivm))}));
    } else if (code != null || main != null) {
      throw new Failure(
          "LLVM program requires initialization function (set using --llvm-main=NAME)");
    }
  }

  /**
   * Calculate the LLVM return type that will be produced by the code in the main Block of a
   * program, if one has been specified.
   */
  llvm.Type initType(LLVMMap lm) throws Failure {
    return (main == null) ? llvm.Type.vd : main.initType(lm);
  }

  /** Generate an LLVM code sequence from the main Block in a program, if one has been specified. */
  llvm.Code initCode(LLVMMap lm, InitVarMap ivm) throws Failure {
    return (main == null) ? new llvm.RetVoid() : main.initCode(lm, ivm);
  }
}
