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
 * Represents a local variable, like a register or temporary, that can be used to hold the result of
 * an LLVM operation.
 */
public class Local extends Location {

  /**
   * A number that distinguishes this particular local from any other local in the same function.
   */
  private int num;

  /** Default constructor. */
  public Local(Type ty, int num) {
    super(ty);
    this.num = num;
    str = "%r" + num;
  }

  /** Caches a string representation for this left hand side in LLVM code. */
  protected String str;

  /**
   * Return the LLVM name for this value. We provide a default definition that relies on the
   * existence of an appendName() implementation, which has a default implementation in terms of
   * getName(). To avoid an infinite loop, at least one of these methods must be implemented for
   * every concrete instance of Value.
   */
  public String getName() {
    return str;
  }
}
