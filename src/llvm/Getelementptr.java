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


/** Represents a getelementptr instruction. */
public class Getelementptr extends Expr {

  /** The type of the result. */
  private Type ty;

  /** Base pointer. */
  private Value ptr;

  /** The index from the pointer. */
  private Value[] offsets;

  /** Default constructor. */
  public Getelementptr(Type ty, Value ptr, Value[] offsets) {
    this.ty = ty;
    this.ptr = ptr;
    this.offsets = offsets;
  }

  public Getelementptr(Type ty, Value ptr, Value o1) {
    this(ty, ptr, new Value[] {o1});
  }

  public Getelementptr(Type ty, Value ptr, Value o1, Value o2) {
    this(ty, ptr, new Value[] {o1, o2});
  }

  public Getelementptr(Type ty, Value ptr, Value o1, Value o2, Value o3) {
    this(ty, ptr, new Value[] {o1, o2, o3});
  }

  /** Return the LLVM type of this value. */
  public Type getType() {
    return ty;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append("getelementptr inbounds (");
    ptr.getType().ptsTo().append(buf);
    buf.append(", ");
    ptr.append(buf);
    for (int i = 0; i < offsets.length; i++) {
      buf.append(", ");
      offsets[i].append(buf);
    }
    buf.append(")");
  }

  /** Generate a string for executing this expression as a right hand side. */
  void appendEval(StringBuilder buf) {
    buf.append("getelementptr inbounds ");
    ptr.getType().ptsTo().append(buf);
    buf.append(", ");
    ptr.append(buf);
    for (int i = 0; i < offsets.length; i++) {
      buf.append(", ");
      offsets[i].append(buf);
    }
  }
}
