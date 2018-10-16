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

  /**
   * Return the number associated with this type if it is a natural number type, or else return
   * null.
   */
  public BigInteger getNat() {
    return null;
  }

  /** Return the string associated with this type if it is a label type, or else return null. */
  public String getLabel() {
    return null;
  }

  /** Test to see if this type scheme is monomorphic. */
  public Type isMonomorphic() {
    return this;
  }

  private static TGen[] genCache;

  public static TGen gen(int n) {
    if (genCache == null) {
      genCache = new TGen[10];
    } else if (n >= genCache.length) {
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
  public boolean alphaEquiv(Scheme right) {
    return right.alphaType(this, null);
  }

  /** Test to determine whether this type is equal to a given TGen. */
  boolean alphaTGen(TGen right, TGenCorresp corresp) {
    return false;
  }

  /** Test to determine whether this type is equal to a given type application. */
  boolean alphaTAp(TAp right, TGenCorresp corresp) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TTycon. */
  boolean alphaTTycon(TTycon right) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TNat. */
  boolean alphaTNat(TNat right) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TLab. */
  boolean alphaTLab(TLab right) {
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
   * generics with a TGen value corresponding to its index. Any other unbound TVars are kept as is.
   * All TInd and bound TVar nodes are eliminated in the process.
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
    return Tycon.arrow.asType().tap(dom, rng);
  }

  /** Convenience method for constructing types of the form dom ->> rng: */
  public static Type milfun(Type dom, Type rng) {
    return Tycon.milArrow.asType().tap(dom, rng);
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

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return (-1);
  }

  public static Type procOf(Type res) {
    return new TAp(Tycon.proc.asType(), res);
  }

  public static final int MAX_BIT_WIDTH = 1000;

  public static final BigInteger BIG_MAX_BIT_WIDTH = BigInteger.valueOf(MAX_BIT_WIDTH);

  /** Convenience method for making a type of the form Bit n for some type w. */
  public static Type bit(Type w) {
    return Tycon.bit.asType().tap(w);
  }

  /** Convenience method for making a type of the form Bit n for some known integer value n. */
  public static Type bit(int w) {
    return Type.bit(new TNat(w));
  }

  /** Convenience method for making a type of the form Ref a for some area type a. */
  public static Type ref(Type areaType) {
    return Tycon.ref.asType().tap(areaType);
  }

  /** Convenience method for making a type of the form Init a for some area type a. */
  public static Type init(Type a) {
    return Tycon.init.asType().tap(a);
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataType bitdataType() {
    return null;
  }

  /** Find the Bitdata Layout associated with values of this type, or else return null. */
  public BitdataLayout bitdataLayout() {
    return null;
  }

  /** Find the name of the associated struct type, if any. */
  public StructType structType() {
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
   * environment to intepret TGen values.
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
   * Find the canonical version of this type in the given set, using the specified environment to
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

  boolean instMatches(Type right) {
    return false;
  }

  boolean instMatchesTycon(Tycon left) {
    return false;
  }

  boolean instMatchesTAp(TAp left) {
    return false;
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return set.rebuild(this, args);
  }

  DataType dataType() {
    return null;
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
    Type w = Tycon.word.asType();
    for (int i = 0; i < n; i++) {
      ws[i] = w;
    }
    return ws;
  }

  /**
   * Return the representation vector for a bitdata value of width w using an appropriate sequence
   * of Word values (so long as w <= MAX_BIT_WIDTH, to avoid creating very large representation
   * vectors for pathological test cases). For values of width 1, the representation vector contains
   * a single MIL Flag value, while for values of width 0, it contains only Unit.
   */
  public static Type[] repBits(int w) {
    return (w == 0)
        ? Tycon.unitRep
        : (w == 1)
            ? Tycon.flagRep
            : (w <= Type.MAX_BIT_WIDTH) ? Type.words(Word.numWords(w)) : null;
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return null;
  }

  /**
   * Return the representation vector for types formed by applying this type to the argument a. This
   * allows us to provide special representations for types of the form Bit a, Ix a, Ref a, etc. If
   * none of these apply, we just return null. TODO: are there other types we should be including
   * here?
   */
  Type[] repCalc(Type a) {
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
   * Return the representation for a value of type NZBit n, assuming that this object is the TNat
   * for n.
   */
  Type[] nzbitvectorRep() {
    return null;
  }

  BigInteger validNat() throws GeneratorException {
    throw new GeneratorException(this + " is not a natural number");
  }

  /**
   * Check that the specified type is a natural number that can be used as the argument for a Bit
   * type.
   */
  int validWidth() throws GeneratorException {
    BigInteger n = validNat();
    validBelow(n, BIG_MAX_BIT_WIDTH);
    return n.intValue();
  }

  int validWidth(int lo) throws GeneratorException {
    int n = validWidth();
    validNotBelow(n, lo);
    return n;
  }

  int validMemBitSize() throws GeneratorException {
    int w = memBitSize(null);
    if (w < 0) {
      throw new GeneratorException("No known BitSize for Stored value of type " + this);
    }
    return w;
  }

  /**
   * Determine whether the given number is small enough to fit in a signed long; we assume already
   * that it is non negative.
   */
  static void validSigned(BigInteger n) throws GeneratorException {
    if (n.compareTo(Word.maxSigned()) > 0) {
      throw new GeneratorException(
          "parameter value " + n + " is too large; must be at most " + Word.maxSigned());
    }
  }

  /**
   * Check that the specified type is a natural number that can be used as the argument for an Ix
   * type. Specifically, we require that this type must be in the range [1..maxSigned], which
   * ensures that all Ix n values are nonempty (because n>0) and can be stored within a single Word
   * (because n<=maxSigned). If this test pasts, then it is safe to use longValue() on the result
   * without loss of information.
   */
  BigInteger validIndex() throws GeneratorException {
    BigInteger n = validNat();
    if (n.signum() <= 0) {
      throw new GeneratorException("parameter value " + n + " is too small; must be at least 1");
    }
    validSigned(n);
    return n;
  }

  static void validBelow(BigInteger v, BigInteger tooBig) throws GeneratorException {
    if (v.compareTo(tooBig) >= 0) {
      throw new GeneratorException("parameter " + v + " is too large; must be less than " + tooBig);
    }
  }

  static void validNotBelow(long n, long lo) throws GeneratorException {
    if (n < lo) {
      throw new GeneratorException("parameter " + n + " is too low; must be at least " + lo);
    }
  }

  /**
   * Find the number of words (parameter slots) that are needed to represent a value of this type.
   * If there is a change of representation, then use the length of the associated representation
   * vector; otherwise, one parameter maps to one word.
   */
  int repLen() {
    Type[] r = repCalc();
    return (r == null) ? 1 : r.length;
  }

  long validArrayArea() throws GeneratorException {
    Type bs = byteSize(null);
    if (bs == null) {
      throw new GeneratorException("Cannot determine ByteSize for " + this);
    }
    BigInteger s = bs.validNat();
    validSigned(s);
    long align = alignment(null);
    if (align == 0) {
      throw new GeneratorException("Cannot determine alignment for " + this);
    }
    long size = s.longValue();
    if ((size % align) != 0) {
      throw new GeneratorException(
          "Element size " + size + " is not divisible by alignment " + align);
    }
    return size;
  }

  /**
   * Continue the work of generatePrim() in the special case where we have found a type of the form
   * [d1,...,dn] ->> rt. The type rt is the receiver here and the types d1,...,dn are in the array
   * ds.
   */
  Call generatePrim(Position pos, String id, Type[] ds) {
    Type[] rs = tupleComponents(0); // Look for tuple components from this range type
    if (rs == null) { // Fail if this is not a tuple type
      return null;
    } else if (rs.length == 1) { // Possible recursion ,,,
      Call call = rs[0].generatePrimNested(pos, id, ds);
      if (call != null) {
        return call;
      }
    }
    BlockType bt = new BlockType(Type.tuple(ds), Type.tuple(rs));
    return new PrimCall(new Prim(id, Prim.IMPURE, bt));
  }

  /**
   * Test to see whether the receiver matches the grammar for pt, but with the additional
   * information that it appears in the context of an enclosing type of the form [d1,...,dn] ->>
   * [this].
   */
  Call generatePrimNested(Position pos, String id, Type[] ds) {
    return null;
  }

  /** Build a new array that combines the elements from the left array with those from the right. */
  public static Type[] append(Type[] left, Type[] right) {
    int l = left.length;
    if (l == 0) {
      return right;
    }
    int r = right.length;
    if (r == 0) {
      return left;
    }
    Type[] n = new Type[l + r];
    for (int i = 0; i < l; i++) {
      n[i] = left[i];
    }
    for (int i = 0; i < r; i++) {
      n[l + i] = right[i];
    }
    return n;
  }

  /**
   * Test to determine if this skeleton is an application of (->>) to a tuple of types, returning
   * either the tuple components in an array or null if there is no match.
   */
  Type[] funcFromTuple1() {
    return null;
  }

  /** Test to determine if this type is the MIL function arrow, ->>, without any arguments. */
  boolean isMILArrow() {
    return false;
  }

  /**
   * Test to determine if this type is a tuple of the form [t1,...,tn], returning either the
   * components of the tuple in an array, or null if there is no match. The argument is the number
   * of potential tuple components that have already been seen; the initial call should use 0 for
   * this argument.
   */
  Type[] tupleComponents(int n) {
    return null;
  }

  /**
   * Generate code for the specified Block that takes at least the arguments us++vs, and will
   * require a call to f @ vs.
   */
  Code liftToCode(Block b, Temp[] us, Atom f, Temp[] vs) {
    Type[] rs = tupleComponents(0); // Test to see if the range is a tuple
    if (rs != null && rs.length == 1) {
      Code c = rs[0].liftToCode0(b, us, f, vs);
      if (c != null) {
        return c;
      }
    }
    // At this point, we know that range does not match [[d1...] ->> r], so we have found all of the
    // required parameters:
    b.setParams(Temp.append(us, vs));
    return new Done(new Enter(f, vs));
  }

  /**
   * Helper function for liftToCode, used in the case where the receiver is the only component (in
   * position 0, explaining the name of this method) in a tuple type that is known to be the range
   * of a ->> function.
   */
  Code liftToCode0(Block b, Temp[] us, Atom f, Temp[] vs) {
    return null;
  }

  /**
   * Returns true if bitdata values of this type use the lo bits representation, or false for hi
   * bits. This method should only be used for types that have an associated bit size.
   */
  boolean useBitdataLo() {
    return true;
  }

  boolean useBitdataLo(Type s) {
    return true;
  }

  /**
   * Determine whether this type is a natural number that falls within the specified range,
   * inclusive of bounds.
   */
  BigInteger inRange(BigInteger lo, BigInteger hi) {
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
   * Calculate the width in bits that is needed to represent a reference or pointer to a value of
   * this type using BitSize (Ref a) = WordSize - p if Align a = 2^p.
   */
  int refWidth(Type[] tenv) {
    long alignment = this.alignment(tenv);
    int width = Word.size();
    if (alignment > 0 && ((alignment & (alignment - 1)) == 0)) { // power of two alignment
      while ((alignment >>= 1) != 0) {
        width--;
      }
    }
    return width;
  }

  public Pat bitPat(Type[] tenv) {
    return null;
  }

  Pat bitPat(Type[] tenv, Type a) {
    return null;
  }

  int bitWidth(Type[] tenv) {
    BigInteger nat = simplifyNatType(tenv).getNat();
    if (nat == null) {
      debug.Internal.error("Unresolved size parameter " + skeleton(tenv));
    } else if (nat.signum() < 0 || nat.compareTo(Type.BIG_MAX_BIT_WIDTH) > 0) {
      return (-1);
    }
    return nat.intValue();
  }

  /**
   * Return the number of bytes that are needed to hold an in-memory representation of a value with
   * the specified bitsize. A negative result indicates that there is no in-memory representation
   * for a value of this width (at least, as a single value).
   */
  public static int numBytes(int numBits) {
    if (numBits < 0) { // negative input ==> no in-memory representation
      return numBits;
    }
    int n = (numBits + 7) / 8;
    return (n == 0)
        ? 0 // if BitSize t == 0
        : (n == 1)
            ? 1 // if 0  < BitSize t <= 8
            : (n == 2)
                ? 2 // if 8  < BitSize t <= 16
                : (n <= 4)
                    ? 4 // if 16 < BitSize t <= 32
                    : (n <= 8)
                        ? 8 // if 32 < BitSize t <= 64
                        : (-1); // too big!
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
   * Worker method for calculating ByteSize (Stored this), with the specified type environment tenv.
   */
  Type byteSizeStored(Type[] tenv) {
    int n = Type.numBytes(memBitSize(tenv));
    return (n >= 0) ? new TNat(n) : null;
  }

  /**
   * Calculate the number of bits required for an in-memory representation of this type. This is
   * essentially the same as the bitSize of the type, except for the special case of reference
   * types, which use a full word. A negative result indicates that there is no in-memory
   * representation for values of this type.
   */
  int memBitSize(Type[] tenv) {
    if (referenceType(tenv)) { // Check cases for Ref and Ptr types first
      return Word.size();
    }
    Type bits = bitSize(tenv); // Otherwise check bit representation of this type
    if (bits != null) {
      BigInteger m = bits.simplifyNatType(null).getNat();
      if (m != null) {
        return m.intValue();
      }
    }
    return (-1);
  }

  /** Determine if this is a type of the form (Ref a) or (Ptr a) for some area type a. */
  boolean referenceType(Type[] tenv) {
    return false;
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return false;
  }

  /** Return the alignment of this type (or zero if there is no alignment). */
  public abstract long alignment(Type[] tenv);

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return 0;
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return 0;
  }

  /**
   * Worker method for calculating Align (Stored this), with the type environment tenv. Returns an
   * alignment of 1 if this type has no stored representation, or has a zero width stored
   * representation.
   */
  long alignmentStored(Type[] tenv) {
    return Math.max(1, Type.numBytes(memBitSize(tenv)));
  }

  /** Check that an area of this type has a known ByteSize. */
  public Type calcAreaSize(Position pos) throws Failure {
    Type size = byteSize(null);
    if (size == null || size.getNat() == null) {
      throw new Failure(pos, "Cannot determine ByteSize for type \"" + this + "\"");
    }
    return size;
  }

  /**
   * Check that an area of this type has a valid alignment, consistent with declared value, if
   * given.
   */
  public long calcAreaAlignment(Position pos, MILEnv milenv, TypeExp alignExp) throws Failure {
    long alignment = this.alignment(null);
    if (alignment < 1) {
      throw new Failure(pos, "Unable to determine alignment for " + this);
    } else if (alignExp != null) {
      alignExp.scopeType(null, milenv.getTyconEnv(), 0);
      alignExp.checkKind(KAtom.NAT);
      return alignExp.getAlignment(alignment);
    }
    return alignment;
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return nonUnit(null);
  }

  boolean nonUnit(Type[] tenv) {
    return true;
  }

  /**
   * Filter all unit values from this array producing either a new (shorter) array, or just
   * returning the original array if all of the elements are non-units.
   */
  static Type[] nonUnits(Type[] xs) {
    int nonUnits = 0; // count number of non unit components
    for (int i = 0; i < xs.length; i++) {
      if (xs[i].nonUnit()) {
        nonUnits++;
      }
    }
    if (nonUnits >= xs.length) { // all components are non unit
      return xs; // so there is no change
    }
    Type[] nxs = new Type[nonUnits]; // make array with just the non units
    for (int i = 0, j = 0; j < nonUnits; i++) {
      if (xs[i].nonUnit()) {
        nxs[j++] = xs[i];
      }
    }
    return nxs;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    debug.Internal.error("toLLVM not defined for type " + this);
    return llvm.Type.vd; // not reached
  }

  /** Returns the LLVM type for value that is returned by a function. */
  llvm.Type retType(LLVMMap lm) {
    return lm.toLLVM(this.canonType(lm).getArg());
  }

  Type getArg() {
    debug.Internal.error("argument to getArg is not a TAp");
    return this;
  }

  /**
   * Calculate an array of llvm Types corresponding to the components of a given MIL Tuple type.
   * Unit types are filtered out in the process, so the resulting array may not actually have as
   * many components as the input tuple type.
   */
  llvm.Type[] tupleToArray(LLVMMap lm, int args, int nonUnits) {
    if (this != TupleCon.tuple(args).asType()) {
      debug.Internal.error("tupleToArray not defined for " + this);
      return null; // not reached
    }
    return new llvm.Type[nonUnits];
  }

  /**
   * Calculate an array of formal argument types for a closure using a value of the specified ptr
   * type as the first argument and adding an extra argument for each component in this type, which
   * must be a tuple.
   */
  llvm.Type[] closureArgs(LLVMMap lm, llvm.Type ptr, int args, int nonUnits) {
    if (this != TupleCon.tuple(args).asType()) {
      debug.Internal.error("closureArgs not defined for " + this);
      return null; // not reached
    }
    llvm.Type[] cargs = new llvm.Type[1 + nonUnits];
    cargs[0] = ptr;
    return cargs;
  }
}
