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
public class Tycons {

  public Tycon head;

  public Tycons next;

  /** Default constructor. */
  public Tycons(Tycon head, Tycons next) {
    this.head = head;
    this.next = next;
  }

  /** Reverse a linked list of elements. */
  public static Tycons reverse(Tycons list) {
    Tycons result = null;
    while (list != null) {
      Tycons temp = list.next;
      list.next = result;
      result = list;
      list = temp;
    }
    return result;
  }

  /** Search for an occurrence of a particular identifier in a list of names. */
  public static Tycon find(String id, Tycons names) {
    for (; names != null; names = names.next) {
      if (names.head.answersTo(id)) {
        return names.head;
      }
    }
    return null;
  }

  /** Search for all occurrences of a particular identifier in a list of names. */
  public static Tycons findAll(String id, Tycons ns) {
    Tycons rs = null;
    for (; ns != null; ns = ns.next) {
      if (ns.head.answersTo(id)) {
        rs = new Tycons(ns.head, rs);
      }
    }
    return Tycons.reverse(rs);
  }

  /**
   * Test to see if any identifier names more than one X\Name in the specified list, reporting a
   * single error for each name that is used multiple times.
   */
  public static void checkForMultiple(Handler handler, Tycons ns) {
    for (Tycons reported = null; ns != null; ns = ns.next) {
      Tycons ms = Tycons.findAll(ns.head.getId(), ns.next);
      if (ms != null && Tycons.find(ns.head.getId(), reported) == null) {
        handler.report(new MultipleTyconDefnsFailure(new Tycons(ns.head, ms)));
        reported = new Tycons(ns.head, reported);
      }
    }
  }
}
