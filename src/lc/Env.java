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

abstract class Env {

  protected Env enclosing;

  /** Default constructor. */
  Env(Env enclosing) {
    this.enclosing = enclosing;
  }

  /** Lookup the definition for a specific identifier in an environment. */
  static DefVar find(String id, Env env) {
    for (; env != null; env = env.enclosing) {
      DefVar v = env.findInThis(id);
      if (v != null) {
        return v;
      }
    }
    return null;
  }

  /** Lookup the definition for an identifier in this environment node. */
  abstract DefVar findInThis(String id);
}
