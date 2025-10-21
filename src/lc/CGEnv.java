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

/**
 * Represents an environment mapping variables in LC programs to corresponding temporaries in
 * generated MIL code.
 */
abstract class CGEnv {

  private CGEnv enclosing;

  /** Default constructor. */
  CGEnv(CGEnv enclosing) {
    this.enclosing = enclosing;
  }

  /** Find the temporary corresponding to a given DefVar in the specified environment. */
  static Temp lookup(DefVar v, CGEnv env) {
    for (; env != null; env = env.enclosing) {
      Temp t = env.findInThis(v);
      if (t != null) {
        return t;
      }
    }
    debug.Internal.error("no definition for " + v.getId() + " in CGEnv");
    return null; // not reached
  }

  /**
   * Search for an entry for the given DefVar in this CGEnv node (ignoring the enclosing
   * environment, if any).
   */
  abstract Temp findInThis(DefVar v);
}
