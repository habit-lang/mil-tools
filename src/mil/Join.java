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

class Join extends Src {

  private Defn d;

  private int j;

  private Join next;

  /** Default constructor. */
  Join(Defn d, int j, Join next) {
    this.d = d;
    this.j = j;
    this.next = next;
  }

  /**
   * A utility method that scans a Join list js for an element of the form d.j and either returns
   * the corresponding value of j, or else signals that there is no such element by returning (-1).
   */
  static int find(Defn d, Join js) {
    for (; js != null; js = js.next) {
      if (js.d == d) {
        return js.j;
      }
    }
    return (-1);
  }

  /**
   * Calculate the join of two Src values, neither of which is bottom. If either one could be "any"
   * value (i.e., the lattice top), then we return that. Otherwise we will use the joinJoin method
   * to do a pairwise merge of two join lists.
   */
  Src join(Src r) {
    return r.joinJoin(this);
  }

  /**
   * Worker function for join. Will either return the receiver for this method (i.e., the right
   * argument in the original call to join()), or else a new Src value (if the result is greater
   * than the receiver).
   */
  Src joinJoin(Join js) {
    Join ks = this;
    do {
      Defn d = js.d;
      int j = js.j;
      int k = Join.find(d, ks); // Is there already a parameter from d in ks?
      if (k < 0) { // If not, then add this one
        ks = new Join(d, j, ks);
      } else if (k != j) { // If so, and if the index is different, then
        return Src.any; // we can potentially return "any" value
      }
    } while ((js = js.next) != null); // Otherwise, move on to the next item in js
    return ks;
  }

  /** Generate a printable description of this Src value (which should not be bottom/null). */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    append(buf);
    return buf.toString();
  }

  /**
   * Worker method for toString() on non-null Join values; adds description of join to specified
   * buffer.
   */
  void append(StringBuilder buf) {
    buf.append(d.toString());
    buf.append(".");
    buf.append(j);
    if (next != null) {
      buf.append(" || ");
      next.append(buf);
    }
  }

  void updateSources(Defn b, int i, Defn d, int j) {
    b.updateSources(i, d, j, this);
  }

  Src propagate() {
    Src src = this;
    Join js = this;
    do {
      src = js.d.propagate(js.j, src);
    } while ((js = js.next) != null);
    return src;
  }

  boolean isInvariant() {
    return true;
  }
}
