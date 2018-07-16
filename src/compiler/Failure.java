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
 * Represents an error diagnostic. To avoid a clash with java.lang.Error, we resisted the temptation
 * to call this class Error.
 */
public class Failure extends Diagnostic {

  /** Construct a simple failure report with a fixed description. */
  public Failure(String text) {
    super(text);
  }

  /** Construct a failure report for a particular source position. */
  public Failure(Position position) {
    super(position);
  }

  /** Construct a simple failure report with a fixed description and a source position. */
  public Failure(Position position, String text) {
    super(position, text);
  }
}
