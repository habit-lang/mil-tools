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

  /** Test to see if this type scheme is monomorphic. */
  public abstract Type isMonomorphic();

  /** Test to determine whether two type schemes are alpha equivalent. */
  public abstract boolean alphaEquiv(Scheme right);

  /**
   * Test to determine whether this type scheme is alpha equivalent to the given Forall type scheme.
   */
  boolean alphaForall(Forall left, TGenCorresp corresp) {
    return false;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal, possibly with some correspondence between the TGen objects in
   * the two types. We use the names left and right to keep track of which types were on the left
   * and the right in the original alphaEquiv() call so that we can build the TGenCorresp in a
   * consistent manner.
   */
  abstract boolean alphaType(Type left, TGenCorresp corresp);

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

  /**
   * Generate a block whose code implements an uncurried version of the TopLevel f, whose type is
   * the receiver. For this operation to succeed, the declared type must be a monomorphic type
   * matching the grammar: et ::= [d1,...dm] ->> [et] | [d1,...dm] ->> t where di, t are types and
   * we apply the first production as many times as possible. For example, if the declared type is
   * [[X,Y] ->> [[Z] ->> [R]]], then the generated block will have type [X,Y,Z] >>= [R] and body
   * b[x,y,z] = t <- f @ [x,y]; t @ [z].
   */
  Block liftToBlock0(Position pos, String id, TopLevel f) {
    return null;
  }
}
