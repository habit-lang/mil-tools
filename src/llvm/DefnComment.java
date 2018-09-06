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

/** Represents an LLVM definition with a comment. */
public class DefnComment extends Defn {

  /** The text of the comment. */
  private String comment;

  /** The definition that the comment applies to. */
  private Defn defn;

  /** Default constructor. */
  public DefnComment(String comment, Defn defn) {
    this.comment = comment;
    this.defn = defn;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    Program.printComment(out, "", comment);
    defn.print(out);
  }

  /**
   * Return a boolean to indicate whether there should be any output from this definition in an LLVM
   * interface description.
   */
  boolean includeInInterface() {
    return defn.includeInInterface();
  }

  /**
   * Print interface text for this definition on the specified PrintWriter, assuming that
   * this.includeInInterface() == true.
   */
  void printInterface(PrintWriter out) {
    print(out);
  }
}
