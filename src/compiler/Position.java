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
package compiler;

/**
 * An abstraction of a position within a source document.
 *
 * <p>[We might also like to have a mechanism for starting an editor at a particular position so
 * that users can view and potentially change the corresponding sections of source code.]
 */
public abstract class Position {

  /** Obtain a printable description of the source position. */
  public abstract String describe();

  /**
   * Return a column number for this position. By convention, column numbers start at zero. If no
   * sensible column number value is available, then we return zero. Calling methods will not be
   * able to distinguish between uses of zero as a genuine column number, and uses of zero as a "no
   * column number available" indicator.
   */
  public int getColumn() {
    return 0;
  }

  /**
   * Return a row number for this position. By convention, row numbers start at zero. If no sensible
   * row number value is available, then we return zero. Calling methods will not be able to
   * distinguish between uses of zero as a genuine row number, and uses of zero as a "no row number
   * available" indicator.
   */
  public int getRow() {
    return 0;
  }

  /** Copy a source position. */
  public abstract Position copy();
}
