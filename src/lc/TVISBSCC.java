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
 * A TVarsInScope for code that appears within the scope of a binding group (e.g., in a let or top
 * level definition).
 */
class TVISBSCC extends TVarsInScope {

  private BindingSCC scc;

  /** Default constructor. */
  TVISBSCC(TVarsInScope enclosing, BindingSCC scc) {
    super(enclosing);
    this.scc = scc;
  }

  /**
   * Extend the given list of unbound type variables, tvs, from the enclosing scope with any
   * additional unbound type variables that appear in this specific object.
   */
  TVars tvarsInScope(TVars tvs) {
    for (Bindings bs = scc.getBindings(); bs != null; bs = bs.next) {
      tvs = bs.head.tvarsInScope(tvs);
    }
    return tvs;
  }
}
