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
import lc.Env;
import lc.LiftEnv;
import mil.*;

public abstract class CoreDefn {

  protected Position pos;

  /** Default constructor. */
  public CoreDefn(Position pos) {
    this.pos = pos;
  }

  public boolean answersTo(String id) {
    return false;
  }

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return null;
  }

  /** Records the successors/callees of this node. */
  private CoreDefns callees = null;

  /** Records the predecessors/callers of this node. */
  private CoreDefns callers = null;

  /** Update callees/callers information with dependencies. */
  public void calls(CoreDefns xs) {
    for (callees = xs; xs != null; xs = xs.next) {
      xs.head.callers = new CoreDefns(this, xs.head.callers);
    }
  }

  /**
   * Flag to indicate that this node has been visited during the depth-first search of the forward
   * dependency graph.
   */
  private boolean visited = false;

  /** Visit this X during a depth-first search of the forward dependency graph. */
  CoreDefns forwardVisit(CoreDefns result) {
    if (!this.visited) {
      this.visited = true;
      return new CoreDefns(this, CoreDefns.searchForward(this.callees, result));
    }
    return result;
  }

  /**
   * Records the binding scc in which this binding has been placed. This field is initialized to
   * null but is set to the appropriate binding scc during dependency analysis.
   */
  private CoreDefnSCC scc = null;

  /** Return the binding scc that contains this binding. */
  public CoreDefnSCC getScc() {
    return scc;
  }

  /**
   * Visit this binding during a depth-first search of the reverse dependency graph. The scc
   * parameter is the binding scc in which all unvisited bindings that we find should be placed.
   */
  void reverseVisit(CoreDefnSCC scc) {
    if (this.scc == null) {
      // If we arrive at a binding that hasn't been allocated to any SCC,
      // then we should put it in this SCC.
      this.scc = scc;
      scc.add(this);
      for (CoreDefns callers = this.callers; callers != null; callers = callers.next) {
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
      CoreDefnSCC.addDependency(this.scc, scc);
    }
  }

  /** Add this value to the front of a list if it is not already present. */
  public CoreDefns uniqueCons(CoreDefns xs) {
    return CoreDefns.isIn(this, xs) ? xs : new CoreDefns(this, xs);
  }

  public abstract void introduceTycons(Handler handler, TyconEnv env);

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public abstract void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env);

  public void setRecursive() {
    /* by default, no action is required */
  }

  public abstract void kindInfer(Handler handler);

  public void fixKinds() {
    /* do nothing */
  }

  /** Initialize size information for this definition, if appropriate. */
  void initSizes(Handler handler) {
    /* Nothing to do here */
  }

  /**
   * Initialize linear equation to calculate size information for this definition, if appropriate.
   */
  public LinearEqns initEqns(Handler handler, LinearEqns eqns) {
    return eqns;
  }

  void checkSizes() throws Failure {
    /* do nothing */
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public abstract void calcCfuns(Handler handler);

  public abstract void addToMILEnv(Handler handler, MILEnv milenv);

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    /* By default, do nothing */
  }

  public void inferTypes(Handler handler) throws Failure {
    /* By default, do nothing */
  }

  public void lift(LiftEnv lenv) {
    /* By default, do nothing */
  }
}
