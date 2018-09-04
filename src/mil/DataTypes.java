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
public class DataTypes {

  public DataType head;

  public DataTypes next;

  /** Default constructor. */
  public DataTypes(DataType head, DataTypes next) {
    this.head = head;
    this.next = next;
  }

  /** Test for membership in a list. */
  public static boolean isIn(DataType val, DataTypes list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }
}
