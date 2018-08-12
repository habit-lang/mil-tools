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

/** Names for type constants, each of which has an associated kind. */
public abstract class Tycon extends TypeName {

  /** Default constructor. */
  public Tycon(Position pos, String id, Kind kind) {
    super(pos, id, kind);
  }

  public int getArity() {
    // TODO: this is a bogus definition; these arity values are only used to provide (hopefully)
    // friendlier error messages when a tycon is applied to the wrong number of arguments ...
    // maybe the getArity() method should be replaced with something that more directly supports
    // that test.  Or perhaps we can find a different way to get a better error message ...
    return Integer.MAX_VALUE;
  }

  public void fixKinds() {
    super.fixKinds();
    debug.Log.println(id + " :: " + kind);
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

  public Synonym isSynonym() {
    return null;
  }

  public int findLevel() throws Failure {
    return 0;
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

  /** Find the name of the associated bitdata type, if any. */
  public BitdataName bitdataName() {
    return null;
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    if (this == DataName.bit
        || this == DataName.nzbit) { // BitSize(Bit n) ==>  n,  same for (NZBit n)
      return a.simplifyNatType(tenv);
    } else if (this == DataName.ix) { // BitSize(Ix n)  ==>  (calculation below)
      BigInteger n = a.ixBound(tenv);
      if (n.signum() <= 0) {
        return new TNat(BigInteger.ZERO);
      }
      int w = n.bitLength();
      if (w < 0 || w >= Type.WORDSIZE) {
        return null;
      }
      return new TNat(BigInteger.valueOf(w));
    }
    return null;
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type bitSize(Type[] tenv, Type a, Type b) {
    if (this == DataName.aref
        || this == DataName.aptr) { // BitSize(ARef (2^(WORDSIZE-w)) a) = w (if 0<=w<=WORDSIZE)
      int w = a.arefWidth(tenv); // (same calculation for aptr)
      return (w > 0) ? new TNat(BigInteger.valueOf(w)) : null;
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
    if (this == DataName.bit) {
      return obdd.Pat.all(a.bitWidth(tenv));
    } else if (this == DataName.nzbit) {
      int w = a.bitWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w) : null;
    } else if (this == DataName.ix) {
      BigInteger n = a.ixBound(tenv);
      if (n.signum() <= 0) {
        return obdd.Pat.empty(0);
      }
      int w = n.bitLength();
      if (w < 0 || w >= Type.WORDSIZE) {
        // TODO: generate an internal error?  or make above internals return null instead?
        return null;
      }
      return obdd.Pat.lessEq(w, n.intValue());
    }
    return null;
  }

  Pat bitPat(Type[] tenv, Type a, Type b) {
    if (this == DataName.aref) {
      int w = a.arefWidth(tenv);
      return (w > 0) ? obdd.Pat.nonzero(w) : null;
    } else if (this == DataName.aptr) {
      int w = a.arefWidth(tenv);
      return (w > 0) ? obdd.Pat.all(w) : null;
    }
    return null;
  }

  /**
   * Find the Bitdata Layout associated with values of this type, if there is one, or else return
   * null. TODO: perhaps this code should be colocated with bitdataName()?
   */
  public BitdataLayout bitdataLayout() {
    return null;
  }

  /** Find the name of the associated struct type, if any. */
  public StructName structName() {
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
    return (this == DataName.stored) ? a.byteSizeStored(tenv) : null;
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    if (this == DataName.array || this == DataName.pad) {
      // ByteSize (Array a b) = a * ByteSize b
      // ByteSize (Pad   a b) = a * ByteSize b
      BigInteger n = a.simplifyNatType(tenv).getNat();
      if (n != null) {
        Type s = b.byteSize(tenv);
        if (s != null) {
          BigInteger m = s.simplifyNatType(null).getNat();
          if (m != null) {
            return new TNat(n.multiply(m));
          }
        }
      }
    }
    return null;
  }

  Type byteSizeStoredRef(Type[] tenv) {
    return null;
  }

  Type byteSizeStoredRef(Type[] tenv, Type a) {
    return null;
  }

  Type byteSizeStoredRef(Type[] tenv, Type a, Type b) {
    return (this == DataName.aref || this == DataName.aptr)
        ? new TNat(Type.numBytes(Type.WORDSIZE))
        : null;
  }

  Tycon canonTycon(TypeSet set) {
    return this;
  }

  /**
   * Attempt to translate a type with this type constructor at its head and some number of arguments
   * on the stack to eliminate newtypes. This will only succeed if the head is a newtype constructor
   * and there are enough arguments; otherwise the call will return null to indicate that no
   * translation was possible.
   */
  Type translate(NewtypeTypeSet set, int args) {
    return null;
  }

  Type specializeTycon(MILSpec spec, Type inst) {
    return inst;
  }

  BitdataRep findRep(BitdataMap m) {
    return null;
  }

  BitdataRep isBitdataRep() {
    return null;
  }

  DataName isDataName() {
    return null;
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
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return null;
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
   * Worker function for funcFromTuple(). Tests to determine if this skeleton is an application of
   * (->>) to a tuple of types, returning either the tuple components in an array or null if there
   * is no match.
   */
  Type[] funcFromTuple1() {
    return null;
  }

  /** Test to determine if this type is the MILArrow, ->>, without any arguments. */
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
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    //  debug.Internal.error("toLLVM not defined for tycon " + this.asType());
    return llvm.Type.vd; // not reached
  }
}
