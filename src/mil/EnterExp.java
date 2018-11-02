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

class EnterExp extends TailExp {

  private VarExp fun;

  private AtomExp[] args;

  /** Default constructor. */
  EnterExp(VarExp fun, AtomExp[] args) {
    this.fun = fun;
    this.args = args;
  }

  /**
   * Perform scope analysis on this TailExp, checking that all of the referenced identifiers are in
   * scope and returning a corresponding mil Tail.
   */
  public Tail inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    return new Enter(
        fun.inScopeOf(handler, milenv, tenv), AtomExp.inScopeOf(handler, milenv, tenv, args));
  }
}
