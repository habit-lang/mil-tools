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
import java.math.BigInteger;
import obdd.Pat;

/** Represents a monomorphic type (skeleton). */
public abstract class Type extends Scheme {

  /** An empty array of types. */
  public static final Type[] noTypes = new Type[0];

  /** Test to see if this type scheme is polymorphic. */
  public boolean isQuantified() {
    return false;
  }

  private static TGen[] genCache = new TGen[10];

  public static TGen gen(int n) {
    if (n >= genCache.length) {
      TGen[] newCache = new TGen[Math.max(n + 1, 2 * genCache.length)];
      for (int i = 0; i < genCache.length; i++) {
        newCache[i] = genCache[i];
      }
      genCache = newCache;
    } else if (genCache[n] != null) {
      return genCache[n];
    }
    // Code to update cache[arg] = ... will be appended here.
    return genCache[n] = new TGen(n);
  }

  /** Test to determine whether two type schemes are alpha equivalent. */
  public boolean alphaEquiv(Scheme s) {
    return s.alphaType(this);
  }

  /** Test to determine whether this type is equal to a given TGen. */
  boolean alphaTGen(TGen that) {
    return false;
  }

  /** Test to determine whether this type is equal to a given type application. */
  boolean alphaTAp(TAp that) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TTycon. */
  boolean alphaTTycon(TTycon that) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TNat. */
  boolean alphaTNat(TNat that) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TLab. */
  boolean alphaTLab(TLab that) {
    return false;
  }

  /** Write a printable version of this type to the specified @TypeWriter@. */
  void write(TypeWriter tw) {
    write(tw, TypeWriter.NEVER, 0);
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  abstract void write(TypeWriter tw, int prec, int args);

  /**
   * Print a type expression in applicative syntax; one parameter specifies the number of arguments
   * that should be used to print the head, while another specifies the total number of arguments
   * (including those for the head, thus args>use is required).
   */
  void applic(TypeWriter tw, int prec, int args, int use) {
    tw.open(prec >= TypeWriter.ALWAYS);
    write(tw, TypeWriter.ALWAYS, use);
    for (int i = use; i < args; i++) {
      tw.write(" ");
      tw.pop().write(tw, TypeWriter.ALWAYS, 0);
    }
    tw.close(prec >= TypeWriter.ALWAYS);
  }

  public String toString(int prec) {
    StringTypeWriter tw = new StringTypeWriter(new Prefix());
    this.write(tw, prec, 0);
    return tw.toString();
  }

  static String toString(Type[] ts) {
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    for (int i = 0; i < ts.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append(ts[i].toString());
    }
    buf.append("]");
    return buf.toString();
  }

  public int findLevel() throws Failure {
    return 0;
  }

  /**
   * Package a type with an environment as a single value, using a TInd if the environment is not
   * empty.
   */
  Type with(Type[] tenv) {
    return (tenv != null && tenv.length != 0) ? new TInd(this, tenv) : this;
  }

  /**
   * Create a fresh instance of this type scheme, allocating an environment of new type variables as
   * necessary for Forall schemes.
   */
  public Type instantiate() {
    return this;
  }

  public Type getType() {
    return this;
  }

  /**
   * Find the list of unbound type variables in this type scheme using an accumulating parameter
   * tvs.
   */
  public TVars tvars(TVars tvs) {
    return this.tvars(null, tvs);
  }

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  abstract TVars tvars(Type[] thisenv, TVars tvs);

  public TVar[] generics(TVars fixed) {
    return TVar.generics(tvars(fixed), fixed);
  }

  public Type skeleton() {
    return this.skeleton(null);
  }

  Type skeleton(Type[] thisenv) {
    return this.skeleton(thisenv, TVar.noTVars);
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any Any other unbound TVars are kept as
   * is. All TInd and bound TVar nodes are eliminated in the process.
   */
  abstract Type skeleton(Type[] thisenv, TVar[] generics);

  public Scheme generalize(TVar[] generics) {
    // Find a skeleton for the inferred type:
    Type type = this.skeleton(null, generics);

    // Add quantifier if necessary:
    return (generics.length > 0) ? new Forall(new Prefix(generics), type) : type;
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public abstract boolean same(Type[] thisenv, Type t, Type[] tenv);

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return false;
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  abstract boolean sameTTycon(Type[] thisenv, TTycon that);

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    return false;
  }

  /**
   * Test to determine whether this type is equal to a specified type variable. NOTE: we assume here
   * that the specified TVar is unbound!
   */
  boolean sameTVar(Type[] thisenv, TVar v) {
    return false;
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  abstract Kind calcKind(Type[] thisenv);

  /**
   * Matching of types: test to see if the type on the right can be obtained by instantiating type
   * variables in the type on the left. (The "receiver", or "this", in the following code.)
   *
   * <p>match :: Type -> Env -> Type -> Env -> IO ()
   *
   * <p>Note that it is possible for a partial match to occur, meaning that some of the variables in
   * the receiver might be bound during the matching process, even if match returns false.
   */
  public abstract boolean match(Type[] thisenv, Type t, Type[] tenv);

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    return false;
  }

  /**
   * Test to determine whether the specified variable can be matched to this type. The only type
   * variable that can be instantiated during this process is the variable v passed in as the second
   * argument. The variable v must be unbound.
   */
  boolean matchTVar(Type[] thisenv, TVar v) {
    return v.matchBind(this, thisenv);
  }

  abstract boolean contains(Type[] thisenv, TVar v);

  /** Unify two types with empty type environments. (i.e., no free generic type variables.) */
  public void unify(Position pos, Type that) throws Failure {
    // ! System.out.println("Attempting to unify " + this + " with " + that);
    unify(pos, null, that, null);
  }

  /** Unify two types with specified environments, throwing an exception if unification fails. */
  public void unify(Position pos, Type[] thisenv, Type that, Type[] thatenv) throws Failure {
    try {
      this.unify(thisenv, that, thatenv);
    } catch (UnifyException ue) {
      // TODO: Make a Failure subclass that captures ue
      // TODO: use the types this and that as context for the unify exception
      // ... error occurred while attempting to unify "this" with "that" ...
      throw new Failure(pos, ue.describe());
    }
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public abstract void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException;

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    throw new TypeMismatchException(tap, tapenv, this, thisenv);
  }

  abstract void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException;

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    throw new TypeMismatchException(t, null, this, thisenv);
  }

  /**
   * Unify this type expression with a given (unbound) type variable. This typically just requires
   * binding the specified type variable, but we also need to indirect through TGen and TVar values.
   */
  void unifyTVar(Type[] thisenv, TVar v) throws UnifyException {
    v.unifyBind(this, thisenv);
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    debug.Internal.error("simplifyNatType applied to invalid type: " + skeleton(tenv));
    return null;
  }

  /**
   * Return the number associated with this type if it is a natural number type, or else return
   * null.
   */
  public BigInteger getNat() {
    return null;
  }

  /** Bind a type variable of kind nat to a specific natural number value. */
  void bindNat(BigInteger n) {
    debug.Internal.error("invalid bindNat call");
  }

  /** Convenience method for constructing type applications of the form "this t1". */
  public Type tap(Type t1) {
    return new TAp(this, t1);
  }

  /** Convenience method for constructing type applications of the form "this t1 t2". */
  public Type tap(Type t1, Type t2) {
    return new TAp(tap(t1), t2);
  }

  /** Convenience method for constructing types of the form dom -> rng: */
  public static Type fun(Type dom, Type rng) {
    return DataName.arrow.asType().tap(dom, rng);
  }

  /** Convenience method for constructing types of the form dom ->> rng: */
  public static Type milfun(Type dom, Type rng) {
    return MILArrow.milArrow.asType().tap(dom, rng);
  }

  /** Convenience method for constructing types of the form [dom] ->> [rng]: */
  public static Type milfunTuple(Type dom, Type rng) {
    return milfun(Type.tuple(dom), Type.tuple(rng));
  }

  public static final TTycon empty = TupleCon.tuple(0).asType();

  public static Type tuple(Type t1) {
    return TupleCon.tuple(1).asType().tap(t1);
  }

  public static Type tuple(Type t1, Type t2) {
    return TupleCon.tuple(2).asType().tap(t1, t2);
  }

  public static Type tuple(Type[] types) {
    Type t = TupleCon.tuple(types.length).asType();
    for (int i = 0; i < types.length; i++) {
      t = new TAp(t, types[i]);
    }
    return t;
  }

  public static Type procOf(Type res) {
    return new TAp(DataName.proc.asType(), res);
  }

  /** Convenience method for making a type of the form Bit n for some type w. */
  public static Type bit(Type w) {
    return new TAp(DataName.bit.asType(), w);
  }

  /** Convenience method for making a type of the form Bit n for some known integer value n. */
  public static Type bit(int w) {
    return Type.bit(new TNat(w));
  }

  /** Convenience method for making a type of the form Init a for some area type a. */
  public static Type init(Type a) {
    return new TAp(DataName.init.asType(), a);
  }

  public static final int FLAGSIZE = 1;

  public static final BigInteger BigFLAGSIZE = BigInteger.valueOf(FLAGSIZE);

  public static final Type TypeFLAGSIZE = new TNat(BigFLAGSIZE);

  public static final int WORDSIZE = 32;

  public static final BigInteger BigWORDSIZE = BigInteger.valueOf(WORDSIZE);

  public static final Type TypeWORDSIZE = new TNat(BigWORDSIZE);

  public static final int MAXWIDTH = 3 * WORDSIZE;

  public static final BigInteger BigMAXWIDTH = BigInteger.valueOf(MAXWIDTH);

  /** Return the number of words that are needed to hold a value with the specified bitsize. */
  public static int numWords(int numBits) {
    return (numBits + WORDSIZE - 1) / WORDSIZE;
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataName bitdataName() {
    return null;
  }

  /**
   * Return the natural number type that specifies the BitSize of this type (required to be of kind
   * *) or null if this type has no BitSize (i.e., no bit-level representation). This method should
   * only be used with a limited collection of classes (we only expect to use it with top-level,
   * monomorphic types), but, just in case, we also provide implementations for classes that we do
   * not expect to see in practice, and allow for the possibility of a type environment, even though
   * we expect it will only ever be null.
   */
  public abstract Type bitSize(Type[] tenv);

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return null;
  }

  BigInteger ixBound(Type[] tenv) {
    BigInteger n = simplifyNatType(tenv).getNat();
    if (n == null) {
      debug.Internal.error("invalid Ix bound: " + skeleton(tenv));
    }
    return n.subtract(BigInteger.ONE);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type bitSize(Type[] tenv, Type a, Type b) {
    return null;
  }

  int arefWidth(Type[] tenv) {
    BigInteger n = simplifyNatType(tenv).getNat();
    if (n == null) {
      debug.Internal.error("invalid ARef alignment: " + skeleton(tenv));
    }
    if (n.signum() <= 0) { // not strictly positive
      return 0;
    }
    BigInteger n1 = n.subtract(BigInteger.ONE);
    if (n.and(n1).signum() != 0) { // not a power of two
      return 0;
    }
    int w = Type.WORDSIZE - n1.bitLength(); // Find width
    return (w >= 0 && w <= Type.WORDSIZE) ? w : 0;
  }

  public Pat bitPat(Type[] tenv) {
    return null;
  }

  Pat bitPat(Type[] tenv, Type a) {
    return null;
  }

  Pat bitPat(Type[] tenv, Type a, Type b) {
    return null;
  }

  /**
   * Find the Bitdata Layout associated with values of this type, if there is one, or else return
   * null. TODO: perhaps this code should be colocated with bitdataName()?
   */
  public BitdataLayout bitdataLayout() {
    return null;
  }

  /** Return the number of bytes that are needed to hold a value with the specified bitsize. */
  public static int numBytes(int numBits) {
    return (numBits + 7) / 8;
  }

  /** Find the name of the associated struct type, if any. */
  public StructName structName() {
    return null;
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public abstract Type byteSize(Type[] tenv);

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return null;
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return null;
  }

  /**
   * Worker method for calculating ByteSize (Stored this), with the specified type environment tenv,
   * short-circuiting through indirections and type variables as necessary. Implements the function:
   * ByteSize (Stored (ARef l a)) = WordSize / 8 ByteSize (Stored (APtr l a)) = WordSize / 8
   * ByteSize (Stored t) = 0, if BitSize t == 0 = 1, if 0 < BitSize t <= 8 = 2, if 8 < BitSize t <=
   * 16 = 4, if 16 < BitSize t <= 32 = 8, if 32 < BitSize t <= 64
   */
  Type byteSizeStored(Type[] tenv) {
    Type bytes = byteSizeStoredRef(tenv); // Check cases for ARef and APtr types first
    if (bytes == null) {
      Type bits = bitSize(tenv); // Otherwise check bit representation of this type
      if (bits != null) {
        BigInteger m = bits.simplifyNatType(null).getNat();
        if (m != null) {
          int n = Type.numBytes(m.intValue());
          return (n == 0)
              ? new TNat(0)
              : (n == 1)
                  ? new TNat(1)
                  : (n == 2) ? new TNat(2) : (n <= 4) ? new TNat(4) : (n <= 8) ? new TNat(8) : null;
        }
      }
    }
    return bytes;
  }

  Type byteSizeStoredRef(Type[] tenv) {
    return null;
  }

  Type byteSizeStoredRef(Type[] tenv, Type a) {
    return null;
  }

  Type byteSizeStoredRef(Type[] tenv, Type a, Type b) {
    return null;
  }

  /** Calculate the array of types for a given array of Atom operands. */
  public static Type[] instantiate(Atom[] args) {
    Type[] ts = new Type[args.length];
    for (int i = 0; i < ts.length; i++) {
      ts[i] = args[i].instantiate();
    }
    return ts;
  }

  /** Calculate an array of fresh types for a given collection of Temp operands. */
  public static Type[] freshTypes(Temp[] vs) {
    Type[] ts = new Type[vs.length];
    for (int i = 0; i < ts.length; i++) {
      ts[i] = vs[i].freshType(Tyvar.star);
    }
    return ts;
  }

  /**
   * A worker function that traverses a tuple type and removes the components that are not marked in
   * usedArgs. We assume a very simple structure for the input type: a left-leaning spine of TAps
   * with a tuple type constructor at the head, and no TGen, TVar, or TInd nodes on the spine.
   */
  Type removeArgs(int numUsedArgs, boolean[] usedArgs, int i) {
    debug.Internal.error("removeArgs for BlockType");
    return null;
  }

  /**
   * Find the canonical version of this type skeleton for the given TypeSet using the specified
   * environment to intepret TGens values.
   */
  Type canonType(Type[] env, TypeSet set) {
    return canonType(env, set, 0);
  }

  /**
   * Find the canonical version of a type with respect to the given TypeSet; this should only be
   * used with monomorphic types that do not contain any TGen values.
   */
  Type canonType(TypeSet set) {
    return canonType(null, set, 0);
  }

  /**
   * Find a canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  abstract Type canonType(Type[] env, TypeSet set, int args);

  /**
   * Determine whether the arguments of this (canonical) type match the types on the top of the
   * stack.
   */
  boolean matches(TypeSet set, int n) {
    return n == 0;
  }

  /** Calculate a new version of this type scheme with canonical components. */
  Scheme canonScheme(TypeSet set) {
    return this.canonType(set);
  }

  public Type apply(TVarSubst s) {
    return apply(null, s);
  }

  abstract Type apply(Type[] thisenv, TVarSubst s);

  /**
   * Extend a substitution by matching this (potentially polymorphic) Scheme against a monomorphic
   * instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, Type inst) {
    if (generics.length != 0 || !this.alphaEquiv(inst)) {
      debug.Internal.error("specializingSubst fails on Type");
    }
    return null;
  }

  Type removeTVar() {
    debug.Internal.error("removeTVar: not a type variable");
    return this;
  }

  private static Type[][] wordsCache = new Type[10][];

  public static Type[] words(int n) {
    if (n >= wordsCache.length) {
      Type[][] newCache = new Type[Math.max(n + 1, 2 * wordsCache.length)][];
      for (int i = 0; i < wordsCache.length; i++) {
        newCache[i] = wordsCache[i];
      }
      wordsCache = newCache;
    } else if (wordsCache[n] != null) {
      return wordsCache[n];
    }
    // Code to update cache[arg] = ... will be appended here.

    Type[] ws = new Type[n];
    Type w = DataName.word.asType();
    for (int i = 0; i < n; i++) {
      ws[i] = w;
    }
    return ws;
  }

  /**
   * Return the representation vector for a bitdata value of width w using an appropriate sequence
   * of Word values or, for values of width 1, a single MIL Flag value.
   */
  public static Type[] repBits(int w) {
    return (w == 1) ? DataName.flagRep : Type.words(Type.numWords(w));
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return null;
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return null;
  }

  /**
   * Return the representation for a value of type Bit n, assuming that this object is the TNat for
   * n.
   */
  Type[] bitvectorRep() {
    return null;
  }

  /**
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return null;
  }

  /**
   * Returns true if bitdata values of this type use the lo bits representation, or false for hi
   * bits. The result is meaningless if there is no bit size for values of this type.
   */
  boolean useBitdataLo() {
    return true;
  }

  boolean useBitdataLo(Type s) {
    return true;
  }

  boolean useBitdataLo(Type t, Type s) {
    return true;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, TypeMap tm, int args) {
    //    debug.Internal.error("toLLVM not defined for type " + this);
    return llvm.Type.vd; // not reached
  }

  /** Returns the LLVM type for value that is returned by a function. */
  llvm.Type retType(TypeMap tm) {
    return tm.toLLVM(this.canonType(tm).getArg());
  }

  Type getArg() {
    debug.Internal.error("argument to getArg is not a TAp");
    return this;
  }

  /** Calculate an array of llvm Types corresponding to the components of a given MIL Tuple type. */
  llvm.Type[] tupleToArray(TypeMap tm, int args) {
    if (this != TupleCon.tuple(args).asType()) {
      // TODO: uncomment the following to trigger stricter error checking
      //      debug.Internal.error("tupleToArray not defined for " + this);
      //      return null; // not reached
    }
    return new llvm.Type[args];
  }

  /**
   * Calculate an array of formal argument types for a closure using a value of the specified ptr
   * type as the first argument and adding an extra argument for each component in this type, which
   * must be a tuple.
   */
  llvm.Type[] closureArgs(TypeMap tm, llvm.Type ptr, int args) {
    if (this != TupleCon.tuple(args).asType()) {
      //        debug.Internal.error("closureArgs not defined for " + this);
      //        return null; // not reached
    }
    llvm.Type[] cargs = new llvm.Type[1 + args];
    cargs[0] = ptr;
    return cargs;
  }
}
