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

class BitdataMap extends TypeSet {

  BitdataRep findRep(DataType dt) {
    DataName ndn = getDataName(dt);
    return (ndn == null) ? null : ndn.isBitdataRep();
  }

  /**
   * Rewrite this program to use bitdata types in place of any of the algebraic types, passed in as
   * candidates, that can be given a suitable bitdata representation.
   */
  int addMappings(DataTypes cands) {
    int total = 0; // Track the total number of mappings created during this call
    if (cands != null) { // If there are no candidates, then there is no need for a rewrite
      int added;
      do { // Invariant: we only start this loop if cands!=null
        DataTypes nextpass = null; // The list of candidates retained for the next pass
        int retained = 0;
        added = 0;
        do {
          DataTypes next = cands.next;
          int act = cands.head.bitdataEncoding(this, next, nextpass);
          if (act > 0) {
            added++;
          } else if (act == 0) {
            debug.Log.println("retaining " + cands.head + " as a bitdata candidate");
            cands.next = nextpass;
            nextpass = cands;
            retained++;
          }
          cands = next;
        } while (cands != null);
        cands = nextpass; // Set the candidates for the next pass (if there is one)
        total += added;
        debug.Log.println(
            "added " + added + " mappings on this pass, " + retained + " candidates retained");
      } while (added > 0
          && cands
              != null); // Keep going if the map grew on the last past and there are still
                        // candidates
      debug.Log.println("synthesized " + total + " data to bitdata type rewrites");
    }
    return total;
  }

  /**
   * Override the TypeSet method for calculating canonical versions of a type with a Tycon at its
   * head. By overriding this method, we are able to check for situations where the head is a
   * DataType that is being replaced by a BitdataType and then make an appropriate substitution of
   * names in all of the types where that is required.
   */
  protected Type canon(Tycon h, int args) {
    if (args == 0) { // Only nullary types are considered
      BitdataRep r = h.findRep(this); // Is a change of data representation required?
      if (r != null) {
        return r.asType(); // Use the BitdataRep as the new type
      }
    }
    return super.canon(h, args); // Or fall back to the canonical representation
  }
}
