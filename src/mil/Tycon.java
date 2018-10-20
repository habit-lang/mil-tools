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
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    if (Character.isLetter(id.charAt(0))) { // Use prefix syntax?
      if (args == 0) {
        tw.write(id);
      } else {
        applic(tw, prec, args, 0);
      }
    } else if (args == 0) { // Infix operator, no arguments
      tw.write("(");
      tw.write(id);
      tw.write(")");
    } else if (args == 1) { // Infix, single argument
      applic(tw, prec, args, 0);
    } else if (args == 2) { // Infix, two arguments
      tw.open(prec > TypeWriter.FUNPREC);
      tw.pop().write(tw, TypeWriter.FUNPREC + 1, 0);
      tw.write(" ");
      tw.write(id);
      tw.write(" ");
      tw.pop().write(tw, TypeWriter.FUNPREC, 0); // right assoc
      tw.close(prec > TypeWriter.FUNPREC);
    } else { // Infix, args>2
      applic(tw, prec, args, 2);
    }
  }

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

  public static final Tycon milArrow =
      new PrimTycon(milArrowId, new KFun(KAtom.TUPLE, new KFun(KAtom.TUPLE, KAtom.STAR)), 2);

  public static final DataType arrow = new DataType("->", Kind.simple(2), 2);

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

  public static final Kind areaToStar = new KFun(KAtom.AREA, KAtom.STAR);

  public static final Kind starToArea = new KFun(KAtom.STAR, KAtom.AREA);

  public static final Kind natToAreaToStar = new KFun(KAtom.NAT, areaToStar);

  public static final Kind areaToArea = new KFun(KAtom.AREA, KAtom.AREA);

  public static final Kind natToAreaToArea = new KFun(KAtom.NAT, areaToArea);

  public static final Tycon word = new PrimTycon("Word", Kind.simple(0), 0);

  public static final Tycon nzword = new PrimTycon("NZWord", Kind.simple(0), 0);

  public static final Tycon addr = new PrimTycon("Addr", Kind.simple(0), 0);

  public static final Tycon flag = new PrimTycon("Flag", Kind.simple(0), 0);

  public static final Tycon bit = new PrimTycon("Bit", natToStar, 1);

  public static final Tycon nzbit = new PrimTycon("NZBit", natToStar, 1);

  public static final Tycon ix = new PrimTycon("Ix", natToStar, 1);

  public static final Tycon pad = new PrimTycon("Pad", natToAreaToArea, 2);

  public static final Tycon array = new PrimTycon("Array", natToAreaToArea, 2);

  public static final Tycon ref = new PrimTycon("Ref", areaToStar, 1);

  public static final Tycon ptr = new PrimTycon("Ptr", areaToStar, 1);

  public static final Tycon init = new PrimTycon("Init", areaToStar, 1);

  public static final Tycon stored = new PrimTycon("Stored", starToArea, 1);

  public static final Tycon string = new PrimTycon("String", KAtom.AREA, 0);

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

  /** Representation vector for singleton types. */
  public static final Type[] unitRep = new Type[] {unit.asType()};

  /** Representation vector for bitdata types of width one. */
  public static final Type[] flagRep = new Type[] {flag.asType()};

  /** Representation vector for Ix, Ref, Ptr, etc. types that fit in a single word. */
  public static final Type[] wordRep = new Type[] {word.asType()};

  /** Representation vector for Init a types as functions of type [Word] ->> [Unit]. */
  public static final Type[] initRep =
      new Type[] {Type.milfun(Type.tuple(word.asType()), Type.tuple(unit.asType()))};

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return (this == addr || this == nzword) ? Tycon.wordRep : null;
  }

  /**
   * Return the representation vector for types formed by applying this type to the argument a. This
   * allows us to provide special representations for types of the form Bit a, Ix a, Ref a, etc. If
   * none of these apply, we just return null. TODO: are there other types we should be including
   * here?
   */
  Type[] repCalc(Type a) {
    return (this == ref || this == ptr)
        ? Tycon.wordRep
        : (this == bit)
            ? a.simplifyNatType(null).bitvectorRep()
            : (this == nzbit)
                ? a.simplifyNatType(null).nzbitvectorRep()
                : (this == ix)
                    ? Tycon
                        .wordRep // N.B. even Ix 1, a singleton type, is represented by a Word ...
                    : (this == init) ? Tycon.initRep : null;
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
    if (this == bit || this == nzbit) { // BitSize(Bit n) ==>  n,  same for (NZBit n)
      return a.simplifyNatType(tenv);
    } else if (this == ix) { // BitSize(Ix n)  ==>  (calculation below)
      BigInteger n = a.ixBound(tenv);
      if (n.signum() <= 0) {
        return new TNat(BigInteger.ZERO);
      }
      int w = n.bitLength();
      if (w < 0 || w >= Word.size()) {
        return null;
      }
      return new TNat(BigInteger.valueOf(w));
    } else if (this == ref || this == ptr) { // BitSize (Ref a) ==>  (calculation below)
      return new TNat(a.refWidth(tenv));
    }
    return null;
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return (this == word || this == nzword)
        ? Word.sizeType()
        : (this == flag) ? Flag.sizeType : null;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    return (this == word)
        ? Word.allPat()
        : (this == nzword) ? Word.nonzeroPat() : (this == flag) ? Flag.allPat : null;
  }

  Pat bitPat(Type[] tenv, Type a) {
    if (this == ref) {
      int w = a.refWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w) : null;
    } else if (this == ptr) {
      int w = a.refWidth(tenv);
      return (w > 0) ? obdd.Pat.all(w) : null;
    } else if (this == bit) {
      int w = a.bitWidth(tenv);
      return (w >= 0) ? obdd.Pat.all(w) : null;
    } else if (this == nzbit) {
      int w = a.bitWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w) : null;
    } else if (this == ix) {
      BigInteger n = a.ixBound(tenv);
      if (n.signum() <= 0) {
        return obdd.Pat.empty(0);
      }
      int w = n.bitLength();
      if (w < 0 || w >= Word.size()) {
        // TODO: generate an internal error?  or make above internals return null instead?
        return null;
      }
      return obdd.Pat.lessEq(w, n.intValue());
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
    return (this == ref || this == ptr);
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

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return true;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    if (this == milArrow) {
      if (args != 2) {
        debug.Internal.error("MILArrow toLLVM arity mismatch");
      }
      return lm.closurePtrTypeCalc(c);
    }
    debug.Internal.error("toLLVM not defined for tycon " + this.asType());
    return llvm.Type.vd; // not reached
  }
}
