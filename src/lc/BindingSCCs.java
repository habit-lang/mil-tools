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
package lc;

import compiler.*;
import core.*;
import debug.Log;
import mil.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class BindingSCCs {

  public BindingSCC head;

  public BindingSCCs next;

  /** Default constructor. */
  public BindingSCCs(BindingSCC head, BindingSCCs next) {
    this.head = head;
    this.next = next;
  }

  public static void display(String label, BindingSCCs sccs) {
    Log.println(label + ": [");
    for (; sccs != null; sccs = sccs.next) {
      BindingSCC scc = sccs.head;
      Log.print("  ");
      Log.print(scc.isRecursive() ? "rec" : "nonrec");
      Log.print(" {");
      String punc = "";
      for (Bindings bs = scc.getBindings(); bs != null; bs = bs.next) {
        Log.print(punc);
        punc = ", ";
        Log.print(bs.head.getId());
      }
      Log.println("}");
    }
    Log.println("]");
  }

  static TVarsInScope inferTypes(Handler handler, TVarsInScope tis, BindingSCCs sccs)
      throws Failure {
    for (; sccs != null; sccs = sccs.next) {
      tis = sccs.head.inferTypes(handler, tis);
    }
    return tis;
  }
}
