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
import java.math.BigInteger;
import obdd.Pat;

/**
 * A representation for type variables, including a Tyvar to specify how an initially unbound type
 * indirection can be instantiated.
 */
public final class TVar extends TInd {

  private Tyvar tyvar;

  /** Default constructor. */
  public TVar(Type bound, Type[] boundenv, Tyvar tyvar) {
    super(bound, boundenv);
    this.tyvar = tyvar;

    this.num = count++;
  }

  private static int count = 0;

  private int num;

  public TVar(Tyvar tyvar) {
    this(null, null, tyvar);
  }

  /** Return the Tyvar (name and kind information) for this particular type variable. */
  public Tyvar getTyvar() {
    return tyvar;
  }

  void write(TypeWriter tw, int prec, int args) {
    tw.write("?" + num);
  }

  boolean alphaType(Type t, TGenCorresp corresp) {
    if (bound != null) {
      debug.Internal.error("alphaType on bound TVar");
    }
    return this == t;
  }

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  TVars tvars(Type[] thisenv, TVars tvs) {
    return (bound != null)
        ? bound.tvars(boundenv, tvs)
        : TVars.isIn(this, tvs) ? tvs : new TVars(this, tvs);
  }

  public static final TVar[] noTVars = new TVar[0];

  public static TVar[] generics(TVars gens, TVars fixed) {
    // Count generic variables:
    int n = 0;
    for (TVars tvs = gens; tvs != fixed; tvs = tvs.next) {
      n++;
    }

    // Make an array of generics:
    TVar[] generics = new TVar[n];
    for (int i = n; gens != fixed; gens = gens.next) {
      generics[--i] = gens.head;
    }
    return generics;
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any other unbound TVars are kept as is.
   * All TInd and bound TVar nodes are eliminated in the process.
   */
  Type skeleton(Type[] thisenv, TVar[] generics) {
    if (bound != null) {
      return bound.skeleton(boundenv, generics);
    } else {
      for (int i = 0; i < generics.length; i++) {
        if (generics[i] == this) {
          return Type.gen(i);
        }
      }
      return this;
    }
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return (bound == null) ? t.sameTVar(tenv, this) : bound.same(boundenv, t, tenv);
  }

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return (bound != null) && bound.sameTAp(boundenv, tap, tapenv);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    return (bound != null) && bound.sameTTycon(boundenv, that);
  }

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    return (bound != null) && bound.sameTLit(boundenv, t);
  }

  /**
   * Test to determine whether this type is equal to a specified type variable. NOTE: we assume here
   * that the specified TVar is unbound!
   */
  boolean sameTVar(Type[] thisenv, TVar v) {
    return (bound == null) ? (this == v) : bound.sameTVar(boundenv, v);
  }

  boolean matchBind(Type t, Type[] tenv) {
    if (t.calcKind(tenv).same(tyvar.getKind())) {
      bound = t;
      boundenv = tenv;
      return true;
    }
    return false;
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return tyvar.getKind();
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
    return (bound == null) ? t.matchTVar(tenv, this) : bound.same(boundenv, t, tenv);
  }

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return (bound != null) && bound.matchTAp(boundenv, tap, tapenv);
  }

  /**
   * Test to determine whether the specified variable can be matched to this type. The only type
   * variable that can be instantiated during this process is the variable v passed in as the second
   * argument. The variable v must be unbound.
   */
  boolean matchTVar(Type[] thisenv, TVar v) {
    return (bound == null) ? (this == v || v.matchBind(this, null)) : bound.matchTVar(boundenv, v);
  }

  void unifyBind(Type t, Type[] tenv) throws UnifyException {
    // ASSERT: bound==null, boundenv==null
    // ASSERT: if (t,tenv) is a TVar, then it is not bound
    if (this != t) {
      if (t.contains(tenv, this)) {
        throw new OccursCheckException(this, t, tenv);
      } else if (!t.calcKind(tenv).same(tyvar.getKind())) {
        throw new KindMismatchException(tyvar.getKind(), t, tenv);
      } else {
        bound = t;
        boundenv = tenv;
      }
    }
  }

  boolean contains(Type[] thisenv, TVar v) {
    return bound != null ? bound.contains(boundenv, v) : (v == this);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    if (bound == null) {
      t.unifyTVar(tenv, this);
    } else {
      bound.unify(boundenv, t, tenv);
    }
  }

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    if (bound == null) {
      this.unifyBind(tap, tapenv);
    } else {
      bound.unifyTAp(boundenv, tap, tapenv);
    }
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    if (bound == null) {
      this.unifyBind(that, null);
    } else {
      bound.unifyTTycon(boundenv, that);
    }
  }

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    if (bound == null) {
      this.unifyBind(t, null);
    } else {
      bound.unifyTLit(boundenv, t);
    }
  }

  /**
   * Unify this type expression with a given (unbound) type variable. This typically just requires
   * binding the specified type variable, but we also need to indirect through TGen and TVar values.
   */
  void unifyTVar(Type[] thisenv, TVar v) throws UnifyException {
    if (bound == null) {
      v.unifyBind(this, null);
    } else {
      bound.unifyTVar(boundenv, v);
    }
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    return (bound == null) ? this : bound.simplifyNatType(boundenv);
  }

  /** Bind a type variable of kind nat to a specific natural number value. */
  void bindNat(BigInteger n) {
    if (bound == null) {
      bound = new TNat(n);
    } else super.bindNat(n);
  }

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return (bound == null) ? (-1) : bound.tupleArity(boundenv, n);
  }

  /**
   * Generate a printable description for an array of type variables, such as the type variables
   * that are implicitly bound by a "big lambda" in a polymorphic definition.
   */
  public static String show(TVar[] tvs) {
    StringBuilder buf = new StringBuilder("[");
    if (tvs != null && tvs.length > 0) {
      buf.append(tvs[0].toString());
      for (int i = 1; i < tvs.length; i++) {
        buf.append(", ");
        buf.append(tvs[i].toString());
      }
    }
    buf.append("]");
    return buf.toString();
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return (bound == null) ? set.canonOther(this, args) : bound.canonType(boundenv, set, args);
  }

  Type apply(Type[] thisenv, TVarSubst s) {
    return (bound == null) ? s.find(this) : bound.apply(boundenv, s);
  }

  Type removeTVar() {
    if (bound == null) {
      debug.Internal.error("removeTVar: variable has not been bound");
    } else if (boundenv != null) {
      debug.Internal.error("removeTVar: non empty environment");
    }
    return bound;
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return (bound == null)
        ? super.canonArgs(tenv, set, args)
        : bound.canonArgs(boundenv, set, args);
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
    return (bound == null) ? this : bound.bitSize(boundenv);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return (bound == null) ? null : bound.bitSize(boundenv, a.with(tenv));
  }

  public Pat bitPat(Type[] tenv) {
    return (bound == null) ? null : bound.bitPat(boundenv);
  }

  Pat bitPat(Type[] tenv, Type a) {
    return (bound == null) ? null : bound.bitPat(boundenv, a.with(tenv));
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    return (bound == null) ? this : bound.byteSize(boundenv);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return (bound == null) ? null : bound.byteSize(boundenv, a.with(tenv));
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return (bound == null) ? null : bound.byteSize(boundenv, a.with(tenv), b.with(tenv));
  }

  /** Determine if this is a type of the form (Ref a) or (Ptr a) for some area type a. */
  boolean referenceType(Type[] tenv) {
    return (bound != null) && bound.referenceType(boundenv);
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return (bound != null) && bound.referenceType(boundenv, a.with(tenv));
  }

  /** Return the alignment of this type (or zero if there is no alignment). */
  public long alignment(Type[] tenv) {
    return (bound == null) ? 0 : bound.alignment(boundenv);
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return (bound == null) ? 0 : bound.alignment(boundenv, a.with(tenv));
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return (bound == null) ? 0 : bound.alignment(boundenv, a.with(tenv), b.with(tenv));
  }

  boolean nonUnit(Type[] tenv) {
    if (bound == null) {
      debug.Internal.error("nonUnit for unbound TVar");
    }
    return bound.nonUnit(boundenv);
  }
}
