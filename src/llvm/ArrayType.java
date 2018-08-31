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


/** Represents an array type. */
public class ArrayType extends Type {

  /** The number of elements in the array. */
  private long size;

  /** The type of elements in the array. */
  private Type elemType;

  /** Default constructor. */
  public ArrayType(long size, Type elemType) {
    this.size = size;
    this.elemType = elemType;
  }

  /** Append the name of this type to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append("[" + size + " x ");
    elemType.append(buf);
    buf.append("]");
  }

  /** Calculate a default value of this type, suitable for use as an initial value. */
  public Value defaultValue() {
    return new ZeroInitializer(this);
  }
}
