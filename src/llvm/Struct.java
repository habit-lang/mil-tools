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
package llvm;


/** Represents an LLVM structure value. */
public class Struct extends Value {

  /** The type of this structure (will be computed if left null). */
  private Type ty;

  /** An array of component values. */
  private Value[] vals;

  /** Default constructor. */
  public Struct(Type ty, Value[] vals) {
    this.ty = ty;
    this.vals = vals;
  }

  /** Return the LLVM type of this value. */
  public Type getType() {
    if (ty == null) {
      Type[] tys = new Type[vals.length];
      for (int i = 0; i < vals.length; i++) {
        tys[i] = vals[i].getType();
      }
      ty = new StructType(tys);
    }
    return ty;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    append(buf, "{", vals, "}");
  }
}
