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
   * exports or entrypoints are in scope.
   */
  void scopeTopDefn(Handler handler, MILEnv milenv, Env env) throws Failure {
    vars = new DefVar[ids.length];
    for (int i = 0; i < ids.length; i++) {
      if ((vars[i] = Env.find(ids[i], env)) == null) {
        handler.report(new NotInScopeFailure(pos, ids[i]));
      }
    }
  }

  /** List of variables corresponding to the identifiers in this declaration. */
  protected DefVar[] vars;

  void liftTopDefn(LiftEnv lenv) {
    topLevels = new TopLevel[vars.length];
    for (int i = 0; i < vars.length; i++) {
      Lifting l = vars[i].findLifting(lenv);
      if (l == null) {
        debug.Internal.error("no lifting for " + vars[i]);
      }
      topLevels[i] = l.getTopLevel();
    }
  }

  /** List of TopLevel values corresponding to the identifiers in this declaration. */
  protected TopLevel[] topLevels;

  void addExports(MILProgram mil, MILEnv milenv) {
    for (int i = 0; i < topLevels.length; i++) {
      milenv.addTop(new TopDef(topLevels[i], 0));
    }
  }
}
