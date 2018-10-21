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
public class StructType extends Tycon {

  /** Default constructor. */
  public StructType(Position pos, String id) {
    super(pos, id);
  }

  private Type byteSize;

  public void setByteSize(Type byteSize) {
    this.byteSize = byteSize;
  }

  private long alignment;

  public void setAlignment(long alignment) {
    this.alignment = alignment;
  }

  private StructField[] fields;

  public StructField[] getFields() {
    return fields;
  }

  public void setFields(StructField[] fields) {
    this.fields = fields;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return KAtom.AREA;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    return 0;
  }

  /** Find the name of the associated struct type, if any. */
  public StructType structType() {
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

  /** Print a definition for this structure type using (an approximation to) source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    out.print("struct ");
    out.print(id);
    out.print(" / ");
    int width = byteSize.getNat().intValue();
    out.println(width);
    final String beg = "  [ ";
    final String bar = "  | ";
    if (fields.length == 0) {
      out.println(beg);
      if (width > 0) {
        dumpPadding(out, width);
        out.println(" ");
      }
      out.println("]");
    } else {
      out.print(beg);
      int next = fields[0].getOffset();
      if (next > 0) {
        dumpPadding(out, next);
        out.println();
        out.print(bar);
      }
      fields[0].dumpTypeDefinition(out);
      next += fields[0].getWidth();
      for (int i = 1; i < fields.length; i++) {
        out.println();
        out.print(bar);
        int offset = fields[i].getOffset();
        if (next < offset) {
          dumpPadding(out, offset - next);
          out.println();
          out.print(bar);
          next = offset;
        }
        fields[i].dumpTypeDefinition(out);
        next += fields[i].getWidth();
      }
      if (next < width) {
        out.println();
        out.print(bar);
        dumpPadding(out, width - next);
      }
      out.println(" ]");
    }
    if (alignment != 0) {
      out.println("  aligned " + alignment);
    }
    out.println();
  }

  private static void dumpPadding(PrintWriter out, int bytes) {
    out.print("... " + bytes + " byte" + ((bytes > 1) ? "s" : "") + " padding ...");
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    Tycon ntycon = set.mapsTyconTo(this);
    if (ntycon != null) { // Use previously computed canonical version if available
      return ntycon;
    } else if (set.containsTycon(
        this)) { // Tycon is already in the target?  (TODO: is this still necessary?)
      return this;
    }
    return makeCanonTycon(set); // But otherwise, make a new canonical version
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  Tycon makeCanonTycon(TypeSet set) {
    if (fields.length == 0) { // Do not make copies of structures with no fields
      set.addTycon(this); // (but still register them as being in use)
      return this;
    } else {
      debug.Log.println("making new version of structure type " + id);
      StructType newSt = new StructType(pos, id);
      set.mapTycon(
          this, newSt); // Add mapping before attempting to find canonical versions of fields.
      debug.Log.println("new version of StructType " + id + " is " + newSt);
      newSt.byteSize = byteSize;
      newSt.alignment = alignment;
      newSt.fields = new StructField[fields.length];
      for (int i = 0; i < fields.length; i++) {
        newSt.fields[i] = fields[i].makeCanonStructField(set);
      }
      return newSt;
    }
  }

  /** Return the nat that specifies the byte size of the type produced by this type constructor. */
  public Type byteSize() {
    return byteSize;
  }

  /** Return the alignment associated with this type constructor. */
  public long alignment() {
    return alignment;
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
}
