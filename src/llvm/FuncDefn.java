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

/** Represents an LLVM function definition. */
public class FuncDefn extends Defn {

  /** Modifiers. */
  private int mods;

  /** The return type for the function. */
  private Type retType;

  /** The name of the function. */
  private String name;

  /** The formal parameters. */
  private Local[] formals;

  /** Labels for each of the basic blocks. */
  private String[] labels;

  /** Code for each of the basic blocks. */
  private Code[] bodies;

  /** Default constructor. */
  public FuncDefn(
      int mods, Type retType, String name, Local[] formals, String[] labels, Code[] bodies) {
    this.mods = mods;
    this.retType = retType;
    this.name = name;
    this.formals = formals;
    this.labels = labels;
    this.bodies = bodies;
  }

  /**
   * Default name for the main/initialization function. Made public so that it can be changed by a
   * command line option. If the name is empty, then, we will either report an error (if there is
   * initialization code) or else just not generate an initialization function at all.
   */
  public static String mainFunctionName = "";

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    out.print("define ");
    out.print(Mods.toString(mods));
    out.print(retType.toString());
    out.print(" @" + name + "(");
    for (int i = 0; i < formals.length; i++) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(formals[i].toString());
    }
    out.print(") {");

    // Print code for each of the basic blocks:
    for (int i = 0; i < labels.length; i++) {
      out.println();
      if (i != 0) {
        out.println(labels[i] + ":");
      }
      bodies[i].print(out);
    }

    // Terminate the function definition:
    out.println("}");
    out.println();
  }

  /**
   * Return a boolean to indicate whether there should be any output from this definition in an LLVM
   * interface description.
   */
  boolean includeInInterface() {
    return !Mods.isLocal(mods);
  }

  /**
   * Print interface text for this definition on the specified PrintWriter, assuming that
   * this.includeInInterface() == true.
   */
  void printInterface(PrintWriter out) {
    out.print("define ");
    out.print(Mods.toString(mods));
    out.print(retType.toString());
    out.print(" @" + name + "(");
    for (int i = 0; i < formals.length; i++) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(formals[i].getType().toString());
    }
    out.println(")");
    out.println();
  }
}
