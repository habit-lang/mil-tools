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
import java.io.PrintWriter;
import java.util.HashMap;

class MergeMap extends TypeSet {

  /**
   * Records mappings that are confirmed as valid. We use Tycon as the key type here so that we can
   * perform lookups for arbitrary type constructors. However, in the methods that add items to
   * these mappings, we will only use DataType values as keys (and values). As a result, any lookup
   * for a Key that is not a DataType will automatically return null.
   */
  private HashMap<Tycon, DataType> confirmed = new HashMap();

  /** Records mappings that are assumed. */
  private HashMap<Tycon, DataType> assumed = new HashMap();

  /** Clear the current set of assumptions. */
  void clearAssumed() {
    assumed.clear();
  }

  /** Move all assumptions to the set of confirmed mappings. */
  void confirmAssumed() {
    confirmed.putAll(assumed);
    for (Tycon tc : assumed.keySet()) {
      debug.Log.println("Equating datatypes " + tc + " and " + assumed.get(tc));
    }
  }

  /** Write a description of this TypeSet to a PrintWriter. */
  public void dump(PrintWriter out) {
    out.println("Equated types: --------------------------");
    for (Tycon tc : confirmed.keySet()) {
      out.println(tc + " ---> " + confirmed.get(tc));
      Cfun[] cs = ((DataName) tc).getCfuns();
      Cfun[] ds = confirmed.get(tc).getCfuns();
      for (int i = 0; i < cs.length; i++) {
        System.out.println("    " + cs[i] + " ---> " + ds[i]);
      }
    }
    super.dump(out);
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
  DataType mappingFor(Tycon tc) {
    DataType dt;
    return ((dt = confirmed.get(tc)) != null || (dt = assumed.get(tc)) != null) ? lookup(dt) : null;
  }

  /**
   * Override the TypeSet method for calculating canonical versions of a type with a Tycon at its
   * head. By overriding this method, we are able to check for situations where the head is a
   * DataType that is being merged with another DataType, and then make an appropriate substitution
   * in all of the types where that is required.
   */
  protected Type canon(Tycon h, int args) {
    if (args == 0) { // Only nullary types are considered
      DataType dt = mappingFor(h); // Is h to be merged with some given DataType dt?
      if (dt != null) {
        h = dt; // Use the new DataType (but still require its canonical version)Cf
      }
    }
    return super.canon(h, args); // Or fall back to the canonical representation
  }
}
