/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
 * Represents a correspondence between the TGens in two skeletons. The order in which TGens are
 * listed in any given pair of skeletons may be different, but if we can establish a bijection
 * between the two sets by using an object of this class, then we can conclude that the types are
 * alpha equivalent.
 */
class TGenCorresp {

  private TGen[] gens = new TGen[4];

  /**
   * Will this correspondence allow a mapping from Type.gen(an) to b? This method answers that
   * question based on information previously gathered in this object, and by making updates to this
   * object if this particular call establishes a new correspondence.
   */
  boolean maps(int an, TGen b) {
    for (int i = 0; i < gens.length; i++) { // Is b already in the range of the current mapping?
      if (gens[i] == b) { // If so, then this maps call can only succeed if an
        return (i == an); // is already mapped to b.
      }
    }

    if (an >= gens.length) { // Expand mapping if necessary to include an
      TGen[] ngens = new TGen[Math.max(an + 1, 2 * gens.length)];
      for (int i = 0; i < gens.length; i++) { // (clearly an is not already mapped in this case)
        ngens[i] = gens[i];
      }
      gens = ngens;
    } else if (gens[an] != null) { // An existing mapping for an cannot be to b because we
      return false; // know that b is not in the current range
    }
    gens[an] = b; // Add mapping { a |-> b }
    return true;
  }
}
