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

class Any extends Src {

  /**
   * Calculate the join of two Src values, neither of which is bottom. If either one could be "any"
   * value (i.e., the lattice top), then we return that. Otherwise we will use the joinJoin method
   * to do a pairwise merge of two join lists.
   */
  Src join(Src r) {
    return this;
  }

  /**
   * Worker function for join. Will either return the receiver for this method (i.e., the right
   * argument in the original call to join()), or else a new Src value (if the result is greater
   * than the receiver).
   */
  Src joinJoin(Join js) {
    return this;
  }

  /** Generate a printable description of this Src value (which should not be bottom/null). */
  public String toString() {
    return "top";
  }

  void updateSources(Defn b, int i, Defn d, int j) {
    /* nothing to do */
  }

  Src propagate() {
    return this;
  }
}
