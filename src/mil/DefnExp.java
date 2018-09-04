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

abstract class DefnExp {

  protected Position pos;

  /** Default constructor. */
  DefnExp(Position pos) {
    this.pos = pos;
  }

  /**
   * Add environment entries for this definition to the given environment. If an error is detected,
   * then it is reported to the specified handler.
   */
  void addTo(Handler handler, MILEnv milenv) {
    try {
      addTo(milenv);
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  abstract void addTo(MILEnv milenv) throws Failure;

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  abstract void inScopeOf(Handler handler, MILEnv milenv) throws Failure;

  /** Add items in export lists to the specified MIL environment. */
  void addExports(MILEnv exports, MILProgram program) {
    /* do nothing */
  }

  /**
   * Add the MIL definition associated with this DefnExp, if any, as an entrypoint to the specified
   * program.
   */
  void addAsEntryTo(MILProgram mil) {
    /* nothing to do, in general */
  }
}
