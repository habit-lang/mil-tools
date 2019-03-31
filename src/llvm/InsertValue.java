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
package llvm;


/**
 * Insert a value into a structure/aggregate type. (Does not support the full generality of LLVM,
 * which allows the instruction to specify a sequence of index values.)
 */
public class InsertValue extends Rhs {

  /** A structure/aggregate value. */
  private Value v;

  /** The field value to insert. */
  private Value f;

  /** The index of the inserted field. */
  private int n;

  /** Default constructor. */
  public InsertValue(Value v, Value f, int n) {
    this.v = v;
    this.f = f;
    this.n = n;
  }

  /** Append a printable string for this instruction to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append("insertvalue ");
    v.append(buf);
    buf.append(", ");
    f.append(buf);
    buf.append(", ");
    buf.append(n);
  }
}
