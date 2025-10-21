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
 * A TVarsInScope value is a linked list data structure that is used to track the set of type
 * variables that are in scope at each point in a program during the type inference process.
 * Traditional treatments of type inference can derive this information from a "type assumption"
 * structure that is also used to look up the types of variables as they are encountered. But, as a
 * result of the earlier scope analysis, we do not need this "look up" capability here. Instead,
 * TVarsInScope lists are used only to determine which unbound type variables are in scope so that
 * we do not generalize over them when calculating most general types.
 */
abstract class TVarsInScope {

  protected TVarsInScope enclosing;

  /** Default constructor. */
  TVarsInScope(TVarsInScope enclosing) {
    this.enclosing = enclosing;
  }

  /** Return the enclosing TVarsInScope object. */
  TVarsInScope getEnclosing() {
    return enclosing;
  }

  /**
   * Calculate the list of unbound type variables in the scope that is described by the given
   * TVarsInScope value.
   */
  static TVars tvarsInScope(TVarsInScope tis) {
    if (tis == null) {
      return null;
    } else {
      TVars tvs = tvarsInScope(tis.enclosing);
      if (tvs == null) {
        // If there are no unbound type variables in the enclosing object now, then there will never
        // be any unbound type variables there in the future and we can unlink it from this list.
        tis.enclosing = null;
      }
      return tis.tvarsInScope(tvs);
    }
  }

  /**
   * Extend the given list of unbound type variables, tvs, from the enclosing scope with any
   * additional unbound type variables that appear in this specific object.
   */
  abstract TVars tvarsInScope(TVars tvs);
}
