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


/** Cast operators that convert a value to a different type. */
public abstract class Cast extends Expr {

  /** The value to be recast. */
  private Value v;

  /** The desired result type. */
  private Type ty;

  /** Default constructor. */
  public Cast(Value v, Type ty) {
    this.v = v;
    this.ty = ty;
  }

  /** Return the LLVM type of this value. */
  public Type getType() {
    return ty;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append(castString());
    buf.append("(");
    v.append(buf);
    buf.append(" to ");
    ty.append(buf);
    buf.append(")");
  }

  /** Return the LLVM opcode for this cast operation. */
  abstract String castString();

  /** Generate a string for executing this expression as a right hand side. */
  void appendEval(StringBuilder buf) {
    buf.append(castString());
    buf.append(" ");
    v.append(buf);
    buf.append(" to ");
    ty.append(buf);
  }
}
