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

/** Represents an LLVM alias definition. */
public class Alias extends Defn {

  /** Modifiers. */
  private int mods;

  /** The name of the new item. */
  private String name;

  /** The value being aliased. */
  private Value val;

  /** Default constructor. */
  public Alias(int mods, String name, Value val) {
    this.mods = mods;
    this.name = name;
    this.val = val;
  }

  /** Print full text for this definition on the specified PrintWriter. */
  void print(PrintWriter out) {
    out.println(
        "@" + name + " = " + Mods.toString(mods) + "alias " + val.getType().ptsTo() + ", " + val);
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
    out.println("@" + name + " = " + Mods.toString(mods) + "alias " + val.getType().ptsTo());
    out.println();
  }
}
