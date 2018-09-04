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
public class DefVars {

  public DefVar head;

  public DefVars next;

  /** Default constructor. */
  public DefVars(DefVar head, DefVars next) {
    this.head = head;
    this.next = next;
  }

  /** Test for membership in a list. */
  public static boolean isIn(DefVar val, DefVars list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }

  /** Return the length of a linked list of elements. */
  public static int length(DefVars list) {
    int len = 0;
    for (; list != null; list = list.next) {
      len++;
    }
    return len;
  }

  /** Add a single element v to the list vs if it is not already included. */
  static DefVars add(DefVar v, DefVars vs) {
    return DefVars.isIn(v, vs) ? vs : new DefVars(v, vs);
  }

  /** Add a list of elements us to the list vs. */
  static DefVars add(DefVars us, DefVars vs) {
    for (; us != null; us = us.next) {
      vs = add(us.head, vs);
    }
    return vs;
  }

  /** Add an array of elements us to the list vs. */
  static DefVars add(DefVar[] us, DefVars vs) {
    for (int i = 0; i < us.length; i++) {
      vs = add(us[i], vs);
    }
    return vs;
  }

  /** Destructively remove a single element v from the list vs. */
  static DefVars remove(DefVar v, DefVars vs) {
    DefVars prev = null;
    for (DefVars us = vs; us != null; us = us.next) {
      if (us.head == v) {
        if (prev == null) {
          return us.next; // remove first element
        } else {
          prev.next = us.next; // remove later element
          return vs;
        }
      }
      prev = us;
    }
    return vs; // v not found
  }

  /** Destructively remove a list of elements us from the list vs. */
  static DefVars remove(DefVars us, DefVars vs) {
    for (; us != null; us = us.next) {
      vs = remove(us.head, vs);
    }
    return vs;
  }

  /** Destructively remove an array of elements us from the list vs. */
  static DefVars remove(DefVar[] us, DefVars vs) {
    for (int i = 0; i < us.length; i++) {
      vs = remove(us[i], vs);
    }
    return vs;
  }

  static DefVar[] toArray(DefVars vs) {
    DefVar[] va = new DefVar[DefVars.length(vs)];
    for (int i = 0; vs != null; vs = vs.next) {
      va[i++] = vs.head;
    }
    return va;
  }
}
