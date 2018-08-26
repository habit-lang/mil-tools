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

/**
 * Represents a polymorphic allocator type with some generic variables that are bound by the given
 * prefix.
 */
class PolyAllocType extends AllocType {

  private Prefix prefix;

  /** Default constructor. */
  PolyAllocType(Type[] stored, Type result, Prefix prefix) {
    super(stored, result);
    this.prefix = prefix;
  }

  /**
   * Construct a printable representation of a block type. This method is not intended for use with
   * TIndAllocTypes; that case is technically covered by the default definition for BlockType but
   * should not be expected to give useful results because it ignores the type environment.
   */
  public String toString() {
    return toString(prefix);
  }

  /**
   * Calculate an lc type scheme for a curried constructor function corresponding to this AllocType.
   * This method is only intended to be used on AllocTypes resulting from explicit declarations or
   * generalization (i.e., no TIndAllocType objects).
   */
  public Scheme toScheme() {
    return prefix.forall(toType());
  }

  /**
   * Instantiate this AllocType, creating a monomorphic instance (either an AllocType or a
   * TIndAllocType) in which any universally quantified variables bound to fresh type variables.
   */
  public AllocType instantiate() {
    return prefix.instantiateAllocType(stored, result);
  }

  /**
   * Generalize this monomorphic AllocType to a polymorphic type using the specified list of generic
   * variables.
   */
  public AllocType generalize(TVar[] generics) {
    // TODO: could omit this case; covered by AllocType branch.
    // Should not be called with a PolyAllocType ...
    debug.Internal.error("generalize on PolyAllocType");
    return null;
  }

  /**
   * Test to determine whether two allocator types are alpha equivalent. We assume that this method
   * will only be used with AllocType and PolyAllocType objects (i.e., not TIndAllocType) because
   * these are the only forms of AllocTypes that can be obtained as the result of generalization or
   * an explicity declared type.
   */
  public boolean alphaEquiv(AllocType at) {
    return at.alphaAllocType(this, prefix.isEmpty() ? null : new TGenCorresp());
  }

  /** Test to see if this allocator type is monomorphic. */
  public AllocType isMonomorphic() {
    return null;
  }

  /** Instantiate this AllocType, ensuring that the result is a newly allocated object. */
  AllocType freshInstantiate() {
    return instantiate();
  }

  /** Calculate a new version of this allocator type with canonical components. */
  AllocType canonAllocType(TypeSet set) {
    AllocType at = instantiate().canonAllocType(set);
    return at.generalize(TVar.generics(at.tvars(), null));
  }

  public AllocType apply(TVarSubst s) {
    debug.Internal.error("Unable to specialize polymorphic allocator type");
    return null;
  }

  /**
   * Extend a substitution by matching this (potentially polymorphic) AllocType against a
   * monomorphic instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, AllocType inst) {
    Type[] tenv = prefix.instantiate();
    if (tenv.length != generics.length || !this.match(tenv, inst, null)) {
      debug.Internal.error("specializingSubst fails on PolyAllocType");
    }
    return TVarSubst.make(generics, tenv);
  }
}
