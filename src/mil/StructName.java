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

/** Represents a type constructor that is introduced as a memory structure type. */
public class StructName extends Tycon {

  /** Default constructor. */
  public StructName(Position pos, String id, Kind kind) {
    super(pos, id, kind);
  }

  /** Find the name of the associated struct type, if any. */
  public StructName structName() {
    return this;
  }

  private Type byteSize;

  public void setByteSize(Type byteSize) {
    this.byteSize = byteSize;
  }

  /** Return the nat that specifies the byte size of the type produced by this type constructor. */
  public Type byteSize() {
    return byteSize;
  }

  private StructField[] fields;

  public StructField[] getFields() {
    return fields;
  }

  public void setFields(StructField[] fields) {
    this.fields = fields;
  }

  public void debugDump() {
    debug.Log.println(
        "Struct " + id + ": size " + byteSize + ", with " + fields.length + " field(s):");
    for (int i = 0; i < fields.length; i++) {
      debug.Log.print("  " + i + ": ");
      fields[i].debugDump();
    }
  }
}
