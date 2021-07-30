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
 * A TVarsInScope for code that appears within the scope of an array of variables (i.e., a lambda or
 * a case alternative).
 */
class TVISVars extends TVarsInScope {

  private DefVar[] vs;

  /** Default constructor. */
  TVISVars(TVarsInScope enclosing, DefVar[] vs) {
    super(enclosing);
    this.vs = vs;
  }

  /**
   * Extend the given list of unbound type variables, tvs, from the enclosing scope with any
   * additional unbound type variables that appear in this specific object.
   */
  TVars tvarsInScope(TVars tvs) {
    for (int i = 0; i < vs.length; i++) {
      tvs = vs[i].getScheme().tvars(tvs);
    }
    return tvs;
  }
}
