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
package lc;

import compiler.*;
import core.*;
import mil.*;

/**
 * An environment that extends the enclosing environment with bindings for an array of identifiers.
 */
class VarsEnv extends Env {

  private DefVar[] vs;

  /** Default constructor. */
  VarsEnv(Env enclosing, DefVar[] vs) {
    super(enclosing);
    this.vs = vs;
  }

  /** Lookup the definition for an identifier in this environment node. */
  DefVar findInThis(String id) {
    for (int i = 0; i < vs.length; i++) {
      if (vs[i].answersTo(id)) {
        return vs[i];
      }
    }
    return null;
  }
}
