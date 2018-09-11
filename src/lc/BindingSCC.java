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
package lc;

import compiler.*;
import core.*;
import debug.Screen;
import mil.*;

public class BindingSCC {

  /** Records the list of X\s in this binding group. */
  private Bindings bindings = null;

  /** Return the list of X\s in this scc. */
  public Bindings getBindings() {
    return bindings;
  }

  /** Add an X to this scc. */
  public void add(Binding binding) {
    bindings = new Bindings(binding, bindings);
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
   * included, so the graph of XSCCs will not have any cycles in it.)
   */
  private BindingSCCs dependsOn = null;

  public BindingSCCs getDependsOn() {
    return dependsOn;
  }

  /**
   * Record a dependency between two binding sccs, avoiding self references and duplicate entries.
   */
  static void addDependency(BindingSCC from, BindingSCC to) {
    if (from != to && !find(to, from.dependsOn)) {
      from.dependsOn = new BindingSCCs(to, from.dependsOn);
    }
  }

  /** Search for a specific binding scc within a given list. */
  static boolean find(BindingSCC scc, BindingSCCs sccs) {
    for (; sccs != null; sccs = sccs.next) {
      if (sccs.head == scc) {
        return true;
      }
    }
    return false;
  }

  void display(Screen s) {
    if (isRecursive()) {
      s.print("rec ");
    }
    int ind = s.getIndent();
    Bindings bs = getBindings();
    if (bs != null) {
      bs.head.display(s);
      while ((bs = bs.next) != null) {
        s.indent(ind);
        bs.head.display(s);
      }
    }
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, "BindingSCC" + (isRecursive() ? ", recursive" : ""));
    Bindings.indent(out, n + 1, getBindings());
  }

  /**
   * Check that the binding groups in a list of SCCs are valid: each one must either be
   * non-recursive, or else must contain only function bindings (and perhaps monadic thunks?).
   */
  public static void checkSafeRecursion(Handler handler, BindingSCCs sccs) {
    for (; sccs != null; sccs = sccs.next) {
      BindingSCC scc = sccs.head;
      if (scc.isRecursive()) {
        Bindings bindings = scc.getBindings();

        // Union the free variables for each binding
        DefVars fvs = null;
        for (Bindings bs = bindings; bs != null; bs = bs.next) {
          fvs = bs.head.checkSafeToRecurse(handler, fvs);
        }

        // Then remove any variables defined in this binding
        for (Bindings bs = bindings; bs != null; bs = bs.next) {
          fvs = bs.head.remove(fvs);
        }
        scc.fvs = fvs;
      }
    }
  }

  /** Records the set of variables that appear free within this scc. */
  private DefVars fvs;

  private boolean checked = true;

  TVarsInScope inferTypes(Handler handler, TVarsInScope tis) throws Failure {
    // Step 0: do not type check this SCC if it depends on an earlier SCC that failed to type check.
    for (BindingSCCs deps = dependsOn; deps != null; deps = deps.next) {
      if (!deps.head.checked) {
        this.checked = false;
        return tis;
      }
    }

    // Step 1: set the type of each item in this binding group to an appropriate initial type,
    // either
    // an instance of the declared type or a fresh type variable if there is no declared type.
    for (Bindings bs = bindings; bs != null; bs = bs.next) {
      bs.head.setInitialType();
    }

    tis = new TVISBSCC(tis, this);
    try {
      // Step 2: run type inference on each binding.
      for (Bindings bs = bindings; bs != null; bs = bs.next) {
        bs.head.checkType(handler, tis);
      }
      // Step 3: calculate most general types for each item in this binding group.
      TVars fixed = TVarsInScope.tvarsInScope(tis.getEnclosing());
      // [Note: we intentionally use tis.getEnclosing() rather than the original value of tis here
      // so that we can take advantage of any pruning of the TVarsInScope objects that has taken
      // place.
      for (Bindings bs = bindings; bs != null; bs = bs.next) {
        bs.head.generalizeType(fixed);
      }
    } catch (Failure f) {
      this.checked = false;
      if (handler != null) {
        handler.report(f);
        return tis.getEnclosing(); // ignore this SCC
      } else {
        throw f;
      }
    }
    return tis;
  }

  /**
   * Calculate the list of variables that should be added as extra arguments to each of the bindings
   * in this SCC during lambda lifting.
   */
  DefVar[] extraVars(LiftEnv lenv) {
    return DefVars.toArray(lenv.extraVars(fvs));
  }

  Binding getNonRecBinding() {
    Bindings bs = getBindings();
    if (isRecursive()) {
      debug.Internal.error("Unlifted local recursive definition");
    } else if (bs == null || bs.next != null) {
      debug.Internal.error("Malformed nonrecursive binding group");
    }
    return bs.head;
  }
}
