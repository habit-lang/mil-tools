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

class Export extends Annotation {

  /** Default constructor. */
  Export(Position pos, String[] ids) {
    super(pos, ids);
  }

  protected Defn[] defns;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure { // export ids
    /* nothing to do here! */
  }

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  void inScopeOf(Handler handler, MILEnv milenv) throws Failure {
    defns = new Defn[ids.length];
    for (int i = 0; i < ids.length; i++) {
      // For each identifier, look first for a top-level, then a closure definition, and then a
      // block.
      Top t = milenv.findTop(ids[i]);
      if (t != null) {
        defns[i] = t.getDefn();
      } else if ((defns[i] = milenv.findClosureDefn(ids[i])) == null
          && (defns[i] = milenv.findBlock(ids[i])) == null) {
        handler.report(MILEnv.notFound(pos, ids[i]));
      }
    }
  }

  /** Add items in export lists to the specified MIL environment. */
  void addExports(MILEnv exports, MILProgram program) {
    for (int i = 0; i < defns.length; i++) {
      defns[i].addExport(exports);
    }
  }
}
