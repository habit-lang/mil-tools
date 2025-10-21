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

abstract class CodeExp {

  Position pos;

  /** Default constructor. */
  CodeExp(Position pos) {
    this.pos = pos;
  }

  Tail toTail(Handler handler, MILEnv milenv, String[] ids, Temp[] ps, String[] args, Temp[] as) {
    Temp[] bps = Temp.makeTemps(ids.length);
    Temp[] bas = Temp.makeTemps(args.length);
    Code c = this.inScopeOf(handler, milenv, new TempEnv(args, bas, new TempEnv(ids, bps, null)));
    Block b = new Block(pos, Temp.append(bps, bas), c);
    return new BlockCall(b).withArgs(Temp.append(ps, as));
  }

  Tail toTail(Handler handler, MILEnv milenv, String[] args) throws Failure {
    int n = args.length;
    Temp[] ps = Temp.makeTemps(n);
    Block b = new Block(pos, ps, this.inScopeOf(handler, milenv, new TempEnv(args, ps, null)));
    return new BlockCall(b).maker(pos, n); // NOTE: types for generated code will be inferred
  }

  /**
   * Perform scope analysis on the AST for a sequence of code, checking that all of the referenced
   * identifiers are in scope, introducing new temporaries for each identifier that is bound in a
   * BindExp, and returning the corresponding mil Code sequence.
   */
  abstract Code inScopeOf(Handler handler, MILEnv milenv, TempEnv tenv);
}
