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

import java.io.PrintWriter;

/** Represents an LLVM type definition. */
public class Typedef extends Defn {

  /** The type that is being defined. */
  private DefinedType def;

  /** Default constructor. */
  public Typedef(DefinedType def) {
    this.def = def;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    out.println(def + " = type " + def.definition());
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
