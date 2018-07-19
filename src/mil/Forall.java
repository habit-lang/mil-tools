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

/** Represents a polymorphic type scheme that includes a quantifier prefix. */
public class Forall extends Scheme {

  private Prefix prefix;

  private Type type;

  /** Default constructor. */
  public Forall(Prefix prefix, Type type) {
    this.prefix = prefix;
    this.type = type;
  }

  /** Test to see if this type scheme is polymorphic. */
  public boolean isQuantified() {
    return true;
  }

  /** Test to determine whether two type schemes are alpha equivalent. */
  public boolean alphaEquiv(Scheme s) {
    return prefix.isEmpty() ? s.alphaType(this.type) : s.alphaForall(this);
  }

  /**
   * Test to determine whether this type scheme is alpha equivalent to the given Forall type scheme.
   */
  boolean alphaForall(Forall f) {
    return this.prefix.alphaPrefix(f.prefix) && this.type.alphaType(f.type);
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal. (Assumes that TGen generics have been allocated in the same
   * order in both inputs.)
   */
  boolean alphaType(Type that) {
    return prefix.isEmpty() && this.type.alphaType(that);
  }

  public String toString(int prec) {
    StringTypeWriter tw = new StringTypeWriter(prefix);
    tw.writeQuantifiers();
    type.write(tw, prec, 0);
    return tw.toString();
  }

  /**
   * Create a fresh instance of this type scheme, allocating an environment of new type variables as
   * necessary for Forall schemes.
   */
  public Type instantiate() {
    return (prefix.numGenerics() > 0) ? type.with(prefix.instantiate()) : type;
  }

  public Type getType() {
    debug.Internal.error("getType on type scheme");
    return null;
  }

  /**
   * Find the list of unbound type variables in this type scheme using an accumulating parameter
   * tvs.
   */
  public TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  public Scheme generalize(TVar[] generics) {
    return this;
  }

  /**
   * Find the canonical version of a type with respect to the given TypeSet; this should only be
   * used with monomorphic types that do not contain any TGen values.
   */
  Type canonType(TypeSet set) {
    debug.Internal.error("canon on Forall");
    return null;
  }

  /** Calculate a new version of this type scheme with canonical components. */
  Scheme canonScheme(TypeSet set) {
    Type t = instantiate().canonType(set);
    return t.generalize(TVar.generics(t.tvars(), null));
  }

  public Type apply(TVarSubst s) {
    debug.Internal.error("Unable to specialize type scheme");
    return null;
  }

  /**
   * Extend a substitution by matching this (potentially polymorphic) Scheme against a monomorphic
   * instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, Type inst) {
    Type[] tenv = prefix.instantiate();
    if (tenv.length != generics.length || !type.match(tenv, inst, null)) {
      debug.Internal.error("specializingSubst fails on Forall");
    }
    return TVarSubst.make(generics, tenv);
  }

  Type[] repCalc() {
    if (!prefix.isEmpty()) {
      debug.Internal.error("repCalc on quantified type scheme");
    }
    return type.repCalc();
  }
}
