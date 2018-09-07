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
import compiler.BuiltinPosition;
import compiler.Position;
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

  public void fixKinds() {
    kind = kind.fixKind();
    debug.Log.println(id + " :: " + kind);
  }

  /** A constructor for defining types that have a BuiltinPosition. */
  public PrimTycon(String id, Kind kind, int arity) {
    this(BuiltinPosition.pos, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  /**
   * Print a definition for this type constructor using source level syntax. TODO: Find a more
   * appropriate place for this code ...
   */
  void dumpTypeDefinition(PrintWriter out) {
    /* do nothing */
  }
}
