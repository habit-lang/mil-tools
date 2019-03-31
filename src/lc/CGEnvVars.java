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

class CGEnvVars extends CGEnv {

  private DefVar[] vs;

  private Temp[] ts;

  /** Default constructor. */
  CGEnvVars(CGEnv enclosing, DefVar[] vs, Temp[] ts) {
    super(enclosing);
    this.vs = vs;
    this.ts = ts;
  }

  /**
   * Search for an entry for the given DefVar in this CGEnv node (ignoring the enclosing
   * environment, if any).
   */
  Temp findInThis(DefVar v) {
    for (int i = 0; i < vs.length; i++) {
      if (vs[i] == v) {
        return ts[i];
      }
    }
    return null;
  }
}
