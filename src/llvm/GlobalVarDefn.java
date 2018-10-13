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

/** Represents an LLVM global variable definition. */
public class GlobalVarDefn extends Defn {

  /** Modifiers. */
  private int mods;

  /** The name of the global variable. */
  private String name;

  /** The initial value. */
  private Value initial;

  /** Alignment (or zero to omit). */
  private long alignment;

  /** Default constructor. */
  public GlobalVarDefn(int mods, String name, Value initial, long alignment) {
    this.mods = mods;
    this.name = name;
    this.initial = initial;
    this.alignment = alignment;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    out.print("@" + name + " = " + Mods.toString(mods) + "global " + initial);
    if (alignment != 0) {
      out.print(", align " + alignment);
    }
    out.println();
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
    out.print("@" + name + " = " + Mods.toString(mods) + "global " + initial.getType());
    if (alignment != 0) {
      out.print(", align " + alignment);
    }
    out.println();
    out.println();
  }
}
