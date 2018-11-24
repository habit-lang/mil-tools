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
 * An identifier in an LC program may reference a lambda-bound variable, a let-bound variable, or a
 * variable that is bound to a top-level MIL primitive.
 */
abstract class Var {

  public static DefVar[] noVars = new DefVar[0];

  /** Find an identifier associated with this variable. */
  public abstract String getId();

  /** Determine whether this variable can be referenced by the specified identifier. */
  public abstract boolean answersTo(String id);

  /** Return a printable description of this variable. */
  public abstract String toString();

  /**
   * Find the Binding corresponding to a particular Var in a given list of Bindings, or return null
   * if no such Binding can be found.
   */
  Binding find(Bindings bs) {
    return null;
  }

  /** Add this Var to the given list of DefVars. */
  abstract DefVars add(DefVars vs);

  abstract DefVars remove(DefVars vs);

  /** Return a type for an instantiated version of this variable. */
  abstract Type instantiate();

  /**
   * Find a lifting for this variable (which will definitely be null if this variable is not a
   * Binding).
   */
  Lifting findLifting(LiftEnv lenv) {
    return null;
  }

  /** Look for a binding for this lc variable in the given environment. */
  abstract Atom lookup(CGEnv env);
}
