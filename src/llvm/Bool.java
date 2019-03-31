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


/** Represents an LLVM boolean constant value. */
public class Bool extends Value {

  /** The boolean value associated with this Bool. */
  private boolean bool;

  /** Default constructor. */
  public Bool(boolean bool) {
    this.bool = bool;
  }

  /** The boolean constant true. */
  public static final Bool TRUE = new Bool(true);

  /** The boolean constant false. */
  public static final Bool FALSE = new Bool(false);

  /** Return the LLVM type of this value. */
  public Type getType() {
    return Type.i1;
  }

  /**
   * Return the LLVM name for this value. We provide a default definition that relies on the
   * existence of an appendName() implementation, which has a default implementation in terms of
   * getName(). To avoid an infinite loop, at least one of these methods must be implemented for
   * every concrete instance of Value.
   */
  public String getName() {
    return bool ? "true" : "false";
  }
}
