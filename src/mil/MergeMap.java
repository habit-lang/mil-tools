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
import java.util.HashMap;

class MergeMap extends TypeSet {

  /**
   * Records mappings that are confirmed as valid. We use DataName as the key type because every
   * Cfun has an associated DataName, and we need this when remapping constructor functions.
   * However, the methods that we use to add entries to the mapping will only allow DataType values.
   */
  private HashMap<DataName, DataType> confirmed = new HashMap();

  /** Records mappings that are assumed. */
  private HashMap<DataName, DataType> assumed = new HashMap();

  /** Clear the current set of assumptions. */
  void clearAssumed() {
    assumed.clear();
  }

  /** Move all assumptions to the set of confirmed mappings. */
  void confirmAssumed() {
    confirmed.putAll(assumed);
    for (DataName dn : assumed.keySet()) {
      debug.Log.println("Equating datatypes " + dn + " and " + assumed.get(dn));
    }
  }

  /** Add a mapping to the current set of assumptions. */
  void assume(DataType dt, DataType ndt) {
    assumed.put(dt, ndt);
  }

  /** Find the current representative for a given Datatype under this mapping. */
  DataType lookup(DataType dt) {
    DataType next;
    while ((next = confirmed.get(dt)) != null || (next = assumed.get(dt)) != null) {
      dt = next;
    }
    return dt;
  }

  /**
   * Test to see if the given DataName should be replaced with another type under this mapping,
   * returning either the new DataType or else null, indicating that the DataName is no remapped.
   */
  DataType mappingFor(DataName dn) {
    DataType dt;
    return ((dt = confirmed.get(dn)) != null || (dt = assumed.get(dn)) != null) ? lookup(dt) : null;
  }
}
