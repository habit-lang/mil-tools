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
import compiler.Failure;
import compiler.Position;
import core.*;

/**
 * Represents a monomorphic allocator type in which any generic variables are interpreted using a
 * lookup in the specified type environment.
 */
class TIndAllocType extends AllocType {

  private Type[] tenv;

  /** Default constructor. */
  TIndAllocType(Type[] stored, Type result, Type[] tenv) {
    super(stored, result);
    this.tenv = tenv;
  }

  /** Return a stored type component for this AllocType. */
  Type storedType(int i) {
    return stored[i].with(tenv);
  }

  /** Return the result type for this AllocType. */
  Type resultType() {
    return result.with(tenv);
  }

  void storedUnifiesWith(Position pos, Type[] inputs) throws Failure {
    storedUnifiesWith(pos, tenv, inputs);
  }

  void resultUnifiesWith(Position pos, Type type) throws Failure {
    result.unify(pos, tenv, type, null);
  }

  TVars tvars(TVars tvs) {
    return tvars(tenv, tvs);
  }

  /**
   * Generalize this monomorphic AllocType to a polymorphic type using the specified list of generic
   * variables.
   */
  public AllocType generalize(TVar[] generics) {
    return generalize(generics, tenv);
  }

  /** Calculate a new version of this allocator type with canonical components. */
  AllocType canonAllocType(TypeSet set) {
    return new AllocType(set.canonTypes(stored, tenv), result.canonType(tenv, set));
  }

  public AllocType apply(TVarSubst s) {
    return apply(tenv, s);
  }

  boolean resultMatches(Type inst) {
    return result.match(tenv, inst, null);
  }
}
