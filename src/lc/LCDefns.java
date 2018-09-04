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
import mil.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class LCDefns {

  public LCDefn head;

  public LCDefns next;

  /** Default constructor. */
  public LCDefns(LCDefn head, LCDefns next) {
    this.head = head;
    this.next = next;
  }

  /**
   * Convert a list of LCDefn values in to a list of Bindings. This process, for example, attaches
   * types that are declared in type annotations with the corresponding identifier definition. We
   * implement this algorithm using two passes over the list of LCDefn values: the first builds up a
   * list of all bindings, and the second adds annotations. This is followed by a final pass over
   * the generated list of bindings to check for duplicate entries.
   */
  public static Bindings toBindings(Handler handler, TyconEnv tenv, LCDefns defns) {
    Bindings bs = null;
    for (LCDefns ds = defns; ds != null; ds = ds.next) {
      bs = ds.head.addBindings(handler, bs);
    }
    for (LCDefns ds = defns; ds != null; ds = ds.next) {
      ds.head.annotateBindings(handler, tenv, bs);
    }
    Bindings.checkForMultiple(handler, bs);
    return bs;
  }

  static void indent(IndentOutput out, int n, LCDefns defns) {
    out.indent(n, "LCDefns");
    for (; defns != null; defns = defns.next) {
      defns.head.indent(out, n + 1);
    }
  }
}
