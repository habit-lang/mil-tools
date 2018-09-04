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
import compiler.Failure;
import compiler.Handler;
import compiler.Position;
import core.*;

class TopLevelExp extends DefnExp {

  private String[] ids;

  private String[] args;

  private CodeExp cexp;

  /** Default constructor. */
  TopLevelExp(Position pos, String[] ids, String[] args, CodeExp cexp) {
    super(pos);
    this.ids = ids;
    this.args = args;
    this.cexp = cexp;
  }

  private TopLevel t;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure {
    TopLhs[] lhs = new TopLhs[ids.length];
    t = new TopLevel(pos, lhs, null);
    for (int i = 0; i < lhs.length; i++) {
      lhs[i] = new TopLhs(ids[i]);
      if (milenv.addTop(ids[i], new TopDef(t, i)) != null) {
        MILEnv.multipleDefns(pos, "top level symbol", ids[i]);
      }
    }
  }

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  void inScopeOf(Handler handler, MILEnv milenv) throws Failure {
    t.inScopeOf(handler, milenv, args, cexp);
  }

  /**
   * Add the MIL definition associated with this DefnExp, if any, as an entrypoint to the specified
   * program.
   */
  void addAsEntryTo(MILProgram mil) {
    mil.addEntry(t);
  }
}
