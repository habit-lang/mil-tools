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
import compiler.BuiltinPosition;
import compiler.Failure;
import core.*;
import java.util.HashMap;

public class RepTypeSet extends TypeSet {

  /**
   * Return a canonical version of a type with a given head, h, and the specified number of
   * (canonical) arguments on the TypeSet stack. Overrides implementation in TypeSet, but defaults
   * back to that implementation (via super) if no change of representation is needed.
   */
  protected Type canon(Tycon h, int args) {
    int len = h.repTransform(this, args); // Rewrite a tuple type?
    return (len < 0)
        ? super.canon(h.canonTycon(this), args)
        : super.canonOther(TupleCon.tuple(len).asType(), len);
  }

  /**
   * Return the array of types that can be used to represent a tuple of values specified by the
   * array ts, each element of which uses the given tenv parameter to provide an interpretation for
   * TGen values. A null result indicates that no change of representation is required.
   */
  Type[] canonTypes(Type[] ts, Type[] tenv) {
    if (tenv != null) {
      debug.Internal.error("canon for RepTypeSet with tenv!=null");
    }

    Type[][] reps = null; // Look for changes in representation
    int len = 0;
    for (int i = 0; i < ts.length; i++) {
      Type[] r = ts[i].repCalc();
      if (r != null) {
        if (reps == null) {
          reps = new Type[ts.length][];
        }
        reps[i] = r;
        len += r.length;
      } else {
        len++;
      }
    }

    if (reps == null) { // No changes required
      return super.canonTypes(ts, tenv);
    }

    Type[] us = new Type[len]; // Rewrite stored type list
    int j = 0;
    for (int i = 0; i < ts.length; i++) {
      Type[] r = reps[i];
      if (r == null) {
        us[j++] = ts[i].canonType(tenv, this);
      } else {
        for (int k = 0; k < r.length; k++) {
          us[j++] = r[k].canonType(tenv, this);
        }
      }
    }
    return us;
  }

  /**
   * Stores a mapping from top-level definitions to the arrays of representation vectors produced by
   * the reps() method.
   */
  private HashMap<TopLevel, Type[][]> topLevelRepMap = new HashMap();

  /** Add an entry to the topLevelRepMap. */
  void putTopLevelReps(TopLevel tl, Type[][] reps) {
    topLevelRepMap.put(tl, reps);
  }

  /**
   * Return a vector of (zero or more) atoms that should replace a TopDef with the specified
   * topLevel and index i. This method should only be called in cases where we know that there is a
   * change of representation.
   */
  Atom[] topDef(TopLevel tl, int i) {
    Type[][] reps = topLevelRepMap.get(tl);
    if (reps == null) {
      return null;
    } else {
      int n = 0; // Find start position in new TopLevel definition:
      for (int j = 0; j < i; j++) {
        n += (reps[j] == null) ? 1 : reps[j].length;
      }

      int len =
          reps[i]
              .length; // Create an array with the new atoms to reference the ith component of tl:
      Atom[] as = new Atom[len];
      for (int j = 0; j < len; j++) {
        as[j] = new TopDef(tl, n + j);
      }
      return as;
    }
  }

  private Tails initializers = null;

  void addInitializer(Tail tail) {
    initializers = new Tails(tail, initializers);
  }

  Defn makeMain(Defn oldMain) throws Failure {
    String id;
    if (oldMain == null) {
      if (initializers == null) {
        return null;
      }
    } else {
      addInitializer(oldMain.makeTail());
    }
    Code code = new Done(initializers.head);
    while ((initializers = initializers.next) != null) {
      code = new Bind(new Temp(), initializers.head, code);
    }
    return new Block(BuiltinPosition.pos, "initialize", Temp.noTemps, code);
  }
}
