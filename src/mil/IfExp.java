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

class IfExp extends CodeExp {

  private AtomExp a;

  private BlockCallExp ifTrue;

  private BlockCallExp ifFalse;

  /** Default constructor. */
  IfExp(Position pos, AtomExp a, BlockCallExp ifTrue, BlockCallExp ifFalse) {
    super(pos);
    this.a = a;
    this.ifTrue = ifTrue;
    this.ifFalse = ifFalse;
  }

  /**
   * Perform scope analysis on the AST for a sequence of code, checking that all of the referenced
   * identifiers are in scope, introducing new temporaries for each identifier that is bound in a
   * BindExp, and returning the corresponding mil Code sequence.
   */
  Code inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    return new If(
        a.inScopeOf(handler, milenv, tenv),
        ifTrue.blockCallInScopeOf(handler, milenv, tenv),
        ifFalse.blockCallInScopeOf(handler, milenv, tenv));
  }
}
