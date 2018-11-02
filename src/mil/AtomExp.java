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
import core.*;

abstract class AtomExp {

  /**
   * Perform scope analysis on this AtomExp, checking that any referenced identifier is in scope,
   * and returning a corresponding MIL Atom.
   */
  abstract Atom inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv);

  /**
   * Check that each of the identifiers in the given list of atom expressions are in scope, either
   * as top level values (defined in the given MIL environment) or as temporaries (defined in the
   * given atom environment). The result is a corresponding array of MIL Atom values.
   */
  static Atom[] inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv, AtomExp[] es) {
    Atom[] as = new Atom[es.length];
    for (int i = 0; i < es.length; i++) {
      as[i] = es[i].inScopeOf(handler, milenv, tenv);
    }
    return as;
  }
}
