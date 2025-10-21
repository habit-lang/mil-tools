/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

class SelExp extends TailExp {

  private Position pos;

  private String id;

  private String subid;

  private int n;

  private AtomExp a;

  /** Default constructor. */
  SelExp(Position pos, String id, String subid, int n, AtomExp a) {
    this.pos = pos;
    this.id = id;
    this.subid = subid;
    this.n = n;
    this.a = a;
  }

  /**
   * Perform scope analysis on this TailExp, checking that all of the referenced identifiers are in
   * scope and returning a corresponding mil Tail.
   */
  Tail inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    return new Sel(
        milenv.mustFindCfun(handler, pos, id, subid), n, a.inScopeOf(handler, milenv, tenv));
  }
}
