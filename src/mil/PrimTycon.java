/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import java.io.PrintWriter;

/** Represents a type constructor that is introduced as a primitive with no other definition. */
public class PrimTycon extends Tycon {

  private Kind kind;

  private int arity;

  /** Default constructor. */
  public PrimTycon(Position pos, String id, Kind kind, int arity) {
    super(pos, id);
    this.kind = kind;
    this.arity = arity;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return kind;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    return arity;
  }

  /** Apply defaults to the kind of this type constructor to eliminate any remaining KVars. */
  public void defaultKinds() {
    kind = kind.defaultKind();
    debug.Log.println(id + " :: " + kind);
  }

  /** A constructor for defining types that have a BuiltinPosition. */
  public PrimTycon(String id, Kind kind, int arity) {
    this(BuiltinPosition.pos, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  /** A constructor for defining types that have a BuiltinPosition and fixity. */
  public PrimTycon(String id, Kind kind, int arity, Fixity fixity) {
    this(id, kind, arity);
    this.fixity = fixity;
  }

  /** Print a definition for this type constructor using source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    this.dumpFixity(out);
    out.print("type ");
    out.print(id);
    if (this.arity < Integer.MAX_VALUE && arity >= 0) {
      out.print(" ");
      out.print(this.arity);
    }
    out.print(" :: ");
    out.println(kind.toString());
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    return this;
  }
}
