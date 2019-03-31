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

/** Abstract base class for variables that are defined within the current lc program. */
abstract class DefVar extends Var {

  /**
   * Record the type of the defining occurrence of this variable. (If an explicit type signature has
   * been provided, then uses of the binding within its own definition can be at different instances
   * of the given signature. If no explicit type signature is provided, however, then all instances
   * share the same monomorphic type.)
   */
  protected Type type;

  /** Return the most general type available for this variable. */
  public Scheme getScheme() {
    return type;
  }

  /** Return a printable description of this variable. */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    append(buf);
    return buf.toString();
  }

  /** Return a printable description of an array of DefVar objects. */
  static String toString(String prefix, DefVar[] vs) {
    StringBuilder buf = new StringBuilder(prefix);
    for (int i = 0; i < vs.length; i++) {
      if (i > 0) {
        buf.append(" ");
      }
      vs[i].append(buf);
    }
    buf.append(" ->");
    return buf.toString();
  }

  /** Append a printable description of this DefVar to the given buffer. */
  void append(StringBuilder buf) {
    if (type == null) {
      buf.append(getId());
    } else {
      buf.append("(");
      buf.append(getId());
      buf.append(" :: ");
      buf.append(type.skeleton().toString());
      buf.append(")");
    }
    buf.append(" ");
  }

  /** Add this variable to the given list of DefVars. */
  DefVars add(DefVars vs) {
    return DefVars.add(this, vs);
  }

  /** Remove this variable from the given list of DefVars. */
  DefVars remove(DefVars vs) {
    return DefVars.remove(this, vs);
  }

  /**
   * Set this variable's type to be a fresh type variable. Used to initialize the type field of a
   * Var.
   */
  public Type freshType(Tyvar tyvar) {
    return type = new TVar(tyvar);
  }

  /** Return a type for an instantiated version of this variable. */
  Type instantiate() {
    return type;
  }

  /** Find the binding for this LC variable in the given environment. */
  Atom lookup(CGEnv env) {
    return CGEnv.lookup(this, env);
  }

  /** Generate an array of new mil temporaries from an array of lc temporaries. */
  static Temp[] freshTemps(DefVar[] vs) {
    int n = vs.length;
    Temp[] ts = new Temp[n];
    for (int i = 0; i < n; i++) {
      ts[i] = vs[i].freshTemp();
    }
    return ts;
  }

  /** Generate a new MIL temporary from an LC DefVar. */
  Temp freshTemp() {
    return new Temp(type);
  }
}
