/*
    Copyright 2018-19 Mark P Jones, Portland State University

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
 * Src values correspond to points in a join lattice. The top element is represented by an object of
 * the Any class. All other lattice values are represented as joins of lists of pairs that we will
 * write in the form d.j, where d is a definition and j is a valid index for a parameter of d. The
 * bottom element of the lattice would be represented by the empty list, but we will never actually
 * use the bottom value because all of the lattice values in our calculations are initialized to
 * singleton joins and increase monotonically from there. A key property for these lattices is that
 * the join of two values with the same definition but different index values is equal to the top
 * value in the lattice. In symbols, this means that, if i/=j, then d.i `join` d.j == any.
 */
abstract class Src {

  public static final Src any = new Any();

  /**
   * Calculate the join of two Src values, neither of which is bottom. If either one could be "any"
   * value (i.e., the lattice top), then we return that. Otherwise we will use the joinJoin method
   * to do a pairwise merge of two join lists.
   */
  abstract Src join(Src r);

  /**
   * Worker function for join. Will either return the receiver for this method (i.e., the right
   * argument in the original call to join()), or else a new Src value (if the result is greater
   * than the receiver).
   */
  abstract Src joinJoin(Join js);

  /** Generate a printable description of this Src value (which should not be bottom/null). */
  public abstract String toString();

  abstract void updateSources(Defn b, int i, Defn d, int j);

  abstract Src propagate();

  boolean isInvariant() {
    return false;
  }
}
