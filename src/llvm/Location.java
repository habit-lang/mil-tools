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


/** Represents an LLVM value that specifies a location in memory or in a register/temporary. */
public abstract class Location extends Value {

  /** The type of value stored in this location. */
  protected Type ty;

  /** Default constructor. */
  public Location(Type ty) {
    this.ty = ty;
  }

  /** Return the LLVM type of this value. */
  public Type getType() {
    return ty;
  }
}
