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
 * A base class for objects that represent compiler diagnostics. Errors should be implemented as
 * subclasses of Failure, while warnings are normally implemented as as subclasses of Warning.
 */
public abstract class Diagnostic extends Exception {

  /**
   * A pointer to the place where the error was detected. A null value can be used for diagnostics
   * that are not associated with any particular point in the source.
   */
  private Position pos;

  /**
   * Used to hold a simple description of the problem that occurred. This field is used by the
   * default implementation of getDescription(); more complex diagnostics may override this method,
   * and not use this field.
   */
  private String text;

  /** Default constructor. */
  public Diagnostic(Position pos, String text) {
    this.pos = pos;
    this.text = text;
  }

  public Position getPos() {
    return pos;
  }

  public String getText() {
    return text;
  }

  /** Construct a simple diagnostic with no associated position. */
  public Diagnostic(String text) {
    this(null, text);
  }
}
