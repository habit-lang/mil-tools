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
public abstract class CastOp extends Rhs {

  /** The value to be recast. */
  private Value v;

  /** The desired result type. */
  private Type type;

  /** Default constructor. */
  public CastOp(Value v, Type type) {
    this.v = v;
    this.type = type;
  }

  /** Generate a string for this right hand side using the given string as the operation name. */
  void append(StringBuilder buf, String op) {
    buf.append(op);
    buf.append(" ");
    v.append(buf);
    buf.append(" to ");
    type.append(buf);
  }
}
