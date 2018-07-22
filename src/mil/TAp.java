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
import core.*;
import obdd.Pat;

/**
 * TAp is used to represent the application of one type to another. For a TAp value to be valid, the
 * two types must have compatible kinds: the application of fun to arg is valid if fun :: k1 -> k2,
 * and arg :: k1 for some kinds k1 and k2, in which case the kind of the result is k2.
 */
public class TAp extends Type {

  private Type fun;

  private Type arg;

  /** Default constructor. */
  public TAp(Type fun, Type arg) {
    this.fun = fun;
    this.arg = arg;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal. (Assumes that TGen generics have been allocated in the same
   * order in both inputs.)
   */
  boolean alphaType(Type that) {
    return that.alphaTAp(this);
  }

  /** Test to determine whether this type is equal to a given type application. */
  boolean alphaTAp(TAp that) {
    return that.fun.alphaType(this.fun) && that.arg.alphaType(this.arg);
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    tw.push(arg);
    fun.write(tw, prec, args + 1);
  }

  public int findLevel() throws Failure {
    return Math.max(fun.findLevel(), arg.findLevel());
  }

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  TVars tvars(Type[] thisenv, TVars tvs) {
    return this.arg.tvars(thisenv, this.fun.tvars(thisenv, tvs));
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any Any other unbound TVars are kept as
   * is. All TInd and bound TVar nodes are eliminated in the process.
   */
  Type skeleton(Type[] thisenv, TVar[] generics) {
    return new TAp(this.fun.skeleton(thisenv, generics), this.arg.skeleton(thisenv, generics));
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return t.sameTAp(tenv, this, thisenv);
  }

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return tap.fun.same(tapenv, this.fun, thisenv) && tap.arg.same(tapenv, this.arg, thisenv);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    return that.sameTAp(null, this, thisenv);
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return fun.calcKind(thisenv).getRng();
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
    return t.matchTAp(tenv, this, thisenv);
  }

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return tap.fun.match(tapenv, this.fun, thisenv) && tap.arg.match(tapenv, this.arg, thisenv);
  }

  boolean contains(Type[] thisenv, TVar v) {
    return fun.contains(thisenv, v) || arg.contains(thisenv, v);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    t.unifyTAp(tenv, this, thisenv);
  }

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    this.fun.unify(thisenv, tap.fun, tapenv);
    this.arg.unify(thisenv, tap.arg, tapenv);
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    that.unifyTAp(null, this, thisenv);
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
    return fun.bitSize(tenv, arg);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return fun.bitSize(tenv, arg, a);
  }

  public Pat bitPat(Type[] tenv) {
    return fun.bitPat(tenv, arg);
  }

  Pat bitPat(Type[] tenv, Type a) {
    return fun.bitPat(tenv, arg, a);
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    return fun.byteSize(tenv, arg);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return fun.byteSize(tenv, arg, a);
  }

  Type byteSizeStoredRef(Type[] tenv) {
    return fun.byteSizeStoredRef(tenv, arg);
  }

  Type byteSizeStoredRef(Type[] tenv, Type a) {
    return fun.byteSizeStoredRef(tenv, arg, a);
  }

  /**
   * A worker function that traverses a tuple type and removes the components that are not marked in
   * usedArgs. We assume a very simple structure for the input type: a left-leaning spine of TAps
   * with a tuple type constructor at the head, and no TGen, TVar, or TInd nodes on the spine.
   */
  Type removeArgs(int numUsedArgs, boolean[] usedArgs, int i) {
    Type t = fun.removeArgs(numUsedArgs, usedArgs, --i);
    return usedArgs[i] ? new TAp(t, arg) : t;
  }

  /**
   * Find a canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    set.push(this.arg.canonType(env, set, 0));
    return fun.canonType(env, set, args + 1);
  }

  /**
   * Determine whether the arguments of this (canonical) type match the types on the top of the
   * stack.
   */
  boolean matches(TypeSet set, int n) {
    return n > 0 && this.arg == set.stackArg(n) && this.fun.matches(set, n - 1);
  }

  Type apply(Type[] thisenv, TVarSubst s) {
    return new TAp(fun.apply(thisenv, s), arg.apply(thisenv, s));
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    // Look for patterns:  Bit n,  Ix n,  ARef n a,  APtr n a, ...
    return fun.bitdataTyconRep(arg);
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return fun.bitdataTyconRep2(arg, a);
  }

  /**
   * Returns true if bitdata values of this type use the lo bits representation, or false for hi
   * bits. The result is meaningless if there is no bit size for values of this type.
   */
  boolean useBitdataLo() {
    return fun.useBitdataLo(arg);
  }

  boolean useBitdataLo(Type s) {
    return fun.useBitdataLo(arg, s);
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, TypeMap tm, int args) {
    tm.push(arg); // arg is already in canonical form
    return fun.toLLVMCalc(c, tm, args + 1); // traverse towards head
  }

  Type getArg() {
    return arg;
  }

  /** Calculate an array of llvm Types corresponding to the components of a given MIL Tuple type. */
  llvm.Type[] tupleToArray(TypeMap tm, int args) {
    llvm.Type[] tys = fun.tupleToArray(tm, ++args);
    tys[tys.length - args] = tm.toLLVM(arg);
    return tys;
  }

  /**
   * Calculate an array of formal argument types for a closure using a value of the specified ptr
   * type as the first argument and adding an extra argument for each component in this type, which
   * must be a tuple.
   */
  llvm.Type[] closureArgs(TypeMap tm, llvm.Type ptr, int args) {
    llvm.Type[] cargs = fun.closureArgs(tm, ptr, ++args);
    cargs[cargs.length - args] = tm.toLLVM(arg);
    return cargs;
  }
}
