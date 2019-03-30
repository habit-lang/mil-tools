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


/** Represents a structure type. */
public class StructType extends Type {

  /** The list of types in the structure. */
  private Type[] tys;

  /** Default constructor. */
  public StructType(Type[] tys) {
    this.tys = tys;
  }

  /** Get the type of the ith component in this (assumed) structure type. */
  public Type at(int i) {
    return tys[i];
  }

  /** Append the name of this type to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append("{");
    append(buf, tys);
    buf.append("}");
  }

  /** Calculate a default value of this type, suitable for use as an initial value. */
  public Value defaultValue() {
    // TODO: This case is included for completeness, but I don't think it will ever be needed;
    // maybe we should generate an internal error instead?
    Value[] vals = new Value[tys.length];
    for (int i = 0; i < tys.length; i++) {
      vals[i] = tys[i].defaultValue();
    }
    return new Struct(this, vals);
  }
}
