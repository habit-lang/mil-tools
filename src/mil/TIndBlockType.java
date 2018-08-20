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
 * Represents a monomorphic block type in which any generic variables are interpreted using a lookup
 * in the specified type environment.
 */
class TIndBlockType extends BlockType {

  Type[] tenv;

  /** Default constructor. */
  TIndBlockType(Type dom, Type rng, Type[] tenv) {
    super(dom, rng);
    this.tenv = tenv;
  }

  void domUnifiesWith(Position pos, Type type) throws Failure {
    dom.unify(pos, tenv, type, null);
  }

  /** Return the domain type of this block type. */
  Type domType() {
    return dom.with(tenv);
  }

  /** Return the range type of this block type. */
  Type rngType() {
    return rng.with(tenv);
  }

  /** Return the arity (number of inputs) for this block type. */
  int getArity() {
    return dom.tupleArity(tenv, 0);
  }

  /** Return the outity (number of outputs) for this block type. */
  int getOutity() {
    return rng.tupleArity(tenv, 0);
  }

  TVars tvars(TVars tvs) {
    return rng.tvars(tenv, dom.tvars(tenv, tvs));
  }

  /**
   * Generalize this monomorphic BlockType to a polymorphic type using the specified list of generic
   * variables.
   */
  public BlockType generalize(TVar[] generics) {
    return generalize(generics, tenv);
  }

  /** Calculate a new version of this block type with canonical components. */
  BlockType canonBlockType(TypeSet set) {
    return new BlockType(dom.canonType(tenv, set), rng.canonType(tenv, set));
  }

  public BlockType apply(TVarSubst s) {
    return apply(tenv, s);
  }
}
