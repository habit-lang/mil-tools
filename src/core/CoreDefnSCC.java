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
package core;

import compiler.*;
import mil.*;

public class CoreDefnSCC {

  /** Records the list of X\s in this binding group. */
  private CoreDefns bindings = null;

  /** Return the list of X\s in this scc. */
  public CoreDefns getBindings() {
    return bindings;
  }

  /** Add an X to this scc. */
  public void add(CoreDefn binding) {
    bindings = new CoreDefns(binding, bindings);
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
  private CoreDefnSCCs dependsOn = null;

  public CoreDefnSCCs getDependsOn() {
    return dependsOn;
  }

  /**
   * Record a dependency between two binding sccs, avoiding self references and duplicate entries.
   */
  static void addDependency(CoreDefnSCC from, CoreDefnSCC to) {
    if (from != to && !find(to, from.dependsOn)) {
      from.dependsOn = new CoreDefnSCCs(to, from.dependsOn);
    }
  }

  /** Search for a specific binding scc within a given list. */
  static boolean find(CoreDefnSCC scc, CoreDefnSCCs sccs) {
    for (; sccs != null; sccs = sccs.next) {
      if (sccs.head == scc) {
        return true;
      }
    }
    return false;
  }

  void calcSizes(Handler handler) {
    // Set sizes to distinguish types with/without bit level representation
    for (CoreDefns ds = getBindings(); ds != null; ds = ds.next) {
      ds.head.initSizes(handler);
    }

    // Validate the type components of the definitions and used them to calculate
    // types for the constructors and to set up linear equations for layout calculations
    LinearEqns eqns = null;
    for (CoreDefns ds = getBindings(); ds != null; ds = ds.next) {
      eqns = ds.head.initEqns(handler, eqns);
    }

    // Calculate size information by collecting and solving corresponding linear equations
    if (eqns != null) { // no equations => no size information to compute and validate
      try {
        LinearEqns.solve(eqns);
        for (CoreDefns ds = getBindings(); ds != null; ds = ds.next) {
          ds.head.checkSizes();
        }
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }
}
