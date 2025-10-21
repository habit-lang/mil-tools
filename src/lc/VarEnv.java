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
package lc;

import compiler.*;
import core.*;
import mil.*;

/** An environment that extends the enclosing environment with a binding for a single identifier. */
class VarEnv extends Env {

  private DefVar v;

  /** Default constructor. */
  VarEnv(Env enclosing, DefVar v) {
    super(enclosing);
    this.v = v;
  }

  /** Lookup the definition for an identifier in this environment node. */
  DefVar findInThis(String id) {
    return v.answersTo(id) ? v : null;
  }
}
