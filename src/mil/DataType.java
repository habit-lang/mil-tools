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
import obdd.MaskTestPat;
import obdd.Pat;

/** Represents a conventional algebraic datatype. */
public class DataType extends DataName {

  private Kind kind;

  private int arity;

  /** Default constructor. */
  public DataType(Position pos, String id, Kind kind, int arity) {
    super(pos, id);
    this.kind = kind;
    this.arity = arity;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return kind;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    return arity;
  }

  public void fixKinds() {
    kind = kind.fixKind();
    debug.Log.println(id + " :: " + kind);
  }

  /** A constructor for defining types that have a BuiltinPosition. */
  public DataType(String id, Kind kind, int arity) {
    this(BuiltinPosition.pos, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  /**
   * Print a definition for this type constructor using source level syntax. TODO: Find a more
   * appropriate place for this code ...
   */
  void dumpTypeDefinition(PrintWriter out) {
    if (cfuns != null) {
      out.print("data ");
      out.print(id);
      Type head = kind.makeHead(pos, out, 0, asType());
      out.println();
      for (int i = 0; i < cfuns.length; i++) {
        out.print((i == 0) ? "  = " : "  | ");
        cfuns[i].dump(out, head);
      }
      out.println();
    }
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  Tycon makeCanonTycon(TypeSet set) {
    if (isEnumeration()) { // Do not make copies of enumerations
      set.addTycon(this); // (but still register them as being in use)
      return this;
    }
    DataType newDt =
        new DataType(pos, id, kind, arity); // Make new type, copying attributes of original
    newDt.isRecursive = this.isRecursive;
    set.mapTycon(this, newDt); // Add mapping from old to new
    debug.Log.println("new version of DataType " + id + " is " + newDt);
    newDt.cfuns = new Cfun[cfuns.length]; // Add canonical versions of constructors
    for (int i = 0; i < cfuns.length; i++) {
      newDt.cfuns[i] = cfuns[i].makeCanonCfun(set, newDt);
    }
    return newDt;
  }

  /**
   * Determine whether a given type is an "enumeration", by which we mean that it has no parameters,
   * and no non-nullary constructors. Examples of such types include the Unit type, and simple
   * enumerations like the Booleans. It is not necessary to generate a new version of an enumeration
   * type in canonTycon: the result would be the same as the original, except for the change in
   * name.
   */
  boolean isEnumeration() {
    if (arity != 0) {
      return false;
    } else if (cfuns != null) {
      for (int i = 0; i < cfuns.length; i++) {
        if (cfuns[i] == null || cfuns[i].getArity() != 0) {
          return false;
        }
      }
    }
    return true;
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
    } else if (args == 0 && isSingleton() && this != Tycon.unit) {
      return Tycon.unit.asType(); // Singleton (non unit) type with no arguments
    }
    return null;
  }

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a non-recursive type that only has one constructor).
   */
  boolean isNewtype() {
    return !isRecursive && isSingleConstructor() && cfuns[0].getArity() == 1;
  }

  /** Return true if this is a single constructor type. */
  boolean isSingleConstructor() {
    return cfuns != null && cfuns.length == 1;
  }

  /**
   * Determine if this is a singleton type (i.e., a type with only one value), in which case we will
   * use the Unit type to provide a representation.
   */
  boolean isSingleton() {
    return arity == 0 && cfuns != null && cfuns.length == 1 && cfuns[0].getArity() == 0;
  }

  /**
   * Find out if there is a specialized version of the type with this Tycon as its head and the set
   * of args (canonical) arguments on the stack of this MILSpec object.
   */
  Tycon specInst(MILSpec spec, int args) {
    return (args == arity)
        ? this.specializeDataName(spec, spec.rebuild(this.asType(), args))
        : null;
  }

  private static int count = 0;

  Tycon specializeDataName(MILSpec spec, Type inst) {
    if (spec.containsTycon(this)) { // Already specialized type
      return this;
    }
    TypeSpecs typespecs = spec.getTypeSpecs(this); // Search previous specializations
    for (TypeSpecs ts = typespecs; ts != null; ts = ts.next) {
      if (ts.inst.instMatches(inst)) {
        return ts.dt; // return previously specialized version of this type
      }
    }
    // Make a new specialized version of this type:
    if (isEnumeration()) { // Keep original Unit definition (and other enumerated types)
      spec.addTycon(this);
      return this;
    }
    DataType newDt = new DataType(pos, id + count++, KAtom.STAR, 0);
    newDt.isRecursive = this.isRecursive;
    spec.addTycon(newDt);
    spec.putTypeSpecs(this, new TypeSpecs(inst, newDt, typespecs));
    debug.Log.println(newDt + " is a specialized DataType for " + inst);
    newDt.cfuns = new Cfun[this.cfuns.length];
    for (int i = 0; i < cfuns.length; i++) {
      newDt.cfuns[i] = cfuns[i].makeSpecializeCfun(spec, newDt, inst);
    }
    return newDt;
  }

  /** Find the bitdata representation for this object, or null if there is none. */
  BitdataRep findRep(BitdataMap m) {
    return m.findRep(this);
  }

  /** Determine whether this Tycon is a DataType that is a candidate for bitdata representation. */
  DataType bitdataCandidate() {
    return (arity == 0 && !isRecursive && cfuns != null && cfuns.length > 0) ? this : null;
  }

  /**
   * Try to find a bitdata representation for this data type, adding it to the BitdataMap if
   * successful. The cands and retained lists include all other data types that are currently being
   * considered as candidates; if one of the fields of this data type does not yet have an assigned
   * bit level representation, but it is included in one of these two lists, then perhaps we will
   * find a bitdata representation for that type and be able to come back to this type on a future
   * pass. If the return result is zero, then the receiver should be kept as a candidate for future
   * consideration. Otherwise, a positive result indicates that a new representation was calculated,
   * and a negative result indicates that we have given up on finding a suitable representation. In
   * either of the last two cases, the receiver should be removed from the list of candidates.
   */
  int bitdataEncoding(BitdataMap m, DataTypes cands, DataTypes retained) {
    int numNullary = 0; // count the number of nullary constructors (constructors of arity 0)
    int numNonNullary = 0; // count the number of non-nullary constructors
    int indexNonNullary = 0; // index of the last non-nullary constructor found
    int maxWidth = 0; // maximum width for individual non-nullary constructors
    Pat[][] pats = null; // bit patterns for each constructor

    for (int i = 0; i < cfuns.length; i++) {
      if (cfuns[i].getArity() == 0) {
        numNullary++;
      } else {
        numNonNullary++;
        indexNonNullary = i;
        AllocType at = cfuns[i].getAllocType();
        int n = at.getArity();
        Pat[] fpats = null; // bit patterns for all fields so far
        int width = 0; // width of all fields so far
        for (int j = 0; j < n; j++) {
          Pat p = at.bitPat(j);
          if (p == null) { // No bit pattern yet, but perhaps we have added one to the mapping?
            DataType dt = at.storedType(j).dataType();
            if (dt == null) {
              return (-1); // Remove this dataname from the list of candidates
            } else if ((p = dt.bitPat(m)) == null) {
              // Still no bit pattern, but keep this DataType as a candidate if dn is still a
              // candidate
              return (DataTypes.isIn(dt, cands) || DataTypes.isIn(dt, retained)) ? 0 : (-1);
            }
          }
          if (fpats == null) {
            fpats = new Pat[n];
          }
          fpats[j] = p;
          width += p.getWidth();
        }

        // At this point, we know that all fields for this constructor are bit representable.
        if (width > maxWidth) {
          maxWidth = width;
          if (maxWidth > Word.size()) { // reject this datatype if an encoding will be too long
            return (-1);
          }
        }
        if (pats == null) {
          pats = new Pat[cfuns.length][];
        }
        pats[i] = fpats;
      }
    }

    // At this point, we know that:
    // - There are numNullary constructors with zero arguments and numNonNullary constructors with
    // 1+ arguments
    // - All fields of all constructors are bit representable; if constructor is non-nullary, then
    // the bit patterns
    //   for its fields are in pats[i]
    // - The number of bits needed to represent any value of this type, not including tagbits, is
    // maxWidth

    BitdataRep br = null;
    if (numNullary == 1 && numNonNullary == 1) { // Special case: Recycling - making use of junk
      br = recycling(m, maxWidth, pats[indexNonNullary]);
    } else if (numNullary == 0 && numNonNullary > 0) { // Simple tagging:
      br = simpleTagging(m, maxWidth, pats);
    }
    if (br == null && numNullary > 0) { // General tagging:
      br = generalTagging(m, maxWidth, pats, numNullary, numNonNullary);
    }
    if (br != null) { // If we found a new representation, add it to m
      m.mapTycon(this, br);
      debug.Log.println("added mapping from " + this + " to bitdata " + br);
      return 1;
    }
    // At this point, we have either found a new representation for this DataType and added it to
    // the
    // BitdataMap, or else we have determined that no such representation is possible.  Either way,
    // it
    // should not be kept on the list of candidates for further consideration.
    return (-1);
  }

  DataType dataType() {
    return this;
  }

  Pat bitPat(BitdataMap m) {
    BitdataRep r = findRep(m);
    return (r == null) ? null : r.bitPat();
  }

  /**
   * Try to find a bitdata encoding of a type with one nullary and one non-nullary constructor by
   * using a bit pattern that is not used by the latter as the representation for the former. We
   * refer to this as "recycling" because it is an attempt to make something new from junk. This
   * method is intended to be called only when there are exactly two constructors, one nullary and
   * one non nullary, but it it possible to adapt the approach to more general cases.
   */
  BitdataRep recycling(BitdataMap m, int maxWidth, Pat[] fpats) {
    if (maxWidth <= Word.size()) {
      Pat p = Pat.concat(fpats); // Bit pattern for the non-nullary case
      if (!p.isAll() && !p.isEmpty()) { // If they are not all used ...
        long s = p.smallestOutside(); // then pick the smallest value to represent the nullary cfun
        Pat q = Pat.intmod(maxWidth, s); // Find the bit pattern for that single value

        // Create a new bitdata type T based on the following encoding:
        //     bitdata T/maxWidth = C [ s ]      -- for the nullary constructor
        //                        | D [ fields ] -- for the non nullary constructor

        BitdataRep br = new BitdataRep(pos, id);
        br.setBitSize(new TNat(maxWidth)); // set the width
        br.setPat(p.or(q)); // set the bit pattern, including both constructors

        // Create layouts for two constructors:
        BitdataLayout[] layouts = new BitdataLayout[cfuns.length];
        for (int i = 0; i < cfuns.length; i++) {
          Cfun cf = cfuns[i];
          layouts[i] =
              (cf.getArity() == 0)
                  ? cf.makeLayout(
                      m, br, BigInteger.valueOf(s), 0, null, q, new MaskTestPat(q, false))
                  : cf.makeLayout(m, br, BigInteger.ZERO, 0, fpats, p, new MaskTestPat(p, true));
        }
        br.setCfuns(BitdataLayout.calcCfuns(layouts));
        br.setLayouts(layouts);
        debug.Log.println(
            "Found bitdata representation for " + this + " using recycling strategy:");
        br.debugDump();
        return br;
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
  BitdataRep simpleTagging(BitdataMap m, int maxWidth, Pat[][] pats) {
    int tagWidth = lg(cfuns.length); // how many bits will be needed for tags?
    if (maxWidth + tagWidth <= Word.size()) {
      // Create a new bitdata type T based on the following encoding:
      //
      //     bitdata T/(maxWidth+tagWidth)
      //        = C_0   [ pad_0   | fields_1   | 0   ]
      //        | ...
      //        | C_n-1 [ pad_n-1 | fields_n-1 | n-1 ]

      BitdataRep br = new BitdataRep(pos, id);
      int width = maxWidth + tagWidth;
      Pat p = Pat.empty(width); // to calculate the pattern for all constructors
      br.setBitSize(new TNat(width)); // set the width

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
        layouts[i] = cfuns[i].makeLayout(m, br, tagbits, tagWidth, pats[i], q, mt);
        p = p.or(q);
      }
      br.setPat(p);
      br.setCfuns(BitdataLayout.calcCfuns(layouts));
      br.setLayouts(layouts);
      debug.Log.println(
          "Found bitdata representation for " + this + " using simple tagging strategy:");
      br.debugDump();
      return br;
    }
    return null;
  }

  /**
   * Try to find a bitdata encoding of a type where there is one distinct tag for each non-nullary
   * constructor, and all the nullary constructors (we assume that there is at least one) share a
   * single (zero) tag. If there are no non-nullary constructors, then this just gives a simple
   * enumeration type.
   */
  BitdataRep generalTagging(
      BitdataMap m, int maxWidth, Pat[][] pats, int numNullary, int numNonNullary) {
    int tagWidth = lg(1 + numNonNullary); // how many bits will be needed for tags?
    int nullaryWidth = lg(numNullary); // how many bits needed for nullary tags?
    if (nullaryWidth > maxWidth) { // maxWidth should be big enough to include nullaryWidth
      maxWidth = nullaryWidth;
    }
    if (maxWidth + tagWidth <= Word.size()) {
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

      BitdataRep br = new BitdataRep(pos, id);
      int width = maxWidth + tagWidth;
      Pat p = Pat.empty(width); // to calculate the pattern for all constructors
      br.setBitSize(new TNat(width)); // set the width

      int nullaryTag = 0; // Next available integer for a nullary constructor tag
      int nonNullaryTag = 1; // Next available integer for a non-nullary constructor tag

      BitdataLayout[] layouts = new BitdataLayout[cfuns.length];
      for (int i = 0; i < cfuns.length; i++) {
        Cfun cf = cfuns[i];
        BigInteger tagbits;
        Pat[] fpats;
        Pat q;
        if (cf.getArity() == 0) { // nullary constructor
          tagbits = BigInteger.valueOf(nullaryTag << tagWidth); // tag value
          fpats = null;
          q = Pat.intmod(width, tagbits, 0);
          nullaryTag++;
        } else { // non-nullary constructor
          tagbits = BigInteger.valueOf(nonNullaryTag);
          fpats = pats[i];
          q = Pat.concat(fpats);
          if (tagWidth > 0) {
            q = q.concat(Pat.intmod(tagWidth, tagbits, 0));
          }
          q = q.padLeftTo(width);
          nonNullaryTag++;
        }
        MaskTestPat mt = new MaskTestPat(q, false);
        layouts[i] = cf.makeLayout(m, br, tagbits, tagWidth, fpats, q, mt);
        p = p.or(q);
      }
      br.setPat(p);
      br.setCfuns(BitdataLayout.calcCfuns(layouts));
      br.setLayouts(layouts);
      debug.Log.println(
          "Found bitdata representation for " + this + " using general tagging strategy:");
      br.debugDump();
      return br;
    }
    return null;
  }

  /**
   * Return the minimum number of bits needed to represent n distinct values. For example, lg(1) =
   * 0, lg(2) = 1, lg(3) = lg(4) = 2, lg(5) = lg(6) = lg(7) = lg(8) = 3, etc. This function is
   * included in DataType as a utility function for the bitdataEncoding method, but otherwise has no
   * special relation to the DataType class.
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

  /**
   * Insert this element into the list vs, placing the new element before any list entries that have
   * a smaller key.
   */
  public DataTypes insertInto(DataTypes vs) {
    if (vs == null) {
      return new DataTypes(this, null);
    } else {
      DataTypes prev = null;
      DataTypes curr = vs;
      for (String k = getId();
          curr != null && k.compareTo(curr.head.getId()) > 0;
          curr = curr.next) {
        prev = curr;
      }
      curr = new DataTypes(this, curr);
      if (prev == null) {
        return curr;
      } else {
        prev.next = curr;
        return vs;
      }
    }
  }

  /** Determine whether this Tycon is a DataType that is a candidate for merging. */
  DataType mergeCandidate() {
    return (arity == 0 && cfuns != null && cfuns.length > 0) ? this : null;
  }

  /**
   * Determine whether this DataType is equivalent to that DataType, modulo the mappings in a given
   * MergeMap. As preconditions, we assume that this and that are distinct DataType objects and that
   * neither one of them is currently mapped by m.
   */
  boolean sameMod(DataType that, MergeMap mmap) {
    Cfun[] cs = this.cfuns;
    Cfun[] ds = that.cfuns;

    // Structurally equal datatypes must have the same number of constructors:
    int n = cs.length;
    if (n != ds.length) {
      return false;
    }

    // And each corresponding pair of constructors must have the same arity:
    for (int i = 0; i < n; i++) {
      AllocType ct = cs[i].getAllocType();
      AllocType dt = ds[i].getAllocType();
      int m = ct.getArity();
      if (m != dt.getArity()) {
        return false;
      }
    }

    // If those basic structural constraints are satisfied, then we will try to prove an equivalence
    // by making an
    // assumption that equates them, and then trying to show that the corresponding types are all
    // equivalent.
    mmap.assume(this, that);
    for (int i = 0; i < n; i++) {
      AllocType ct = cs[i].getAllocType();
      AllocType dt = ds[i].getAllocType();
      int m = ct.getArity();
      for (int j = 0; j < m; j++) {
        if (!ct.storedType(j).sameMod(null, dt.storedType(j), null, mmap)) {
          return false;
        }
      }
    }
    return true;
  }

  boolean sameMod(Type t, Type[] tenv, MergeMap mmap) {
    return t.sameDataTypeMod(tenv, this, mmap);
  }

  /**
   * Determine whether this Tycon is equivalent to a specified DataType, modulo a given MergeMap.
   */
  boolean sameDataTypeMod(DataType l, MergeMap mmap) {
    if (l == this) {
      return true;
    } else {
      DataType ldt = mmap.lookup(l);
      DataType rdt = mmap.lookup(this);
      return (ldt == rdt) || ldt.sameMod(rdt, mmap);
    }
  }

  Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c) {
    return new Assert(a, cf.canonCfun(set), c);
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

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return this != unit;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full (canonical)
   * type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    if (args != 0 || arity != 0) {
      debug.Internal.error("DataType toLLVM arity mismatch");
      return llvm.Type.vd;
    }
    return lm.dataPtrTypeCalc(c);
  }
}
