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
import mil.*;

class Export extends TopDefn {

  protected String[] ids;

  /** Default constructor. */
  Export(Position pos, String[] ids) {
    super(pos);
    this.ids = ids;
  }

  /**
   * Run scope analysis on a top level lc definition to ensure that all the items identified as
   * exports or entrypoints are in scope, either as a binding in this program, or as a Top that is
   * visible in the current environment.
   */
  void scopeTopDefn(Handler handler, MILEnv milenv, Env env) throws Failure {
    vars = new DefVar[ids.length];
    tops = new Top[ids.length];
    for (int i = 0; i < ids.length; i++) {
      if ((vars[i] = Env.find(ids[i], env)) == null
          && // look for a top level binding
          (tops[i] = milenv.findTop(ids[i])) == null) { // look for a Top in the current milenv
        Cfun cf = milenv.findCfun(ids[i]); // and then look for a reference to a constructor
        if (cf != null) {
          tops[i] = cf.getTop();
        } else {
          handler.report(new NotInScopeFailure(pos, ids[i]));
        }
      }
    }
  }

  /** List of variables corresponding to the identifiers in this declaration. */
  protected DefVar[] vars;

  /** List of Top values corresponding to the identifiers in this declaration. */
  protected Top[] tops;

  /** Check types of expressions appearing in top-level definitions. */
  void inferTypes(Handler handler) throws Failure {
    /* Do nothing */
  }

  void liftTopDefn(LiftEnv lenv) {
    for (int i = 0; i < vars.length; i++) {
      if (vars[i] != null) {
        Lifting l = vars[i].findLifting(lenv);
        if (l != null) {
          tops[i] = new TopDef(l.getTopLevel(), 0);
        } else {
          debug.Internal.error("no lifting for " + vars[i]);
        }
      }
    }
  }

  void addExports(MILProgram mil, MILEnv milenv) {
    for (int i = 0; i < tops.length; i++) {
      milenv.addTop(tops[i]);
    }
  }
}
