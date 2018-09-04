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
package core;

import compiler.*;
import mil.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class CoreDefns {

  public CoreDefn head;

  public CoreDefns next;

  /** Default constructor. */
  public CoreDefns(CoreDefn head, CoreDefns next) {
    this.head = head;
    this.next = next;
  }

  /** Reverse a linked list of elements. */
  public static CoreDefns reverse(CoreDefns list) {
    CoreDefns result = null;
    while (list != null) {
      CoreDefns temp = list.next;
      list.next = result;
      result = list;
      list = temp;
    }
    return result;
  }

  /** Search for an occurrence of a particular identifier in a list of names. */
  public static CoreDefn find(String id, CoreDefns names) {
    for (; names != null; names = names.next) {
      if (names.head.answersTo(id)) {
        return names.head;
      }
    }
    return null;
  }

  /** Search for all occurrences of a particular identifier in a list of names. */
  public static CoreDefns findAll(String id, CoreDefns ns) {
    CoreDefns rs = null;
    for (; ns != null; ns = ns.next) {
      if (ns.head.answersTo(id)) {
        rs = new CoreDefns(ns.head, rs);
      }
    }
    return CoreDefns.reverse(rs);
  }

  /**
   * Depth-first search the forward dependency graph. Returns a list with the same Xs in reverse
   * order of finishing times. (In other words, the last node that we finish visiting will be the
   * first node in the result list.)
   */
  static CoreDefns searchForward(CoreDefns xs, CoreDefns result) {
    for (; xs != null; xs = xs.next) {
      result = xs.head.forwardVisit(result);
    }
    return result;
  }

  /**
   * Depth-first search the reverse dependency graph, using the list of bindings that was obtained
   * in the forward search, with the latest finishers first.
   */
  static CoreDefnSCCs searchReverse(CoreDefns xs) {
    CoreDefnSCCs sccs = null;
    for (; xs != null; xs = xs.next) {
      if (xs.head.getScc() == null) {
        CoreDefnSCC scc = new CoreDefnSCC();
        sccs = new CoreDefnSCCs(scc, sccs);
        xs.head.reverseVisit(scc);
      }
    }
    return sccs;
  }

  /**
   * Calculate the strongly connected components of a list of Xs that have been augmented with
   * dependency information.
   */
  public static CoreDefnSCCs scc(CoreDefns xs) {
    // Run the two depth-first searches of the main algorithm.
    return searchReverse(searchForward(xs, null));
  }

  /** Test for membership in a list. */
  public static boolean isIn(CoreDefn val, CoreDefns list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }
}
