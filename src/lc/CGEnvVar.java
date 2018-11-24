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

class CGEnvVar extends CGEnv {

  private DefVar v;

  private Temp t;

  /** Default constructor. */
  CGEnvVar(CGEnv enclosing, DefVar v, Temp t) {
    super(enclosing);
    this.v = v;
    this.t = t;
  }

  /**
   * Search for an entry for the given DefVar in this CGEnv node (ignoring the enclosing
   * environment, if any).
   */
  Temp findInThis(DefVar v) {
    return (this.v == v) ? this.t : null;
  }
}
