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
import compiler.Handler;
import core.*;
import debug.Screen;
import mil.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class Bindings {

  public Binding head;

  public Bindings next;

  /** Default constructor. */
  public Bindings(Binding head, Bindings next) {
    this.head = head;
    this.next = next;
  }

  /** Reverse a linked list of elements. */
  public static Bindings reverse(Bindings list) {
    Bindings result = null;
    while (list != null) {
      Bindings temp = list.next;
      list.next = result;
      result = list;
      list = temp;
    }
    return result;
  }

  /** Search for an occurrence of a particular identifier in a list of names. */
  public static Binding find(String id, Bindings names) {
    for (; names != null; names = names.next) {
      if (names.head.answersTo(id)) {
        return names.head;
      }
    }
    return null;
  }

  /** Search for all occurrences of a particular identifier in a list of names. */
  public static Bindings findAll(String id, Bindings ns) {
    Bindings rs = null;
    for (; ns != null; ns = ns.next) {
      if (ns.head.answersTo(id)) {
        rs = new Bindings(ns.head, rs);
      }
    }
    return Bindings.reverse(rs);
  }

  /**
   * Depth-first search the forward dependency graph. Returns a list with the same Xs in reverse
   * order of finishing times. (In other words, the last node that we finish visiting will be the
   * first node in the result list.)
   */
  static Bindings searchForward(Bindings xs, Bindings result) {
    for (; xs != null; xs = xs.next) {
      result = xs.head.forwardVisit(result);
    }
    return result;
  }

  /**
   * Depth-first search the reverse dependency graph, using the list of bindings that was obtained
   * in the forward search, with the latest finishers first.
   */
  static BindingSCCs searchReverse(Bindings xs) {
    BindingSCCs sccs = null;
    for (; xs != null; xs = xs.next) {
      if (xs.head.getScc() == null) {
        BindingSCC scc = new BindingSCC();
        sccs = new BindingSCCs(scc, sccs);
        xs.head.reverseVisit(scc);
      }
    }
    return sccs;
  }

  /**
   * Calculate the strongly connected components of a list of Xs that have been augmented with
   * dependency information.
   */
  public static BindingSCCs scc(Bindings xs) {
    // Run the two depth-first searches of the main algorithm.
    return searchReverse(searchForward(xs, null));
  }

  /**
   * Test to see if any identifier names more than one X\Name in the specified list, reporting a
   * single error for each name that is used multiple times.
   */
  public static void checkForMultiple(Handler handler, Bindings ns) {
    for (Bindings reported = null; ns != null; ns = ns.next) {
      Bindings ms = Bindings.findAll(ns.head.getId(), ns.next);
      if (ms != null && Bindings.find(ns.head.getId(), reported) == null) {
        handler.report(new MultipleBindingDefnsFailure(new Bindings(ns.head, ms)));
        reported = new Bindings(ns.head, reported);
      }
    }
  }

  public static void display(Screen s, Bindings bindings) {
    int ind = s.getIndent();
    if (bindings != null) {
      bindings.head.display(s);
      while ((bindings = bindings.next) != null) {
        // s.print(";");
        // s.println();
        s.println();
        s.indent(ind);
        bindings.head.display(s);
      }
      s.indent(ind);
    }
  }

  static void display(Screen s, Bindings bindings, BindingSCCs bsccs) {
    if (bsccs == null) {
      Bindings.display(s, bindings);
    } else {
      int ind = s.getIndent();
      bsccs.head.display(s);
      while ((bsccs = bsccs.next) != null) {
        s.indent(ind);
        bsccs.head.display(s);
      }
    }
  }

  static void indent(IndentOutput out, int n, Bindings bindings) {
    out.indent(n, "Bindings");
    for (; bindings != null; bindings = bindings.next) {
      bindings.head.indent(out, n + 1);
    }
  }

  /** Test for membership in a list. */
  public static boolean isIn(Binding val, Bindings list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }
}
