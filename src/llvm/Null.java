/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
 * Represents an LLVM null pointer value. TODO: should we store the type that is pointed to rather
 * than the actual pointer type? With the current definition, it is possible to create a Null value
 * that does not have pointer type ...
 */
public class Null extends Value {

  /** The type for this null (which should be a pointer type). */
  private Type ty;

  /** Default constructor. */
  public Null(Type ty) {
    this.ty = ty;
  }

  /** Return the LLVM type of this value. */
  public Type getType() {
    return ty;
  }

  /**
   * Return the LLVM name for this value. We provide a default definition that relies on the
   * existence of an appendName() implementation, which has a default implementation in terms of
   * getName(). To avoid an infinite loop, at least one of these methods must be implemented for
   * every concrete instance of Value.
   */
  public String getName() {
    return "null";
  }
}
