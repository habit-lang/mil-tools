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

class AssertExp extends CodeExp {

  private AtomExp a;

  private String id;

  private CodeExp cexp;

  /** Default constructor. */
  AssertExp(Position pos, AtomExp a, String id, CodeExp cexp) {
    super(pos);
    this.a = a;
    this.id = id;
    this.cexp = cexp;
  }

  /**
   * Perform scope analysis on the AST for a sequence of code, checking that all of the referenced
   * identifiers are in scope, introducing new temporaries for each identifier that is bound in a
   * BindExp, and returning the corresponding mil Code sequence.
   */
  public Code inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    return new Assert(
        a.inScopeOf(handler, milenv, tenv),
        milenv.mustFindCfun(handler, pos, id),
        cexp.inScopeOf(handler, milenv, tenv));
  }
}
