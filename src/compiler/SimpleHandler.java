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
 * A simple implementation of the Handler interface that prints the position and description of each
 * diagnostic on System.err, and then returns to the caller.
 */
public class SimpleHandler extends Handler {

  /** Respond to a diagnostic by displaying it on the error output stream. */
  protected void respondTo(Diagnostic d) {
    System.err.print((d instanceof Warning) ? "WARNING: " : "ERROR: ");
    Position pos = d.getPos();
    if (pos != null) {
      System.err.println(pos.describe());
    }
    String txt = d.getText();
    if (txt != null) {
      System.err.println(txt);
    }
    System.err.println();
  }
}
