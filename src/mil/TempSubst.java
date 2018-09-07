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
package mil;

import compiler.*;
import core.*;

/** TempSubst value represent substitutions of Atoms for Temps as simple linked list structures. */
public class TempSubst {

  private Temp v;

  private Atom a;

  private TempSubst rest;

  /** Default constructor. */
  public TempSubst(Temp v, Atom a, TempSubst rest) {
    this.v = v;
    this.a = a;
    this.rest = rest;
  }

  /** Extend a substitution with bindings given by a pair of arrays. */
  public static TempSubst extend(Temp[] vs, Atom[] as, TempSubst s) {
    if (vs.length != as.length) {
      debug.Internal.error("TempSubst.extend: variable/atom counts do not match.");
    }
    for (int i = 0; i < as.length; i++) {
      s = vs[i].mapsTo(as[i], s);
    }
    return s;
  }

  /** Return a printable representation of this substitution (for debugging purposes). */
  static String toString(TempSubst s) {
    StringBuilder buf = new StringBuilder("[");
    for (int i = 0; s != null; s = s.rest) {
      if (0 < i++) {
        buf.append(", ");
      }
      buf.append(s.v.toString());
      buf.append(" --> ");
      buf.append(s.a.toString());
    }
    buf.append("]");
    return buf.toString();
  }

  /** Apply the given substitution to the specified Temp. */
  public static Atom apply(Temp w, TempSubst s) {
    for (; s != null; s = s.rest) {
      if (s.v == w) {
        return s.a;
      }
    }
    return w;
  }

  /** Apply the given substitution to an array of atoms. */
  public static Atom[] apply(Atom[] args, TempSubst s) {
    Atom[] nargs = new Atom[args.length];
    for (int i = 0; i < args.length; i++) {
      nargs[i] = args[i].apply(s);
    }
    return nargs;
  }
}
