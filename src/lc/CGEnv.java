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
package lc;

import compiler.*;
import core.*;
import mil.*;

/**
 * Represents an environment mapping variables in the LC program to corresponding temporaries in the
 * generated MIL code.
 */
abstract class CGEnv {

  private CGEnv enclosing;

  /** Default constructor. */
  CGEnv(CGEnv enclosing) {
    this.enclosing = enclosing;
  }

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

  abstract Temp findInThis(DefVar v);
}
