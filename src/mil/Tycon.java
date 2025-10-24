/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
import java.io.PrintWriter;
import java.math.BigInteger;
import obdd.Pat;

/** Names for type constants, each of which has an associated kind. */
public abstract class Tycon extends Name {

  /** Default constructor. */
  public Tycon(Position pos, String id) {
    super(pos, id);
  }

  /**
   * Get the array of constructor functions associated with this object, or return null if this is
   * not a DataName.
   */
  public Cfun[] getCfuns() {
    return null;
  }

  /** Return the kind of this type constructor. */
  public abstract Kind getKind();

  /** Return the arity of this type constructor. */
  public abstract int getArity();

  public Synonym isSynonym() {
    return null;
  }

  public void fixKinds() {
    /* Nothing to do here */
  }

  private TTycon type = new TTycon(this);

  public TTycon asType() {
    return type;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal, possibly with some correspondence between the TGen objects in
   * the two types. We use the names left and right to keep track of which types were on the left
   * and the right in the original alphaEquiv() call so that we can build the TGenCorresp in a
   * consistent manner.
   */
  boolean alphaType(Type left, TGenCorresp corresp) {
    return left.alphaTycon(this);
  }

  /** Test to determine whether this type is equal to a given type application. */
  boolean alphaTAp(TAp right, TGenCorresp corresp) {
    return false;
  }

  /** Test to determine whether this type is equal to a given Tycon. */
  boolean alphaTycon(Tycon right) {
    return this == right;
  }

  /** Test to determine whether this type is equal to a given TNat. */
  boolean alphaTNat(TNat right) {
    return false;
  }

  /** Test to determine whether this type is equal to a given TLab. */
  boolean alphaTLab(TLab right) {
    return false;
  }

  protected Fixity fixity = Fixity.unspecified;

  /** Get the fixity associated with this name, if any. */
  public Fixity getFixity() {
    return fixity;
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    //                      has fixity null fixity   has fixity null fixity
    //          +-------+  +----------+----------+  +----------+----------+
    // begins   |   n   |  |  (x `n`) |   n x    |  | x `n` y  |  n x y   |
    // w/letter |       |  |          |          |  |          |          |
    //          +-------+  +----------+----------+  +----------+----------+
    // begins   |  (n)  |  |  (x n)   |   (x n)  |  | x n y    |  x n y   |
    // w/other  |       |  |          |          |  |          |          |
    //          +-------+  +----------+----------+  +----------+----------+
    //           nullary             unary                   binary
    //
    boolean alphaName = Character.isLetter(id.charAt(0));
    if (args == 0) {
      if (alphaName) {
        tw.write(id); // Id
      } else {
        tw.write("(", id, ")"); // (->)
      }
    } else if (alphaName && fixity == Fixity.unspecified) { // Id t ... t
      applic(tw, prec, args, 0);
    } else if (args == 1) {
      tw.write("(");
      tw.pop().write(tw, fixity.leftPrec(), 0);
      if (alphaName) {
        tw.write(" `", id, "`)"); // (t `Id`)
      } else {
        tw.write(" ", id, ")"); // (t ->)
      }
    } else if (args == 2) {
      tw.open(prec > fixity.getPrec());
      tw.pop().write(tw, fixity.leftPrec(), 0);
      if (alphaName) {
        tw.write(" `", id, "` "); // t `Id` t
      } else {
        tw.write(" ", id, " "); // t -> t
      }
      tw.pop().write(tw, fixity.rightPrec(), 0);
      tw.close(prec > fixity.getPrec());
    } else { // (t -> t) t ... t
      applic(tw, prec, args, 2);
    }
  }

  /**
   * Print a type expression in applicative syntax; one parameter specifies the number of arguments
   * that should be used to print the head, while another specifies the total number of arguments
   * (including those for the head, thus args>use is required).
   */
  void applic(TypeWriter tw, int prec, int args, int use) {
    tw.open(prec >= Fixity.ALWAYS);
    write(tw, Fixity.ALWAYS, use);
    for (int i = use; i < args; i++) {
      tw.write(" ");
      tw.pop().write(tw, Fixity.ALWAYS, 0);
    }
    tw.close(prec >= Fixity.ALWAYS);
  }

  public int findLevel() throws Failure {
    return 0;
  }

  boolean sameTLit(TLit t) {
    return false;
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    return null;
  }

  public static final String milArrowId = "->>";

  private static final Fixity arrowFixity = new Fixity(Fixity.RIGHT, 5);

  public static final Tycon milArrow =
      new PrimTycon(
          milArrowId, new KFun(KAtom.TUPLE, new KFun(KAtom.TUPLE, KAtom.STAR)), 2, arrowFixity);

  public static final DataType arrow = new DataType("->", Kind.simple(2), 2, arrowFixity);

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return (-1);
  }

  public static final DataType proc = new DataType("Proc", Kind.simple(1), 1);

  public static final DataType unit = new DataType("Unit", KAtom.STAR, 0);

  public static final Kind natToStar = new KFun(KAtom.NAT, KAtom.STAR);

  public static final Kind labToStar = new KFun(KAtom.LAB, KAtom.STAR);

  public static final Kind areaToStar = new KFun(KAtom.AREA, KAtom.STAR);

  public static final Kind starToArea = new KFun(KAtom.STAR, KAtom.AREA);

  public static final Kind natToAreaToStar = new KFun(KAtom.NAT, areaToStar);

  public static final Kind areaToArea = new KFun(KAtom.AREA, KAtom.AREA);

  public static final Kind natToAreaToArea = new KFun(KAtom.NAT, areaToArea);

  public static final Tycon addr = new PrimTycon("Addr", KAtom.STAR, 0);

  public static final Tycon bit = new PrimTycon("Bit", natToStar, 1);

  public static final Tycon nzbit = new PrimTycon("NZBit", natToStar, 1);

  public static final Tycon ix = new PrimTycon("Ix", natToStar, 1);

  public static final Tycon inx = new PrimTycon("Inx", natToStar, 1);

  public static final Tycon nat = new PrimTycon("ProxyNat", natToStar, 1);

  public static final Tycon lab = new PrimTycon("ProxyLab", labToStar, 1);

  public static final Tycon pad = new PrimTycon("Pad", natToAreaToArea, 2);

  public static final Tycon array = new PrimTycon("Array", natToAreaToArea, 2);

  public static final Tycon ref = new PrimTycon("Ref", areaToStar, 1);

  public static final Tycon init = new PrimTycon("Init", areaToStar, 1);

  public static final Tycon stored = new PrimTycon("Stored", starToArea, 1);

  public static final Tycon string = new PrimTycon("String", KAtom.AREA, 0);

  public static final Synonym wordBits = new Synonym("WordBits", KAtom.NAT, null);

  public static final Synonym word = new Synonym("Word", KAtom.STAR, Type.bit(wordBits.asType()));

  public static final Synonym nzword =
      new Synonym("NZWord", KAtom.STAR, Type.nzbit(wordBits.asType()));

  public static final Synonym flag = new Synonym("Flag", KAtom.STAR, Type.bit(1));

  public static final DataType ptr = new Ptr("Ptr", areaToStar, 1);

  public static final DataType phys = new Ptr("Phys", areaToStar, 1);

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

  /**
   * Print a definition for this type constructor using source level syntax. TODO: Find a more
   * appropriate place for this code ...
   */
  abstract void dumpTypeDefinition(PrintWriter out);

  /**
   * Generate a diagram for this type constructor using tikz syntax. TODO: Find a more appropriate
   * place for this code ...
   */
  void dumpTypeDiagram(PrintWriter out) {
    /* do nothing */
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return set.canon(this, args);
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  abstract Tycon canonTycon(TypeSet set);

  /**
   * Attempt to translate a type with this type constructor at its head and some number of arguments
   * on the stack to eliminate newtypes. This will only succeed if the head is a newtype constructor
   * and there are enough arguments; otherwise the call will return null to indicate that no
   * translation was possible.
   */
  Type translate(NewtypeTypeSet set, int args) {
    return null;
  }

  /**
   * Determine if this is a singleton type (i.e., a type with only one value), in which case we will
   * use the Unit type to provide a representation.
   */
  boolean isSingleton() {
    return false;
  }

  /**
   * Find out if there is a specialized version of the type with this Tycon as its head and the set
   * of args (canonical) arguments on the stack of this MILSpec object.
   */
  Tycon specInst(MILSpec spec, int args) {
    return null;
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return set.rebuild(this.asType(), args);
  }

  /** Find the bitdata representation for this object, or null if there is none. */
  BitdataRep findRep(BitdataMap m) {
    return null;
  }

  BitdataRep isBitdataRep() {
    return null;
  }

  /** Determine whether this Tycon is a DataType that is a candidate for bitdata representation. */
  DataType bitdataCandidate() {
    return null;
  }

  DataType dataType() {
    return null;
  }

  /** Determine whether this Tycon is a DataType that is a candidate for merging. */
  DataType mergeCandidate() {
    return null;
  }

  boolean sameMod(Type t, Type[] tenv, MergeMap mmap) {
    return t.sameTyconMod(tenv, this, mmap);
  }

  boolean sameTyconMod(Tycon l, MergeMap mmap) {
    return this == l;
  }

  /**
   * Determine whether this Tycon is equivalent to a specified DataType, modulo a given MergeMap.
   */
  boolean sameDataTypeMod(DataType l, MergeMap mmap) {
    return false;
  }

  /** Representation vector for singleton types. */
  static final Type[] unitRep = new Type[] {unit.asType()};

  /** Representation vector for bitdata types of width one. */
  static final Type[] flagRep = new Type[] {flag.asType()};

  /** Representation vector for Ix, Ref, Ptr, etc. types that fit in a single word. */
  static final Type[] wordRep = new Type[] {word.asType()};

  /** Representation vector for Init a types as functions of type [Word] ->> [Unit]. */
  static final Type[] initRep =
      new Type[] {Type.milfun(Type.tuple(word.asType()), Type.tuple(unit.asType()))};

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return (this == addr) ? Tycon.wordRep : null;
  }

  /**
   * Return the representation vector for types formed by applying this type constructor to the
   * argument a. This allows us to provide special representations for types of the form Bit a, Ix
   * a, Ref a, etc. If none of these apply, we just return null. TODO: are there other types we
   * should be including here?
   */
  Type[] repCalc(Type a) {
    return (this == ref)
        ? Tycon.wordRep
        : (this == bit)
            ? a.simplifyNatType(null).bitvectorRep()
            : (this == nzbit)
                ? a.simplifyNatType(null).nzbitvectorRep()
                : (this == ix)
                    ? a.simplifyNatType(null).ixbitvectorRep()
                    : (this == inx)
                        ? a.simplifyNatType(null).inxbitvectorRep()
                        : (this == init)
                            ? Tycon.initRep
                            : (this == nat) ? Tycon.unitRep : (this == lab) ? Tycon.unitRep : null;
  }

  /**
   * Rewrite the argument stack in the given RepTypeSet to account for a change of representation if
   * this is a tuple type with any arguments whose representation is changed. Returns the number of
   * arguments in the rewritten list, or (-1) if no change of representation is required.
   */
  int repTransform(RepTypeSet set, int args) {
    return (-1);
  }

  /**
   * Generate a call to a new primitive, wrapped in an appropriate chain of closure definitions, if
   * this type can be derived from pt in the following grammar: pt ::= [d1,...,dn] ->> rt ; rt ::=
   * [pt] | [r1,...,rm] .
   */
  Tail generatePrim(Position pos, String id) {
    return null;
  }

  /**
   * Test to see whether the receiver matches the grammar for pt, but with the additional
   * information that it appears in the context of an enclosing type of the form [d1,...,dn] ->>
   * [this].
   */
  Call generatePrimNested(Position pos, String id, Type[] ds) {
    return null;
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
    return this == milArrow;
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

  /**
   * Helper function for liftToCode, used in the case where the receiver is the only component (in
   * position 0, explaining the name of this method) in a tuple type that is known to be the range
   * of a ->> function.
   */
  Code liftToCode0(Block b, Temp[] us, Atom f, Temp[] vs) {
    return null;
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    if (this == ref || this == ptr || this == phys) { // BitSize (Ref a) ==>  (calculation follows)
      int w = a.refWidth(tenv);
      return (w > 0) ? new TNat(w) : null;
    } else if (this == bit || this == nzbit) { // BitSize(Bit n) ==>  n,  same for (NZBit n)
      return a.simplifyNatType(tenv);
    } else if (this == ix) { // BitSize(Ix n)  ==>  (see below, upper bound n-1)
      return rangeBitSize(a.ixUpper(tenv).subtract(BigInteger.ONE));
    } else if (this == inx) { // BitSize(Inx n)  ==>  (see below, upper bound n)
      return rangeBitSize(a.ixUpper(tenv));
    }
    return null;
  }

  /**
   * Find a type for the width of a bit field that contains numbers in the range 0 to n inclusive,
   * or null if n is negative.
   */
  private Type rangeBitSize(BigInteger n) {
    if (n.signum() >= 0) {
      int w = n.bitLength();
      if (w >= 0 && w < Word.size()) {
        return new TNat(BigInteger.valueOf(w));
      }
    }
    return null;
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return null;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    return null;
  }

  Pat bitPat(Type[] tenv, Type a) {
    if (this == ref) {
      int w = a.refWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w).restrict() : null;
    } else if (this == ptr || this == phys) {
      int w = a.refWidth(tenv);
      return (w > 0) ? obdd.Pat.all(w).restrict() : null;
    } else if (this == bit) {
      int w = a.bitWidth(tenv);
      return (w >= 0) ? obdd.Pat.all(w) : null;
    } else if (this == nzbit) {
      int w = a.bitWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w) : null;
    } else if (this == ix) {
      return rangeBitPat(a.ixUpper(tenv).subtract(BigInteger.ONE));
    } else if (this == inx) {
      return rangeBitPat(a.ixUpper(tenv));
    }
    return null;
  }

  private Pat rangeBitPat(BigInteger n) {
    if (n.signum() >= 0) {
      int w = n.bitLength();
      if (w >= 0 && w < Word.size()) {
        return obdd.Pat.lessEq(w, n.intValue());
      }
    }
    return null;
  }

  /** Return the nat that specifies the byte size of the type produced by this type constructor. */
  public Type byteSize() {
    return null;
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return (this == stored) ? a.byteSizeStored(tenv) : null;
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    if (this == array || this == pad) {
      // ByteSize (Array a b) = a * ByteSize b
      // ByteSize (Pad   a b) = a * ByteSize b
      BigInteger n = a.simplifyNatType(tenv).getNat();
      if (n != null) {
        Type s = b.byteSize(tenv);
        if (s != null) {
          BigInteger m = s.simplifyNatType(null).getNat();
          if (m != null) {
            if (this
                == array) { // For array, check that element size is a multiple of the alignment
              long align = b.alignment(tenv);
              if (align < 1 || (m.longValue() % align) != 0) {
                return null;
              }
            }
            return new TNat(n.multiply(m));
          }
        }
      }
    }
    return null;
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
    return (this == ref);
  }

  /** Return the alignment associated with this type constructor. */
  public long alignment() {
    return 0;
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return (this == stored) ? a.alignmentStored(tenv) : null;
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return (this == array)
        ? b.alignment(tenv) // Align (Array a b) = Align b
        : (this == pad)
            ? 1L // Align (Pad   a b) = 1
            : 0;
  }

  public TypeExp scopeTycon(Position pos, int arity) throws Failure {
    // The following test is technically unnecessary given the subsequent use
    // of kind inference but may result in friendlier error diagnostics.
    if (arity > getArity()) {
      throw new TooManyTyconArgsFailure(pos, this, arity);
    }
    return new TyconTypeExp(pos, this);
  }

  /** Return the argument of this type (assuming that this is a type application). */
  public Type argOf(Type[] tenv) {
    debug.Internal.error("argOf applied to a non-TAp");
    return null;
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return true;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full (canonical)
   * type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    if (this == milArrow) {
      if (args != 2) {
        debug.Internal.error("MILArrow toLLVM arity mismatch");
      }
      return lm.closurePtrTypeCalc(c);
    } else if (this == bit) {
      if (args != 1) {
        debug.Internal.error("Bit toLLVM arity mismatch");
      }
      return lm.stackArg(1).simplifyNatType(null).llvmBitType();
    }
    debug.Internal.error("toLLVM not defined for tycon " + this.asType());
    return llvm.Type.vd; // not reached
  }
}
