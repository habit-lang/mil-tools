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

  /** The name of the new item. */
  private String name;

  /** The value being aliased. */
  private Value val;

  /** Default constructor. */
  public Alias(String name, Value val) {
    this.name = name;
    this.val = val;
  }

  void print(PrintWriter out) {
    out.println("@" + name + " = alias " + val.getType().ptsTo() + ", " + val);
  }
}
