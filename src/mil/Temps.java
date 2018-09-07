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

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class Temps {

  public Temp head;

  public Temps next;

  /** Default constructor. */
  public Temps(Temp head, Temps next) {
    this.head = head;
    this.next = next;
  }

  /** Test for membership in a list. */
  public static boolean isIn(Temp val, Temps list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }

  /** Return the length of a linked list of elements. */
  public static int length(Temps list) {
    int len = 0;
    for (; list != null; list = list.next) {
      len++;
    }
    return len;
  }

  /**
   * Push each of the variables in the given array, starting at index 0, on to the list of Temps,
   * and returning the new list as the result. There is no attempt to check for or prevent duplicate
   * occurrences.
   */
  static Temps push(Temp[] vs, Temps ts) {
    for (int i = 0; i < vs.length; i++) {
      ts = new Temps(vs[i], ts);
    }
    return ts;
  }

  public static String toString(Temps vs) {
    StringBuilder b = new StringBuilder("{");
    if (vs != null) {
      b.append(vs.head.toString());
      while ((vs = vs.next) != null) {
        b.append(",");
        b.append(vs.head.toString());
      }
    }
    b.append("}");
    return b.toString();
  }

  static Temp[] toArray(Temps vs) {
    Temp[] va = new Temp[Temps.length(vs)];
    for (int i = 0; vs != null; vs = vs.next) {
      va[i++] = vs.head;
    }
    return va;
  }

  /** Add a single element v to the list vs if it is not already included. */
  static Temps add(Temp v, Temps vs) {
    return Temps.isIn(v, vs) ? vs : new Temps(v, vs);
  }

  /** Add a list of elements us to the list vs. */
  static Temps add(Temps us, Temps vs) {
    for (; us != null; us = us.next) {
      vs = add(us.head, vs);
    }
    return vs;
  }

  public static Temps add(Atom[] args, Temps us) {
    for (int i = 0; i < args.length; i++) {
      us = args[i].add(us);
    }
    return us;
  }

  /** Destructively remove a single element v from the list vs. */
  static Temps remove(Temp v, Temps vs) {
    Temps prev = null;
    for (Temps us = vs; us != null; us = us.next) {
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
  static Temps remove(Temps us, Temps vs) {
    for (; us != null; us = us.next) {
      vs = remove(us.head, vs);
    }
    return vs;
  }

  public static Temps remove(Temp[] ts, Temps us) {
    for (int i = 0; i < ts.length; i++) {
      us = ts[i].removeFrom(us);
    }
    return us;
  }

  /**
   * Find the position of a specific variable in the given list, or return (-1) if the variable is
   * not found.
   */
  public static int lookup(Temp v, Temps vs) {
    for (int pos = 0; vs != null; vs = vs.next) {
      if (v == vs.head) {
        return pos;
      } else {
        pos++;
      }
    }
    return (-1);
  }
}
