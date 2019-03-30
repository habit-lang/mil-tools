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


/** Represents an index into a structure as required for getelementptr. */
public class Index extends Value {

  /** The numeric value of the index. */
  private int num;

  /** Default constructor. */
  public Index(int num) {
    this.num = num;
  }

  /** The index constant with all bits zero. */
  public static final Index ZERO = new Index(0);

  /** Return the LLVM type of this value. */
  public Type getType() {
    return Type.i32;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append(num);
  }
}
