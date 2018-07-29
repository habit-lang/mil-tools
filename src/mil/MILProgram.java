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
import java.io.IOException;
import java.io.PrintWriter;

/** Provides a representation for MIL programs. */
public class MILProgram {

  /** Stores a list of the entry points for this program. */
  private Defns entries = null;

  /** Add an entry point for this program, if it is not already included. */
  public void addEntry(Defn defn) {
    // !System.out.println("Adding entry for " + defn.getId());
    if (!Defns.isIn(defn, entries)) {
      entries = new Defns(defn, entries);
    }
  }

  /** Report whether this program is empty or not (i.e., whether it has any entrypoints). */
  public boolean isEmpty() {
    return (entries == null);
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
    // ! if (defns==null) {
    // !   System.out.println("No definitions remain");
    // ! }
    return defns;
  }

  /**
   * Perform tree shaking on this program, computing strongly-connected components for the reachable
   * portion of the input graph.
   */
  public void shake() {
    sccs = Defns.searchReverse(reachable()); // Compute the strongly-connected components
  }

  public void toDot() {
    PrintWriter out = new PrintWriter(System.out);
    toDot(out);
    out.flush();
  }

  public void toDot(String name) {
    try {
      PrintWriter out = new PrintWriter(name);
      toDot(out);
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to create dot output in \"" + name + "\" failed");
    }
  }

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

  /** Display a printable representation of this MIL construct on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Write text for this MIL construct in a file with the specified name. */
  public void dump(String name) {
    try {
      PrintWriter out = new PrintWriter(name);
      dump(out);
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to create mil output in \"" + name + "\" failed");
    }
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.dump(out);
    }
  }

  /** Add a special block for aborting the program. TODO: where do we set a type for this block? */
  public static final Block abort = new Block(BuiltinPosition.position, Temp.noTemps, Code.halt);

  public void typeChecking(Handler handler) throws Failure {
    shake();
    // ! dump();
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
      // !System.out.println("==================================================");
      // !System.out.println("Step " + i);
      // !System.out.println("==================================================");
      // !dump();
      // !System.out.println("==================================================");
      count = 0;
      inlining();
      debug.Log.println("Inlining pass finished, running shake.");
      shake();
      // !System.out.println("Before lift allocators: ==========================");
      // !dump();
      liftAllocators(); // TODO: Is this the right position for liftAllocators?
      // !System.out.println("Before eliminateUnusedArgs: ======================");
      // !dump();
      eliminateUnusedArgs();
      shake();
      // !System.out.println("Before flow: =====================================");
      // !dump();
      flow();
      if (count == 0) { // If no changes so far this iteration, try collect.
        shake();
        // collapse();     // TODO: Should we do this every time anyway?
        collect();
      }
      debug.Log.println("Flow pass finished, running shake.");
      shake();
      debug.Log.println("Steps performed = " + count);
      totalCount += count;
    }

    // Final cleanup: look for opportunities to collapse duplicated definitions:
    int postcount = 0;
    count = 1;
    for (int i = 0; i < MAX_OPTIMIZE_PASSES && count > 0 && count != postcount; i++) {
      debug.Log.println("-------------------------");
      count = 0;
      collapse(); // TODO: move inside loop?
      postcount = count;
      collect();
      shake();
      inlining();
      shake();
      flow();
      shake(); // restore SCCs
      debug.Log.println("Cleanup steps performed = " + count);
      totalCount += count;
    }
    // while (count!=0 && count!=postcount);
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
    boolean found = false;

    // Visit each block to compute summaries and populate the table:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        found |= ds.head.summarizeDefns(blocks, topLevels);
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

  /**
   * Scan a program and simplify any blocks that are only ever called with specific values for
   * particular parameters. In particular, this includes blocks that are only called once with one
   * or more known parameters.
   */
  void collect() {
    // Collect information about all block calls in the program:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      // Initialize argVals for each block in this SCC
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.clearArgVals();
      }

      // Update argVals for each code fragment in this SCC:
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.collect();
      }
    }

    // Now look for places where a unique value is used for all calls to a given block:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.checkCollection();
      }
    }
  }

  public void collect(TypeSet set) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      dsccs.head.collect(set);
    }
  }

  public void cfunRewrite() {
    TypeSet set = new NewtypeTypeSet();
    collect(set);
    // ! System.out.println("TypeSet after Newtype Removal: ==========");
    // ! set.dump();
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

    // Step 1: Generate specialized versions of each entry point:
    for (Defns es = entries; es != null; es = es.next) {
      try {
        spec.addEntry(es.head);
      } catch (Failure f) {
        handler.report(f);
      }
    }
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

  public RepTypeSet repTransform(Handler handler) throws Failure {
    RepTypeSet set = new RepTypeSet();
    collect(set);
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      // Rewrite the left hand side of any top level definitions in this SCC:
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.topLevelrepTransform(set);
      }

      // Rewrite the remaining portions of any definitions in this SCC:
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.repTransform(handler, set);
      }
    }
    return set;
  }

  private static MILEnv primEnv = null;

  /** Load MIL primitive definitions from the specified filed. */
  public static void loadPrims(Handler handler, String name) {
    try {
      // Load the named file (and anything it depends on):
      MILLoader loader = new MILLoader();
      loader.require(name);
      MILProgram mil = new MILProgram();
      primEnv = loader.load(handler, mil);
      handler.abortOnFailures();

      // Run basic type checking on the resulting program:
      mil.typeChecking(handler);
      handler.abortOnFailures();
      // !
      // !   mil.dump(); // Output result
    } catch (Failure f) {
      handler.report(f);
      debug.Internal.error("Aborting due to errors while loading primitives from " + name);
    }
  }

  /** Return a pointer to the set of MIL primitive definitions. */
  public static MILEnv primEnv() {
    return primEnv;
  }

  public void addArgs() throws Failure {
    for (Defns ds = reachable(); ds != null; ds = ds.next) {
      ds.head.addArgs();
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

    // Scan the full program to register any additional non-tail calls:
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.countCalls();
      }
    }
    // ! // Display results:
    // ! System.out.print("CALLED :");
    // ! for (DefnSCCs dsccs = sccs; dsccs!=null; dsccs=dsccs.next) {
    // !   for (Defns ds=dsccs.head.getBindings(); ds!=null; ds=ds.next) {
    // !     int calls = ds.head.getNumberCalls();
    // !     if (calls>0) {
    // !       System.out.print(" " + ds.head + "[" + calls + "]");
    // !     }
    // !   }
    // ! }
    // ! System.out.println();
  }

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(TypeMap tm, llvm.Program prog) {
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      boolean anyTopLevels = false;
      // First reset all static values in this SCC to null
      // TODO: Is this necessary? Why would they contain non-null values?
      // TODO: Is this sufficient to produce the correct results for mutually recursive definitions?
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        anyTopLevels |= ds.head.resetStaticValues();
      }
      if (anyTopLevels) { // The second pass is only necessary if there are TopLevels in this SCC
        for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
          ds.head.calcStaticValues(tm, prog);
        }
      }
    }
  }

  /**
   * Identify all of the blocks in this program that should be "callable" as a function in the
   * generated code: this includes all blocks that are listed as entry points, as well as all blocks
   * that are used in a non-tail call somewhere in the program.
   */
  public llvm.Program toLLVM() {
    analyzeCalls();
    CFGs cfgs = null;
    llvm.Program prog = new llvm.Program();
    TypeMap tm = new TypeMap(prog);
    calcStaticValues(tm, prog);
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        CFG cfg = ds.head.makeCFG();
        if (cfg != null) {
          // !        cfg.display();
          DefnVarMap vm = new DefnVarMap();
          /* TempSubst s = */ cfg.paramElim(tm, vm);

          prog.add(cfg.toLLVMFuncDefn(tm, vm));

          // System.out.println(TempSubst.toString(s));
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
    prog.add(generateInitFunction(tm));
    //  CFGs.toDot("cfgs.dot", cfgs);       // TODO: should not generate this unless requested
    return prog;
  }

  /**
   * Generate LLVM code to initialize all TopLevels in this program that do not have static values.
   */
  llvm.FuncDefn generateInitFunction(TypeMap tm) {
    llvm.Code code = null;
    InitVarMap ivm = new InitVarMap();
    for (DefnSCCs dsccs = sccs; dsccs != null; dsccs = dsccs.next) {
      for (Defns ds = dsccs.head.getBindings(); ds != null; ds = ds.next) {
        code = ds.head.addRevInitCode(tm, ivm, code);
      }
    }
    return new llvm.FuncDefn(
        llvm.Type.vd,
        llvm.FuncDefn.mainFunctionName,
        new llvm.Local[0],
        new String[] {"entry"},
        new llvm.Code[] {llvm.Code.reverseOnto(code, new llvm.RetVoid())});
  }
}
