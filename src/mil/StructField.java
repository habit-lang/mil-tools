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
package mil;

import compiler.*;
import core.*;
import java.io.PrintWriter;

/** Represents a single field of a struct type. */
public class StructField extends Name {

  /** Specifies the type of area in this field. */
  private Type type;

  /** The offset (measured in bytes from the start of the struct to this field. */
  private int offset;

  /** The width (in bytes) of this field. */
  private int width;

  /** Default constructor. */
  public StructField(Position pos, String id, Type type, int offset, int width) {
    super(pos, id);
    this.type = type;
    this.offset = offset;
    this.width = width;
  }

  public Type getType() {
    return type;
  }

  public int getOffset() {
    return offset;
  }

  public int getWidth() {
    return width;
  }

  public void debugDump() {
    debug.Log.println(id + " :: " + type + " -- offset=" + offset + ", width=" + width);
  }

  /** Print a definition for this structure type using (an approximation to) source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    out.print(id);
    out.print(" :: ");
    out.print(type.toString());
    out.print("  {- offset=" + offset + " -}");
  }

  /**
   * Make a new version of this structure field with a types that is canonical wrt the given set.
   */
  StructField makeCanonStructField(TypeSet set) {
    StructField field = new StructField(pos, id, type.canonType(set), offset, width);
    field.selectPrim = selectPrim.canonPrim(set);
    return field;
  }

  private Prim selectPrim;

  public Prim getSelectPrim() {
    return selectPrim;
  }

  /** Generate code for a selection operator for this field. */
  public void generateSelector(StructType st) {
    Temp[] args = Temp.makeTemps(Tycon.wordRep);
    Block impl = new Block(pos, args, new Done(Prim.add.withArgs(args[0], offset)));
    BlockType bt = new BlockType(Type.tuple(Type.ref(st.asType())), Type.tuple(Type.ref(type)));
    selectPrim = new Prim.blockImpl("select_" + id, Prim.PURE, bt, impl);
  }

  /**
   * Stores a field initializer primitive of type [Init T] >>= [Init S] for this field (of type T)
   * in a structure (of type S).
   */
  private Prim initStructField = null;

  /**
   * Return the initializer primitive for this field, calculating a definition for the primitive on
   * the first call.
   */
  Prim initStructFieldPrim(StructType st) {
    if (initStructField == null) {
      BlockType bt = new BlockType(Type.tuple(Type.init(type)), Type.tuple(Type.init(st.asType())));
      initStructField = new Prim.initStructField("init_" + id, Prim.PURE, bt, offset);
    }
    return initStructField;
  }

  private TopLevel defaultInit = null;

  public TopLevel getDefaultInit() {
    return defaultInit;
  }

  public void setDefaultInit(TopLevel defaultInit) {
    this.defaultInit = defaultInit;
  }
}
