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
import compiler.Position;
import core.*;

/** Represents a named entity with a source code position and an identifying name. */
public abstract class Name {

  protected Position pos;

  protected String id;

  /** Default constructor. */
  public Name(Position pos, String id) {
    this.pos = pos;
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public Position getPos() {
    return pos;
  }

  /** Display the identifier for this name as its string representation. */
  public String toString() {
    return id;
  }

  /** Ask this name if it expects to be referred to by the specified string. */
  public boolean answersTo(String id) {
    return id.equals(this.id);
  }

  /**
   * Find the index for the name with the given label in the specified array, or return (-1) if
   * there is no matching element.
   */
  public static int index(String lab, Name[] names) {
    for (int i = 0; i < names.length; i++) {
      if (names[i].answersTo(lab)) {
        return i;
      }
    }
    return (-1);
  }
}
