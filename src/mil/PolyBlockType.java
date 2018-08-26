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
 * Represents a polymorphic block type with some generic variables that are bound by the given
 * prefix.
 */
class PolyBlockType extends BlockType {

  Prefix prefix;

  /** Default constructor. */
  PolyBlockType(Type dom, Type rng, Prefix prefix) {
    super(dom, rng);
    this.prefix = prefix;
  }

  /**
   * Construct a printable representation of this BlockType. This method is not intended for use
   * with TIndBlockTypes; that case is technically covered by the default definition for BlockType
   * but it will probably not produce the right output because it ignores the type environment.
   */
  public String toString() {
    return toString(prefix);
  }

  /**
   * Instantiate this BlockType, creating a monomorphic instance (either a BlockType or a
   * TIndBlockType) in which any universally quantified variables bound to fresh type variables.
   */
  public BlockType instantiate() {
    return prefix.instantiateBlockType(dom, rng);
  }

  /**
   * Generalize this monomorphic BlockType to a polymorphic type using the specified list of generic
   * variables.
   */
  public BlockType generalize(TVar[] generics) {
    // TODO: could omit this case; covered by BlockType branch.  Should not be called with a
    // PolyBlockType ...
    debug.Internal.error("generalize on PolyBlockType");
    return null;
  }

  /**
   * Test to determine whether two block types are alpha equivalent. We assume that this method will
   * only be used with BlockType and PolyBlockType objects (i.e., not TIndBlockType) because these
   * are the only forms of BlockTypes that can be obtained as the result of generalization or an
   * explicitly declared type.
   */
  public boolean alphaEquiv(BlockType bt) {
    return bt.alphaBlockType(this, prefix.isEmpty() ? null : new TGenCorresp());
  }

  /** Test to see if this block type is monomorphic. */
  public BlockType isMonomorphic() {
    return null;
  }

  /**
   * Instantiate this BlockType, ensuring that the result is a newly allocated object (that can
   * therefore be modified by side effecting operations without modifying the original).
   */
  BlockType freshInstantiate() {
    return instantiate();
  }

  /** Calculate a new version of this block type with canonical components. */
  BlockType canonBlockType(TypeSet set) {
    BlockType bt = instantiate().canonBlockType(set);
    return bt.generalize(TVar.generics(bt.tvars(), null));
  }

  public BlockType apply(TVarSubst s) {
    debug.Internal.error("Unable to specialize polymorphic block type");
    return null;
  }

  /**
   * Extend a substitution by matching this (potentially polymorphic) BlockType against a
   * monomorphic instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, BlockType inst) {
    Type[] tenv = prefix.instantiate();
    if (tenv.length != generics.length || !this.match(tenv, inst, null)) {
      debug.Internal.error("specializingSubst fails on PolyBlockType");
    }
    return TVarSubst.make(generics, tenv);
  }
}
