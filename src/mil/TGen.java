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
 * TGen is used to represent a generic or bound variable; the associated int value is typically
 * interpreted as an index into an associated array of type parameters or instantiating arguments.
 */
public class TGen extends Type {

  private int n;

  /** Default constructor. */
  public TGen(int n) {
    this.n = n;
  }

  /**
   * Determine whether this TGen can be (or has already been) associated with the given
   * correspondence.
   */
  boolean mapsTo(TGen b, TGenCorresp corresp) {
    return corresp != null && corresp.maps(n, b);
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal, possibly with some correspondence between the TGen objects in
   * the two types. We use the names left and right to keep track of which types were on the left
   * and the right in the original alphaEquiv() call so that we can build the TGenCorresp in a
   * consistent manner.
   */
  boolean alphaType(Type left, TGenCorresp corresp) {
    return left.alphaTGen(this, corresp);
  }

  /** Test to determine whether this type is equal to a given TGen. */
  boolean alphaTGen(TGen right, TGenCorresp corresp) {
    return this.mapsTo(right, corresp);
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    if (args == 0) {
      tw.writeTGen(n);
    } else {
      applic(tw, prec, args, 0);
    }
  }

  /**
   * Package a type with an environment as a single value, using a TInd if the environment is not
   * empty.
   */
  Type with(Type[] tenv) {
    return tenv[n];
  }

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  TVars tvars(Type[] thisenv, TVars tvs) {
    // If thisenv is null, then this is a generic inside a type scheme
    // and it does not contain any TVars, unbound or otherwise!
    return (thisenv == null) ? tvs : thisenv[n].tvars(null, tvs);
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any other unbound TVars are kept as is.
   * All TInd and bound TVar nodes are eliminated in the process.
   */
  Type skeleton(Type[] thisenv, TVar[] generics) {
    return thisenv[this.n].skeleton(null, generics);
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return thisenv[n].same(null, t, tenv);
  }

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return thisenv[n].sameTAp(null, tap, tapenv);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    return thisenv[n].sameTTycon(null, that);
  }

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    return thisenv[n].sameTLit(null, t);
  }

  /**
   * Test to determine whether this type is equal to a specified type variable. NOTE: we assume here
   * that the specified TVar is unbound!
   */
  boolean sameTVar(Type[] thisenv, TVar v) {
    return thisenv[n].sameTVar(null, v);
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return thisenv[n].calcKind(null);
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
    return thisenv[n].match(null, t, tenv);
  }

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return thisenv[n].matchTAp(null, tap, tapenv);
  }

  /**
   * Test to determine whether the specified variable can be matched to this type. The only type
   * variable that can be instantiated during this process is the variable v passed in as the second
   * argument. The variable v must be unbound.
   */
  boolean matchTVar(Type[] thisenv, TVar v) {
    return thisenv[n].matchTVar(null, v);
  }

  boolean contains(Type[] thisenv, TVar v) {
    return thisenv[n].contains(null, v);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    thisenv[n].unify(null, t, tenv);
  }

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    thisenv[n].unifyTAp(null, tap, tapenv);
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    thisenv[n].unifyTTycon(null, that);
  }

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    thisenv[n].unifyTLit(null, t);
  }

  /**
   * Unify this type expression with a given (unbound) type variable. This typically just requires
   * binding the specified type variable, but we also need to indirect through TGen and TVar values.
   */
  void unifyTVar(Type[] thisenv, TVar v) throws UnifyException {
    thisenv[n].unifyTVar(null, v);
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    return tenv[n].simplifyNatType(null);
  }

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return tenv[n].tupleArity(null, n);
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return (env == null) ? set.canonOther(this, args) : env[n].canonType(null, set, args);
  }

  Type apply(Type[] thisenv, TVarSubst s) {
    return thisenv[n].apply(null, s);
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return (tenv == null) ? super.canonArgs(tenv, set, args) : tenv[n].canonArgs(null, set, args);
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
    return tenv[n].bitSize(null);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return tenv[n].bitSize(null, a.with(tenv));
  }

  public Pat bitPat(Type[] tenv) {
    return tenv[n].bitPat(null);
  }

  Pat bitPat(Type[] tenv, Type a) {
    return tenv[n].bitPat(null, a.with(tenv));
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    return tenv[n].byteSize(null);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return tenv[n].byteSize(null, a.with(tenv));
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return tenv[n].byteSize(null, a.with(tenv), b.with(tenv));
  }

  /** Determine if this is a type of the form (Ref a) or (Ptr a) for some area type a. */
  boolean referenceType(Type[] tenv) {
    return tenv[n].referenceType(null);
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return tenv[n].referenceType(null, a.with(tenv));
  }

  /** Return the alignment of this type (or zero if there is no alignment). */
  public long alignment(Type[] tenv) {
    return tenv[n].alignment(null);
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return tenv[n].alignment(null, a.with(tenv));
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return tenv[n].alignment(null, a.with(tenv), b.with(tenv));
  }

  boolean nonUnit(Type[] tenv) {
    return tenv[n].nonUnit(null);
  }
}
