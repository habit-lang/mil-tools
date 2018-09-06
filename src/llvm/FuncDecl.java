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

/** Represents an LLVM function declaration (to allow the use of a function defined elsewhere). */
public class FuncDecl extends Defn {

  /** The name of the function. */
  private String name;

  /** The types of the function. */
  private FunctionType ftype;

  /** Default constructor. */
  public FuncDecl(String name, FunctionType ftype) {
    this.name = name;
    this.ftype = ftype;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    ftype.printFunDecl(out, name);
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
