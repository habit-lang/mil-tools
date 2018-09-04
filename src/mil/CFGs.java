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
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class CFGs {

  public CFG head;

  public CFGs next;

  /** Default constructor. */
  public CFGs(CFG head, CFGs next) {
    this.head = head;
    this.next = next;
  }

  public static void toDot(CFGs cfgs) {
    PrintWriter out = new PrintWriter(System.out);
    toDot(out, cfgs);
    out.flush();
  }

  public static void toDot(String name, CFGs cfgs) {
    try {
      PrintWriter out = new PrintWriter(name);
      toDot(out, cfgs);
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to create dot output in \"" + name + "\" failed");
    }
  }

  public static void toDot(PrintWriter out, CFGs cfgs) {
    out.println("digraph CFGs {");
    for (; cfgs != null; cfgs = cfgs.next) {
      cfgs.head.cfgToDot(out);
    }
    out.println("}");
  }
}
