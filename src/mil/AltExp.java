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

class AltExp {

  private Position pos;

  private String id;

  private BlockCallExp bc;

  /** Default constructor. */
  AltExp(Position pos, String id, BlockCallExp bc) {
    this.pos = pos;
    this.id = id;
    this.bc = bc;
  }

  /** Perform scope analysis on the given array of case alternatives. */
  static Alt[] inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv, AltExp[] altexps) {
    Alt[] alts = new Alt[altexps.length];
    for (int i = 0; i < altexps.length; i++) {
      alts[i] = altexps[i].inScopeOf(handler, milenv, tenv);
    }
    return alts;
  }

  /**
   * Perform scope analysis on this case alternative, checking that the referenced constructor
   * function is in scope and that the block call is valid.
   */
  Alt inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    return new Alt(
        milenv.mustFindCfun(handler, pos, id), bc.blockCallInScopeOf(handler, milenv, tenv));
  }
}
