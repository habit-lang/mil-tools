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
 * Representation for variables that are declared for use in this program but defined elsewhere (and
 * made available via a top-level definition in a MIL program).
 */
class DecVar extends Var {

  private Top top;

  /** Default constructor. */
  DecVar(Top top) {
    this.top = top;
  }

  /** Find an identifier associated with this variable. */
  public String getId() {
    return top.getId();
  }

  /** Determine whether this variable can be referenced by the specified identifier. */
  public boolean answersTo(String id) {
    return id.equals(top.getId());
  }

  /** Return a printable description of this variable. */
  public String toString() {
    return getId();
  }

  /** Add this variable to the given list of DefVars. */
  DefVars add(DefVars vs) {
    return vs;
  }

  /** Remove this variable from the given list of DefVars. */
  DefVars remove(DefVars vs) {
    return vs;
  }

  /** Return a type for an instantiated version of this variable. */
  Type instantiate() {
    return top.instantiate();
  }

  /** Find the binding for this LC variable in the given environment. */
  Atom lookup(CGEnv env) {
    return top;
  }
}
