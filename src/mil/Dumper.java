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
package mil;

import compiler.*;
import core.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base class for objects that can be used to generate a printable description of an item on the
 * console, to a file, or on a PrintWriter.
 */
public abstract class Dumper {

  /** Return a string to describe the type of item that is being dumped. */
  public abstract String description();

  /** Write a description of this item on the specified PrintWriter. */
  public abstract void dump(PrintWriter out);

  /** Dump a description of this item on the standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Write a description of this item in a file with the specified name. */
  public void dump(String description, String name) {
    try {
      PrintWriter out = new PrintWriter(name);
      dump(out);
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to write " + description + " to \"" + name + "\" failed");
    }
  }
}
