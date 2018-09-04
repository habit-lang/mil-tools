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
import compiler.Handler;
import core.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class Tyvars {

  public Tyvar head;

  public Tyvars next;

  /** Default constructor. */
  public Tyvars(Tyvar head, Tyvars next) {
    this.head = head;
    this.next = next;
  }

  /** Reverse a linked list of elements. */
  public static Tyvars reverse(Tyvars list) {
    Tyvars result = null;
    while (list != null) {
      Tyvars temp = list.next;
      list.next = result;
      result = list;
      list = temp;
    }
    return result;
  }

  /** Search for an occurrence of a particular identifier in a list of names. */
  public static Tyvar find(String id, Tyvars names) {
    for (; names != null; names = names.next) {
      if (names.head.answersTo(id)) {
        return names.head;
      }
    }
    return null;
  }

  /** Search for all occurrences of a particular identifier in a list of names. */
  public static Tyvars findAll(String id, Tyvars ns) {
    Tyvars rs = null;
    for (; ns != null; ns = ns.next) {
      if (ns.head.answersTo(id)) {
        rs = new Tyvars(ns.head, rs);
      }
    }
    return Tyvars.reverse(rs);
  }

  /**
   * Test to see if any identifier names more than one X\Name in the specified list, reporting a
   * single error for each name that is used multiple times.
   */
  public static void checkForMultiple(Handler handler, Tyvars ns) {
    for (Tyvars reported = null; ns != null; ns = ns.next) {
      Tyvars ms = Tyvars.findAll(ns.head.getId(), ns.next);
      if (ms != null && Tyvars.find(ns.head.getId(), reported) == null) {
        handler.report(new MultipleTyvarDefnsFailure(new Tyvars(ns.head, ms)));
        reported = new Tyvars(ns.head, reported);
      }
    }
  }
}
