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

  private String subid;

  private BlockCallExp bc;

  /** Default constructor. */
  AltExp(Position pos, String id, String subid, BlockCallExp bc) {
    this.pos = pos;
    this.id = id;
    this.subid = subid;
    this.bc = bc;
  }

  /** Perform scope analysis on the given array of case alternatives. */
  static Alts inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv, AltExp[] altexps, Alts alts) {
    for (int i = altexps.length; --i >= 0; ) {
      alts = altexps[i].inScopeOf(handler, milenv, tenv, alts);
    }
    return alts;
  }

  /**
   * Perform scope analysis on this case alternative, checking that the referenced constructor
   * function is in scope and that the block call is valid.
   */
  Alts inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv, Alts alts) {
    return new CfunAlt(
        milenv.mustFindCfun(handler, pos, id, subid),
        bc.blockCallInScopeOf(handler, milenv, tenv),
        alts);
  }
}
