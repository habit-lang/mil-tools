/*
    Copyright 2018-19 Mark P Jones, Portland State University

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

class VarExp extends AtomExp {

  private Position pos;

  private String id;

  /** Default constructor. */
  VarExp(Position pos, String id) {
    this.pos = pos;
    this.id = id;
  }

  /**
   * Perform scope analysis on this AtomExp, checking that any referenced identifier is in scope,
   * and returning a corresponding MIL Atom.
   */
  Atom inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    Temp a = TempEnv.find(id, tenv); // Look for locally bound atom
    if (a != null) {
      return a;
    }
    Cfun cf = milenv.findCfun(id); // Look for a constructor function
    if (cf != null) {
      return cf.getTop();
    }
    return milenv.mustFindTop(handler, pos, id); // Look for a top-level definition
  }
}
