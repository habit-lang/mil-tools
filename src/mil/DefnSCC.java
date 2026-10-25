/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

/** Represents a strongly connected component of Defn objects. */
public class DefnSCC {

  /** Records the list of Defns in this binding group. */
  private Defns bindings = null;

  /** Return the list of Defns in this scc. */
  public Defns getBindings() {
    return bindings;
  }

  /** Add an Defn to this scc. */
  public void add(Defn binding) {
    bindings = new Defns(binding, bindings);
  }

  /**
   * Indicates if the bindings in this scc are recursive. This flag is initialized to false but will
   * be set to true if recursive bindings are discovered during dependency analysis. If there are
   * multiple bindings in this scc, then they must be mutually recursive (otherwise they would have
   * been placed in different binding sccs) and this flag will be set to true.
   */
  private boolean recursive = false;

  /** This method is called when a recursive binding is discovered during dependency analysis. */
  void setRecursive() {
    recursive = true;
  }

  /** Return a boolean true if this is a recursive binding scc. */
  public boolean isRecursive() {
    return recursive;
  }

  /**
   * A list of the binding sccs on which this scc depends. (This particular scc will not be
   * included, so the graph of DefnSCCs will not have any cycles in it.)
   */
  private DefnSCCs dependsOn = null;

  public DefnSCCs getDependsOn() {
    return dependsOn;
  }

  /**
   * Record a dependency between two binding sccs, avoiding self references and duplicate entries.
   */
  static void addDependency(DefnSCC from, DefnSCC to) {
    if (from != to && !find(to, from.dependsOn)) {
      from.dependsOn = new DefnSCCs(to, from.dependsOn);
    }
  }

  /** Search for a specific binding scc within a given list. */
  static boolean find(DefnSCC scc, DefnSCCs sccs) {
    for (; sccs != null; sccs = sccs.next) {
      if (sccs.head == scc) {
        return true;
      }
    }
    return false;
  }

  /** Display a printable representation of this object on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Display a printable representation of this object on the specified PrintWriter. */
  public void dump(PrintWriter out) {
    // out.print("----------------------------------------- ");
    out.println("-----------------------------------------");
    out.print("-- ");
    out.println(isRecursive() ? "recursive" : "not recursive");
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.dump(out);
    }
    out.println();
  }

  private boolean checked = true;

  void inferTypes(Handler handler) {
    // Step 0: do not type check this SCC if it depends on an earlier
    // SCC that failed to type check.
    for (DefnSCCs deps = dependsOn; deps != null; deps = deps.next) {
      if (!deps.head.checked) {
        this.checked = false;
        return;
      }
    }

    try {
      // Check that there are no recursive top level definitions in
      // this binding group.
      if (recursive) {
        for (Defns ds = bindings; ds != null; ds = ds.next) {
          ds.head.limitRecursion();
        }
      }
      // Infer types for all of the bindings:
      Defns.inferTypes(handler, bindings);
    } catch (Failure f) {
      this.checked = false;
      handler.report(f);
    }
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.generateMain(handler, builder);
    }
  }

  /** Second pass code generation: produce code for block and closure definitions. */
  void generateFunctions(MachineBuilder builder) {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.generateFunctions(builder);
    }
  }

  /** Clear derived blocks and closure definitions. */
  void clearDerived() {
    for (Defns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.clearDerived();
    }
  }

  /**
   * Heuristic to determine if this block is a good candidate for the casesOn(). TODO: investigate
   * better functions for finding candidates!
   */
  boolean contCand() {
    if (isRecursive()) {
      return false;
    }
    for (DefnSCCs ds = getDependsOn(); ds != null; ds = ds.next) {
      if (!ds.head.contCand()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test to determine if this SCC contains only Block definitions, and if the only (non-tail) calls
   * in the code sequences for those blocks are to blocks in other SCCs.
   */
  boolean localLoopSCC() {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      if (!ds.head.localLoopSCC(this)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compute values for doesntReturn for all of the definitions (specifically Blocks) in this SCC.
   */
  void returnAnalysis() {
    // Initialize return analysis information for each definition
    // in this scc:
    for (Defns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.setDoesntReturn();
    }

    // Compute return analysis results for each item in this scc:
    boolean changed = true;
    do {
      changed = false;
      for (Defns ds = getBindings(); ds != null; ds = ds.next) {
        changed |= ds.head.returnAnalysis();
      }
    } while (changed);
    //  // TODO: Why is this done on a per Block basis instead of per SCC?
  }

  /** Perform pre-inlining cleanup on each Block in this SCC. */
  void cleanup() {
    for (Defns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.cleanup();
    }
  }

  void detectLoops() {
    for (Defns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.detectLoops(null);
    }
  }

  /** Apply inlining. */
  void inlining() {
    for (Defns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.inlining();
    }
  }

  void invariantAnalysis() {
    initSources();
    calcSources();
    propagateSources();
  }

  void initSources() {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.initSources();
    }
  }

  /**
   * Traverse the abstract syntax tree to calculate initial values for the the sources of the
   * parameters of each Block and Closure definition.
   */
  void calcSources() {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.calcSources();
    }
  }

  void propagateSources() {
    boolean changed;
    do {
      changed = false;
      for (Defns ds = bindings; ds != null; ds = ds.next) {
        changed |= ds.head.propagateSources();
      }
    } while (changed);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    for (Defns ds = bindings; ds != null; ds = ds.next) {
      ds.head.collect(set);
    }
  }
}
