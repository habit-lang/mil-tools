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
import compiler.BuiltinPosition;
import compiler.Failure;
import compiler.Handler;
import compiler.Position;
import core.*;
import java.math.BigInteger;
import obdd.MaskTestPat;
import obdd.Pat;

/** Represents a type constructor that is introduced as an algebraic data type. */
public class DataName extends Tycon {

  protected int arity;

  /** Default constructor. */
  public DataName(Position pos, String id, Kind kind, int arity) {
    super(pos, id, kind);
    this.arity = arity;
  }

  public int getArity() {
    return arity;
  }

  /** Holds the list of constructor functions for this DataName. */
  protected Cfun[] cfuns;

  public Cfun[] getCfuns() {
    return cfuns;
  }

  public void setCfuns(Cfun[] cfuns) {
    this.cfuns = cfuns;
  }

  /** Flag to indicate if this is a recursive type. */
  protected boolean isRecursive = false;

  /** Set the flag to indicate that this datatype is recursive. */
  public void setRecursive() {
    isRecursive = true;
  }

  /** Determine whether this has been marked as a recursive type. */
  public boolean isRecursive() {
    return isRecursive;
  }

  /** A constructor for defining names that have BuiltinPosition. */
  public DataName(String id, Kind kind, int arity) {
    this(BuiltinPosition.position, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  public static final DataName arrow = new DataName("->", Kind.simple(2), 2);

  public static final DataName proc = new DataName("Proc", Kind.simple(1), 1);

  public static final DataName unit = new DataName("Unit", KAtom.STAR, 0);

  private static final Kind natToStar = new KFun(KAtom.NAT, KAtom.STAR);

  private static final Kind areaToStar = new KFun(KAtom.AREA, KAtom.STAR);

  private static final Kind starToArea = new KFun(KAtom.STAR, KAtom.AREA);

  private static final Kind natToAreaToStar = new KFun(KAtom.NAT, areaToStar);

  private static final Kind areaToArea = new KFun(KAtom.AREA, KAtom.AREA);

  private static final Kind natToAreaToArea = new KFun(KAtom.NAT, areaToArea);

  public static final DataName word = new DataName("Word", Kind.simple(0), 0);

  public static final DataName nzword = new DataName("NZWord", Kind.simple(0), 0);

  public static final DataName flag = new DataName("Flag", Kind.simple(0), 0);

  public static final DataName bit = new DataName("Bit", natToStar, 1);

  public static final DataName nzbit = new DataName("NZBit", natToStar, 1);

  public static final DataName ix = new DataName("Ix", natToStar, 1);

  public static final DataName pad = new DataName("Pad", natToAreaToArea, 2);

  public static final DataName array = new DataName("Array", natToAreaToArea, 2);

  public static final DataName aref = new DataName("ARef", natToAreaToStar, 2);

  public static final DataName aptr = new DataName("APtr", natToAreaToStar, 2);

  public static final DataName init = new DataName("Init", areaToStar, 1);

  public static final DataName stored = new DataName("Stored", starToArea, 1);

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return (this == DataName.word || this == DataName.nzword)
        ? Type.TypeWORDSIZE
        : (this == DataName.flag) ? Type.TypeFLAGSIZE : null;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    // TODO: cache these patterns?
    return (this == DataName.word)
        ? Pat.all(Type.WORDSIZE)
        : (this == DataName.nzword)
            ? Pat.nonzero(Type.WORDSIZE)
            : (this == DataName.flag) ? Pat.all(Type.FLAGSIZE) : null;
  }

  public obdd.Pat getPat(int num) {
    debug.Internal.error("DataName does not have a bit pattern");
    return null;
  }

  Tycon canonTycon(TypeSet set) {
    return canonDataName(set);
  }

  DataName canonDataName(TypeSet set) {
    if (cfuns == null) {
      return this;
    } else {
      DataName newDn = set.getDataName(this);
      if (newDn == null) {
        // TODO: if all of the new Cfuns have the same type as the originals, then we don't need to
        // generate a
        // new type here at all ... but this might be hard to implement because we have to add the
        // mapping from
        // this --> newDn before we can generate the Cfuns, and hence before we know if they are the
        // same ...
        newDn = new DataName(pos, id, kind, arity); // copy attributes of original
        set.putDataName(this, newDn); // add mapping from old to new
        set.putDataName(newDn, newDn); // TODO: this is a hack!
        newDn.isRecursive = this.isRecursive;
        newDn.cfuns = new Cfun[this.cfuns.length]; // make specialized versions of cfuns
        debug.Log.println("new version of " + id);
        for (int i = 0; i < this.cfuns.length; i++) {
          newDn.cfuns[i] = this.cfuns[i].remap(set, newDn);
          debug.Log.println("    orig: " + cfuns[i] + " :: " + this.cfuns[i].getAllocType());
          debug.Log.println("    new:  " + newDn.cfuns[i] + " :: " + newDn.cfuns[i].getAllocType());
        }
      }
      return newDn;
    }
  }

  /**
   * Attempt to translate a type with this type constructor at its head and some number of arguments
   * on the stack to eliminate newtypes. This will only succeed if the head is a newtype constructor
   * and there are enough arguments; otherwise the call will return null to indicate that no
   * translation was possible.
   */
  Type translate(NewtypeTypeSet set, int args) {
    if (arity <= args && isNewtype()) { // newtype with enough arguments to expand?
      Type t = cfuns[0].getAllocType().storedType(0); // find skeleton for stored value
      return t.canonType(set.pop(arity), set, args - arity); // find canonical type
    }
    return null;
  }

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a nonrecursive type that only has one constructor).
   */
  public boolean isNewtype() {
    return !isRecursive && isSingleConstructor() && cfuns[0].getArity() == 1;
  }

  /** Return true if this is a single constructor type. */
  public boolean isSingleConstructor() {
    return cfuns != null && cfuns.length == 1;
  }

  Type specializeTycon(MILSpec spec, Type inst) {
    return (cfuns == null) ? inst : specializeDataName(spec, inst).asType();
  }

  DataName specializeDataName(MILSpec spec, Type inst) {
    // Look for a previous specialization of this DataName:
    DataName newDn = spec.get(inst);
    if (newDn != null) {
      return newDn;
    }
    // If there was no previous specialized version, then we should make one!
    newDn = new DataName(pos, id + count++, KAtom.STAR, 0);
    spec.put(inst, newDn); // add to table now so that mapping is visible for cfun types
    spec.put(newDn.asType(), newDn); // the new type is its own specialized form
    newDn.isRecursive = this.isRecursive;
    newDn.cfuns = new Cfun[this.cfuns.length]; // make specialized versions of cfuns
    for (int i = 0; i < this.cfuns.length; i++) {
      newDn.cfuns[i] = this.cfuns[i].specialize(spec, newDn, inst);
    }
    debug.Log.println(newDn.getId() + " is a specialized DataName for " + inst);
    return newDn;
  }

  private static int count = 0;

  /**
   * Try to find a bitdata encoding of a type with one nullary and one non-nullary constructor by
   * using a bit pattern that is not used by the latter as the representation for the former. We
   * refer to this as "recycling" because it is an attempt to make something new from junk. This
   * method is intended to be called only when there are exactly two constructors, one nullary and
   * one non nullary, but it it possible to adapt the approach to more general cases.
   */
  BitdataName recycling(int maxWidth, Pat[] fpats) {
    if (maxWidth <= Type.WORDSIZE) {
      Pat p = Pat.concat(fpats); // Bit pattern for the non-nullary case
      if (!p.isAll()) { // If they are not all used ...
        int s = p.smallestOutside(); // then pick the smallest value to represent the nullary cfun
        Pat q = Pat.intmod(maxWidth, s); // Find the bit pattern for that single value

        // Create a new bitdata type T based on the following encoding:
        //     bitdata T/maxWidth = C [ s ]      -- for the nullary constructor
        //                        | D [ fields ] -- for the non nullary constructor

        BitdataName bn = new BitdataName(pos, id, kind, arity);
        bn.setBitSize(new TNat(maxWidth)); // set the width
        bn.setPat(p.or(q)); // set the bit pattern, including both constructors

        // Create layouts for two constructors:
        BitdataLayout[] layouts = new BitdataLayout[cfuns.length];
        for (int i = 0; i < cfuns.length; i++) {
          Cfun cf = cfuns[i];
          layouts[i] =
              (cf.getArity() == 0)
                  ? cf.makeLayout(bn, BigInteger.valueOf(s), 0, null, q, new MaskTestPat(q, false))
                  : cf.makeLayout(bn, BigInteger.ZERO, 0, fpats, p, new MaskTestPat(p, true));
        }
        bn.setCfuns(BitdataLayout.calcCfuns(layouts));
        bn.setLayouts(layouts);
        debug.Log.println(
            "Found bitdata representation for " + this + " using recycling strategy:");
        bn.debugDump();
        return bn;
      }
    }
    return null;
  }

  /**
   * Try to find a bitdata encoding of a type where every constructor has a distinct tag of some
   * fixed width. We will assume that this method is only used when there are no nullary
   * constructors in the type; the generalTagging scheme can be used instead if there are nullary
   * constructors. In particular, this allows us to assume that none of the pats[i] arrays in this
   * call is null.
   */
  BitdataName simpleTagging(int maxWidth, Pat[][] pats) {
    int tagWidth = lg(cfuns.length); // how many bits will be needed for tags?
    if (maxWidth + tagWidth <= Type.WORDSIZE) {
      // Create a new bitdata type T based on the following encoding:
      //
      //     bitdata T/(maxWidth+tagWidth)
      //        = C_0   [ pad_0   | fields_1   | 0   ]
      //        | ...
      //        | C_n-1 [ pad_n-1 | fields_n-1 | n-1 ]

      BitdataName bn = new BitdataName(pos, id, kind, arity);
      int width = maxWidth + tagWidth;
      Pat p = Pat.empty(width); // to calculate the pattern for all constructors
      bn.setBitSize(new TNat(width)); // set the width

      BitdataLayout[] layouts = new BitdataLayout[cfuns.length];
      for (int i = 0; i < cfuns.length; i++) {
        BigInteger tagbits = BigInteger.valueOf(i); // tag value
        Pat q = Pat.concat(pats[i]); // bit pattern for fields
        MaskTestPat mt;
        if (tagWidth > 0) {
          Pat tag = Pat.intmod(tagWidth, tagbits, 0);
          q = q.concat(tag); // add on bit pattern for tag
          mt = new MaskTestPat(tag.padLeftTo(width), false);
        } else {
          mt = new MaskTestPat(Pat.all(width), false);
        }
        q = q.padLeftTo(width);
        layouts[i] = cfuns[i].makeLayout(bn, tagbits, tagWidth, pats[i], q, mt);
        p = p.or(q);
      }
      bn.setPat(p);
      bn.setCfuns(BitdataLayout.calcCfuns(layouts));
      bn.setLayouts(layouts);
      debug.Log.println(
          "Found bitdata representation for " + this + " using simple tagging strategy:");
      bn.debugDump();
      return bn;
    }
    return null;
  }

  /**
   * Try to find a bitdata encoding of a type where there is one distinct tag for each non-nullary
   * constructor, and all the nullary constructors (we assume that there is at least one) share a
   * single (zero) tag. If there are no non-nullary constructors, then this just gives a simple
   * enumeration type.
   */
  BitdataName generalTagging(int maxWidth, Pat[][] pats, int numNullary, int numNonNullary) {
    int tagWidth = lg(1 + numNonNullary); // how many bits will be needed for tags?
    int nullaryWidth = lg(numNullary); // how many bits needed for nullary tags?
    if (nullaryWidth > maxWidth) { // maxWidth should be big enough to include nullaryWidth
      maxWidth = nullaryWidth;
    }
    if (maxWidth + tagWidth <= Type.WORDSIZE) {
      // Create a new bitdata type T based on the following encoding:
      //
      //     bitdata T'/S = C1   [ pad1 | fields1 | 1 ]  -- \v -> (v&mask)==1
      //                  | ...
      //                  | CM   [ padM | fieldsM | M ]  -- \v -> (v&mask)==M
      //                  | CM+1 [              0 | 0 ]  -- \v -> (v==0)
      //                  | ...
      //                  | CM+N [            N-1 | 0 ]  -- \v -> (v==(N-1)<<lg(1+M))
      //
      // In this example, we assume all the non-nullary constructors come first; in practice, they
      // could
      // be arbitrarily interleaved with the nullary constructors.

      BitdataName bn = new BitdataName(pos, id, kind, arity);
      int width = maxWidth + tagWidth;
      Pat p = Pat.empty(width); // to calculate the pattern for all constructors
      bn.setBitSize(new TNat(width)); // set the width

      int nullaryTag = 0; // Next available integer for a nullary constructor tag
      int nonNullaryTag = 0; // Next available integer for a non-nullary constructor tag

      BitdataLayout[] layouts = new BitdataLayout[cfuns.length];
      for (int i = 0; i < cfuns.length; i++) {
        Cfun cf = cfuns[i];
        BigInteger tagbits;
        Pat[] fpats;
        MaskTestPat mt;
        Pat q;
        if (cf.getArity() == 0) { // nullary constructor
          tagbits = BigInteger.valueOf(nullaryTag << tagWidth); // tag value
          fpats = null;
          q = Pat.intmod(width, tagbits, 0);
          mt = new MaskTestPat(q, false);
          nullaryTag++;
        } else { // non-nullary constructor
          tagbits = BigInteger.valueOf(nonNullaryTag);
          fpats = pats[i];
          q = Pat.concat(fpats);
          if (tagWidth > 0) {
            q = q.concat(Pat.intmod(tagWidth, tagbits, 0));
          }
          q = q.padLeftTo(width);
          mt = new MaskTestPat(Pat.intmod(width, tagbits, 0), false);
          nonNullaryTag++;
        }
        layouts[i] = cf.makeLayout(bn, tagbits, tagWidth, fpats, q, mt);
        p = p.or(q);
      }
      bn.setPat(p);
      bn.setCfuns(BitdataLayout.calcCfuns(layouts));
      bn.setLayouts(layouts);
      debug.Log.println(
          "Found bitdata representation for " + this + " using general tagging strategy:");
      bn.debugDump();
      return bn;
    }
    return null;
  }

  /**
   * Return the minimum number of bits needed to represent n distinct values. For example, lg(1) =
   * 0, lg(2) = 1, lg(3) = lg(4) = 2, lg(5) = lg(6) = lg(7) = lg(8) = 3, etc. This function is
   * included in DataName as a utility function for the bitdataEncoding method, but otherwise has no
   * special relation to the DataName class.
   */
  static int lg(int n) {
    if (n <= 1) {
      return 0;
    } else {
      int i = 0;
      for (n--; n > 0; i++) {
        n = n >>> 1;
      }
      return i;
    }
  }

  /** Representation vector for bitdata types of width one. */
  public static final Type[] flagRep = new Type[] {flag.asType()};

  /** Representation vector for Ix, ARef, APtr, etc. types that fit in a single word. */
  public static final Type[] wordRep = new Type[] {word.asType()};

  /** Representation vector for NZBit types that fit in a single word. */
  public static final Type[] nzwordRep = new Type[] {nzword.asType()};

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return null;
    // TODO: The current method of eliminating the runtime representation for singleton types does
    // not work
    // as intended, so the test below is disabled until we figure out a better option ...
    // No runtime representation needed for singleton types, otherwise no change in representation.
    // return isSingleton() ? Type.noTypes : null;
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return (this == DataName.bit)
        ? a.simplifyNatType(null).bitvectorRep()
        : (this == DataName.nzbit)
            ? a.simplifyNatType(null).nzbitvectorRep()
            : (this == DataName.ix) ? DataName.wordRep : null;
  }

  /**
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return (this == DataName.aref || this == DataName.aptr) ? DataName.wordRep : null;
  }

  /**
   * Determine if this is a singleton type (i.e., a type with only one value), in which case no
   * representation is required.
   */
  boolean isSingleton() {
    return cfuns != null && cfuns.length == 1 && cfuns[0].getArity() == 0;
  }

  Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c) {
    return new Assert(a, cf.canonCfun(set), c);
  }

  Block maskTestBlock(int num) {
    debug.Internal.error("DataName does not have a mask test block");
    return null;
  }

  Tail repTransformDataAlloc(RepTypeSet set, Cfun cf, Atom[] args) {
    return cf.canonCfun(set).withArgs(args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, Cfun cf, int n, Atom a) {
    return new Sel(cf.canonCfun(set), n, a);
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c) {
    int offset = cf.repOffset(n);
    if (vs.length > 0) {
      if (vs.length == 1) {
        return new Bind(vs, new Sel(cf.canonCfun(set), offset, a), c);
      } else {
        // A sequence of Sel operations are needed in this situation to load each of the components
        // in the
        // representation for the value that was loaded from offset n in the original version of the
        // code
        // (i.e., before the representation transformation).
        for (int k = vs.length; 0 < k--; ) {
          c = new Bind(new Temp[] {vs[k]}, new Sel(cf.canonCfun(set), offset + k, a), c);
        }
      }
    }
    return c;
  }

  public void addCfunsTo(Handler handler, MILEnv milenv) {
    for (int i = 0; i < cfuns.length; i++) {
      try {
        milenv.addCfunAndTop(cfuns[i]);
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, TypeMap tm, int args) {
    if (args != 0 || arity != 0) {
      //    debug.Internal.error("DataName toLLVM arity mismatch");
      return llvm.Type.vd;
    }
    return tm.dataPtrTypeCalc(c);
  }
}
