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

import java.io.PrintWriter;

/**
 * Represents an LLVM global variable declaration (to allow access to a variable defined elsewhere).
 */
public class GlobalVarDecl extends Defn {

  /** The name of the global variable. */
  private String name;

  /** The type of value stored in the variable. */
  private Type type;

  /** Default constructor. */
  public GlobalVarDecl(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    out.println("@" + name + " = external global " + type);
    out.println();
  }

  /**
   * Return a boolean to indicate whether there should be any output from this definition in an LLVM
   * interface description.
   */
  boolean includeInInterface() {
    return true;
  }

  /**
   * Print interface text for this definition on the specified PrintWriter, assuming that
   * this.includeInInterface() == true.
   */
  void printInterface(PrintWriter out) {
    print(out);
  }
}
