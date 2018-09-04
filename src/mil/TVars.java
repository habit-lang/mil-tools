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
public class TVars {

  public TVar head;

  public TVars next;

  /** Default constructor. */
  public TVars(TVar head, TVars next) {
    this.head = head;
    this.next = next;
  }

  /** Test for membership in a list. */
  public static boolean isIn(TVar val, TVars list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }

  /**
   * Produce a comma separated list of the type variables that are in extras but not gens, assuming
   * that extras was produced by one or more calls extending gens. Returns null if the lists are the
   * same.
   */
  public static String listAmbigTVars(TVars extras, TVars gens) {
    if (extras == gens) {
      return null;
    } else {
      StringBuilder buf = new StringBuilder(extras.head.skeleton().toString());
      while ((extras = extras.next) != gens) {
        buf.append(", ");
        buf.append(extras.head.skeleton().toString());
      }
      return buf.toString();
    }
  }
}
