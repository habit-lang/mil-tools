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
import compiler.Position;
import core.*;

/**
 * Base class for type schemes, which can either be a simple monomorphic type, or else include type
 * variables bound by a forall quantifier.
 */
public abstract class Scheme {

  /** Test to see if this type scheme is polymorphic. */
  public abstract boolean isQuantified();

  /** Test to determine whether two type schemes are alpha equivalent. */
  public abstract boolean alphaEquiv(Scheme s);

  /**
   * Test to determine whether this type scheme is alpha equivalent to the given Forall type scheme.
   */
  boolean alphaForall(Forall f) {
    return false;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal. (Assumes that TGen generics have been allocated in the same
   * order in both inputs.)
   */
  abstract boolean alphaType(Type that);

  /** Construct a printable representation of a type scheme. */
  public String toString() {
    return toString(TypeWriter.NEVER);
  }

  public abstract String toString(int prec);

  /**
   * Create a fresh instance of this type scheme, allocating an environment of new type variables as
   * necessary for Forall schemes.
   */
  public abstract Type instantiate();

  public abstract Type getType();

  /** Find the list of unbound type variables in this type scheme. */
  public TVars tvars() {
    return tvars(null);
  }

  /**
   * Find the list of unbound type variables in this type scheme using an accumulating parameter
   * tvs.
   */
  public abstract TVars tvars(TVars tvs);

  public abstract Scheme generalize(TVar[] generics);

  /**
   * Find the canonical version of a type with respect to the given TypeSet; this should only be
   * used with monomorphic types that do not contain any TGen values.
   */
  abstract Type canonType(TypeSet set);

  /** Calculate a new version of this type scheme with canonical components. */
  abstract Scheme canonScheme(TypeSet set);

  public abstract Type apply(TVarSubst s);

  /**
   * Extend a substitution by matching this (potentially polymorphic) Scheme against a monomorphic
   * instance.
   */
  public abstract TVarSubst specializingSubst(TVar[] generics, Type inst);

  /** Return the representation vector for values of this type. */
  abstract Type[] repCalc();

  /**
   * Generate a call to a new primitive, wrapped in an appropriate chain of closure definitions, if
   * this type can be derived from pt in the following grammar: pt ::= [d1,...,dn] ->> rt ; rt ::=
   * [pt] | [r1,...,rm] .
   */
  Tail generatePrim(Position pos, String id) {
    return null;
  }
}
