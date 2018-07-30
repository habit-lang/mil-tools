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

  public static final DataName flag = new DataName("Flag", Kind.simple(0), 0);

  public static final DataName bit = new DataName("Bit", natToStar, 1);

  public static final DataName ix = new DataName("Ix", natToStar, 1);

  public static final DataName pad = new DataName("Pad", natToAreaToArea, 2);

  public static final DataName array = new DataName("Array", natToAreaToArea, 2);

  public static final DataName aref = new DataName("ARef", natToAreaToStar, 2);

  public static final DataName aptr = new DataName("APtr", natToAreaToStar, 2);

  public static final DataName init = new DataName("Init", areaToStar, 1);

  public static final DataName stored = new DataName("Stored", starToArea, 1);

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return (this == DataName.word)
        ? Type.TypeWORDSIZE
        : (this == DataName.flag) ? Type.TypeFLAGSIZE : null;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    // TODO: cache these patterns?
    return (this == DataName.word)
        ? obdd.Pat.all(Type.WORDSIZE)
        : (this == DataName.flag) ? obdd.Pat.all(Type.FLAGSIZE) : null;
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

  /** Representation vector for bitdata types of width one. */
  public static final Type[] flagRep = new Type[] {flag.asType()};

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    if (isSingleton()) { // No runtime representation needed for singleton types
      return Type.noTypes;
    } else {
      return null; // Default behavior: no change in representation
    }
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return (this == DataName.bit)
        ? a.simplifyNatType(null).bitvectorRep()
        : (this == DataName.ix) ? Type.words(1) : null;
  }

  /**
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return (this == DataName.aref || this == DataName.aptr) ? Type.words(1) : null;
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
      //        debug.Internal.error("DataName toLLVM arity mismatch");
      return llvm.Type.vd;
    }
    return tm.dataPtrTypeCalc(c);
  }
}
