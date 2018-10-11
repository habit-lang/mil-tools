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
import obdd.Pat;

/**
 * A representation for type indirections, each packaging up a skeleton and an associated
 * environment.
 */
public class TInd extends Type {

  protected Type bound;

  protected Type[] boundenv;

  /** Default constructor. */
  public TInd(Type bound, Type[] boundenv) {
    this.bound = bound;
    this.boundenv = boundenv;
  }

  /**
   * Package a type with an environment as a single value, using a TInd if the environment is not
   * empty.
   */
  Type with(Type[] tenv) {
    return this;
  }

  void write(TypeWriter tw, int prec, int args) {
    bound.write(tw, prec, args);
  }

  boolean alphaType(Type t, TGenCorresp corresp) {
    debug.Internal.error("alphaType on TInd");
    return false;
  }

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  TVars tvars(Type[] thisenv, TVars tvs) {
    return bound.tvars(boundenv, tvs);
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any other unbound TVars are kept as is.
   * All TInd and bound TVar nodes are eliminated in the process.
   */
  Type skeleton(Type[] thisenv, TVar[] generics) {
    return bound.skeleton(boundenv, generics);
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return bound.same(boundenv, t, tenv);
  }

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return bound.sameTAp(boundenv, tap, tapenv);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    return bound.sameTTycon(boundenv, that);
  }

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    return bound.sameTLit(boundenv, t);
  }

  /**
   * Test to determine whether this type is equal to a specified type variable. NOTE: we assume here
   * that the specified TVar is unbound!
   */
  boolean sameTVar(Type[] thisenv, TVar v) {
    return bound.sameTVar(boundenv, v);
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return bound.calcKind(boundenv);
  }

  /**
   * Matching of types: test to see if the type on the right can be obtained by instantiating type
   * variables in the type on the left. (The "receiver", or "this", in the following code.)
   *
   * <p>match :: Type -> Env -> Type -> Env -> IO ()
   *
   * <p>Note that it is possible for a partial match to occur, meaning that some of the variables in
   * the receiver might be bound during the matching process, even if match returns false.
   */
  public boolean match(Type[] thisenv, Type t, Type[] tenv) {
    return bound.match(boundenv, t, tenv);
  }

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return bound.matchTAp(boundenv, tap, tapenv);
  }

  /**
   * Test to determine whether the specified variable can be matched to this type. The only type
   * variable that can be instantiated during this process is the variable v passed in as the second
   * argument. The variable v must be unbound.
   */
  boolean matchTVar(Type[] thisenv, TVar v) {
    return bound.matchTVar(boundenv, v);
  }

  boolean contains(Type[] thisenv, TVar v) {
    return bound.contains(boundenv, v);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    bound.unify(boundenv, t, tenv);
  }

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    bound.unifyTAp(boundenv, tap, tapenv);
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    bound.unifyTTycon(boundenv, that);
  }

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    bound.unifyTLit(boundenv, t);
  }

  /**
   * Unify this type expression with a given (unbound) type variable. This typically just requires
   * binding the specified type variable, but we also need to indirect through TGen and TVar values.
   */
  void unifyTVar(Type[] thisenv, TVar v) throws UnifyException {
    bound.unifyTVar(boundenv, v);
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    return bound.simplifyNatType(boundenv);
  }

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return bound.tupleArity(boundenv, n);
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return bound.canonType(boundenv, set, args);
  }

  Type apply(Type[] thisenv, TVarSubst s) {
    return bound.apply(boundenv, s);
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return bound.canonArgs(boundenv, set, args);
  }

  /**
   * Return the natural number type that specifies the BitSize of this type (required to be of kind
   * *) or null if this type has no BitSize (i.e., no bit-level representation). This method should
   * only be used with a limited collection of classes (we only expect to use it with top-level,
   * monomorphic types), but, just in case, we also provide implementations for classes that we do
   * not expect to see in practice, and allow for the possibility of a type environment, even though
   * we expect it will only ever be null.
   */
  public Type bitSize(Type[] tenv) {
    return bound.bitSize(boundenv);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return bound.bitSize(boundenv, a.with(tenv));
  }

  public Pat bitPat(Type[] tenv) {
    return bound.bitPat(boundenv);
  }

  Pat bitPat(Type[] tenv, Type a) {
    return bound.bitPat(boundenv, a.with(tenv));
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    return bound.byteSize(boundenv);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return bound.byteSize(boundenv, a.with(tenv));
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return bound.byteSize(boundenv, a.with(tenv), b.with(tenv));
  }

  /** Determine if this is a type of the form (Ref a) or (Ptr a) for some area type a. */
  boolean referenceType(Type[] tenv) {
    return bound.referenceType(boundenv);
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return bound.referenceType(boundenv, a.with(tenv));
  }

  /** Return the alignment of this type (or zero if there is no alignment). */
  public long alignment(Type[] tenv) {
    return bound.alignment(boundenv);
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return bound.alignment(boundenv, a.with(tenv));
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return bound.alignment(boundenv, a.with(tenv), b.with(tenv));
  }

  boolean nonUnit(Type[] tenv) {
    return bound.nonUnit(boundenv);
  }
}
