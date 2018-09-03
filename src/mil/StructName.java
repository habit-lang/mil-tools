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
import compiler.Position;
import core.*;
import java.io.PrintWriter;

/** Represents a type constructor that is introduced as a memory structure type. */
public class StructName extends Tycon {

  /** Default constructor. */
  public StructName(Position pos, String id, Kind kind) {
    super(pos, id, kind);
  }

  private Type byteSize;

  private StructField[] fields;

  public void setByteSize(Type byteSize) {
    this.byteSize = byteSize;
  }

  public StructField[] getFields() {
    return fields;
  }

  public void setFields(StructField[] fields) {
    this.fields = fields;
  }

  /** Find the name of the associated struct type, if any. */
  public StructName structName() {
    return this;
  }

  public void debugDump() {
    debug.Log.println(
        "Struct " + id + ": size " + byteSize + ", with " + fields.length + " field(s):");
    for (int i = 0; i < fields.length; i++) {
      debug.Log.print("  " + i + ": ");
      fields[i].debugDump();
    }
  }

  /** Print a definition for this structure type using source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    out.print("struct ");
    out.print(id);
    out.print(" /");
    out.println(byteSize.toString());
    if (fields.length == 0) {
      out.println("[ ]");
    } else {
      out.print(" [ ");
      fields[0].dumpTypeDefinition(out);
      for (int i = 1; i < fields.length; i++) {
        out.print(" | ");
        fields[i].dumpTypeDefinition(out);
      }
      out.println(" ]");
    }
    out.println();
  }

  /**
   * Return a primitive for turning an initializer for the ith field of this structure into an
   * initializer for the full structure. This primitive is used exclusively in the code that is
   * generated for structure initializers, which ensures that it will be part of a set of
   * initializers that, together, initialize the full structure.
   */
  public Prim initStructFieldPrim(int i) {
    return fields[i].initStructFieldPrim(this);
  }

  /** Return the nat that specifies the byte size of the type produced by this type constructor. */
  public Type byteSize() {
    return byteSize;
  }
}
