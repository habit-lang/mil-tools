/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

/** Names for constructor functions, each of which is associated with a specific DataName value. */
public class Cfun extends Name {

  /** The name of the type for this constructor. */
  private DataName dn;

  /** The number of this constructor. */
  private int num;

  /** The type of the associated allocator. */
  private AllocType allocType;

  /** Default constructor. */
  public Cfun(Position pos, String id, DataName dn, int num, AllocType allocType) {
    super(pos, id);
    this.dn = dn;
    this.num = num;
    this.allocType = allocType;
  }

  public int getNum() {
    return num;
  }

  public AllocType getAllocType() {
    return allocType;
  }

  public int getArity() {
    return allocType.getArity();
  }

  public DataName getDataName() {
    return dn;
  }

  /**
   * Get the array of constructor functions associated with this object, or return null if this is
   * not a DataName.
   */
  public Cfun[] getCfuns() {
    return dn.getCfuns();
  }

  /** A constructor for defining Cfuns that have BuiltinPosition. */
  public Cfun(String id, DataName dn, int num, AllocType allocType) {
    this(BuiltinPosition.pos, id, dn, num, allocType);
    // TODO: add this constructor to some builtin environment here?
  }

  /** Represents the allocator type for the Func constructor function. */
  private static final AllocType funcType =
      new PolyAllocType(
          new Type[] {Type.milfunTuple(Type.gen(0), Type.gen(1))},
          Type.fun(Type.gen(0), Type.gen(1)),
          Prefix.star_star);

  /** Represents the constructor function for the Func type. */
  public static final Cfun Func = new Cfun("Func", Tycon.arrow, 0, funcType);

  static {
    Tycon.arrow.setCfuns(new Cfun[] {Cfun.Func});
  }

  /** Represents allocator type forall a::*. {[] ->> [a]}.Proc a. */
  private static final AllocType procType =
      new PolyAllocType(
          new Type[] {Type.milfun(Type.empty, Type.tuple(Type.gen(0)))},
          Type.proc(Type.gen(0)),
          Prefix.star);

  /** Represents the constructor function for the Proc type. */
  public static final Cfun Proc = new Cfun("Proc", Tycon.proc, 0, procType);

  static {

    // debug.Log.println("Proc alloc type is: " + Proc.allocType);
    Tycon.proc.setCfuns(new Cfun[] {Cfun.Proc});
  }

  /** Represents the allocator type for the Unit constructor. */
  private static final AllocType unitType = new AllocType(Tycon.unit.asType());

  /** Represents the constructor function for the Unit type. */
  public static final Cfun Unit = new Cfun("Unit", Tycon.unit, 0, unitType);

  static {
    Tycon.unit.setCfuns(new Cfun[] {Cfun.Unit});
  }

  /** Represents the type skeleton Ptr a. */
  private static final Type ptrA = new TAp(Tycon.ptr.asType(), Type.gen(0));

  /** Represents the allocator type forall a::area. {}(Ptr a). */
  private static final AllocType nullType = new PolyAllocType(Type.noTypes, ptrA, Prefix.area);

  /** Represents the constructor Null :: Ptr a. */
  public static final Cfun Null = new Cfun("Null", Tycon.ptr, 0, nullType);

  /** Represents the allocator type forall a::area. {Ref a}(Ptr a). */
  private static final AllocType refType =
      new PolyAllocType(new Type[] {Type.ref(Type.gen(0))}, ptrA, Prefix.area);

  /** Represents the constructor function Ref :: Ref a -> Ptr a. */
  public static final Cfun Ref = new Cfun("Ref", Tycon.ptr, 1, refType);

  static {
    Tycon.ptr.setCfuns(new Cfun[] {Cfun.Null, Cfun.Ref});
    Tycon.phys.setCfuns(new Cfun[0]);
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataType bitdataType() {
    return dn.bitdataType();
  }

  /** Return the bit pattern for this object. */
  public obdd.Pat getPat() {
    return dn.getPat(num);
  }

  void dump(PrintWriter out, Type head) {
    out.print(id);
    allocType.instantiate().dump(out, head);
    out.println();
  }

  /** Create a call with the given arguments. */
  public Call withArgs(Atom[] args) {
    return new DataAlloc(this, args);
  }

  /** Create a call with no arguments. */
  public Call withArgs() {
    return withArgs(Atom.noAtoms);
  }

  /** Create a call with a single argument. */
  public Call withArgs(Atom a) {
    return withArgs(new Atom[] {a});
  }

  /**
   * Points to a top level definition corresponding to a function value (i.e., closure) for this
   * constructor.
   */
  private TopLevel topLevel;

  /** Return the top-level constructor function for this constructor. */
  TopLevel getTopLevel() {
    return topLevel;
  }

  /**
   * Set the topLevel associated with this constructor function to and appropriate value, supported
   * by definitions generated using the maker() method.
   */
  void addTopLevel() {
    topLevel = new TopLevel(pos, id, new DataAlloc(this).maker(pos, getArity()));
    topLevel.setDeclared(0, allocType.toScheme());
  }

  /**
   * Return an Atom (actually a Top) that can be used to reference the higher-order function value
   * for this constructor function.
   */
  public Top getTop() {
    return new TopDef(topLevel, 0);
  }

  /**
   * Return the declared type for (the higher-order function version of) this constructor function.
   */
  public Scheme getDeclared() {
    return topLevel.getDeclared(0);
  }

  AllocType instantiate() {
    return allocType.instantiate();
  }

  /**
   * Returns the allocator type for this constructor function, also checking that the constructor
   * has an nth component. In other words, all of the code for type checking a Sel that needs needs
   * access to fields of the Cfun is captured in this method.
   */
  AllocType checkSelIndex(Position pos, int n) throws Failure {
    if (n < 0 || n >= getArity()) {
      throw new Failure(
          pos,
          "selector index " + n + " is out of range for " + id + ", which has arity " + getArity());
    }
    return allocType;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
   */
  int summary() {
    return id.hashCode();
  }

  /**
   * Make a new version of this constructor function using types that are canonical wrt the given
   * set.
   */
  Cfun makeCanonCfun(TypeSet set, DataName newDn) {
    Cfun cf = new Cfun(pos, id, newDn, num, allocType.canonAllocType(set));
    debug.Log.println("    orig: " + this + " :: " + this.getAllocType());
    debug.Log.println("    new:  " + cf + " :: " + cf.getAllocType());
    return cf;
  }

  /**
   * Return the canonical version of this constructor function (by looking it up in the associated
   * canonical Tycon, which---by an invariant not currently captured in the types---must be a
   * DataName).
   */
  Cfun canonCfun(TypeSet set) {
    return dn.canonTycon(set).getCfuns()[num];
  }

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a non-recursive type that only has one constructor).
   */
  boolean isNewtype() {
    return num == 0 && dn.isNewtype();
    // The first conjunct is implied by the second, but we include it for
    // clarity and to avoid a (likely more expensive) test on dn in many
    // cases.
  }

  /** Return true if this is a single constructor type. */
  boolean isSingleConstructor() {
    return dn.isSingleConstructor();
  }

  /**
   * Determine if this is a singleton type (i.e., a type with only one value), in which case we will
   * use the Unit type to provide a representation.
   */
  boolean isSingleton() {
    return dn.isSingleton();
  }

  /** A counter used to generate numeric suffixes on new Cfun names. */
  private static int count = 0;

  /** Create a specialized version of this constructor function. */
  Cfun makeSpecializeCfun(MILSpec spec, DataName newDn, Type inst) {
    AllocType at = allocType.instantiate();
    if (!at.resultMatches(inst)) {
      debug.Internal.error("failed to specialize allocType " + this + " :: " + at + " to " + inst);
    }
    return new Cfun(pos, id + count++, newDn, num, at.canonAllocType(spec));
  }

  /**
   * Find the specialized version of this constructor function at the instance described by the
   * given AllocType and in the context of the substitution s.
   */
  Cfun specializeCfun(MILSpec spec, AllocType type, TVarSubst s) {
    Type inst = type.resultType().apply(s).canonArgs(null, spec, 0);
    return dn.specializeDataName(spec, inst).getCfuns()[num];
  }

  /**
   * Find the synthesized bitdata representation for this object in the given BitdataMap, or else
   * return null if there is none.
   */
  BitdataRep findRep(BitdataMap m) {
    return dn.findRep(m);
  }

  /**
   * Build a bitdata layout corresponding to this constructor function with the additional
   * parameters that specify the associated bitdata type (bt), tagbits, offset (for rightmost
   * field), bit patterns for fields (fpats), bit pattern p for the full constructor, and a suitably
   * chosen mask test predicate, mt.
   */
  BitdataLayout makeLayout(
      BitdataMap m,
      BitdataType bt,
      BigInteger tagbits,
      int offset,
      Pat[] fpats,
      Pat p,
      MaskTestPat mt) {
    int n = getArity();
    BitdataField[] fields = new BitdataField[n];
    for (int i = n - 1; i >= 0; i--) {
      int width = fpats[i].getWidth();
      fields[i] =
          new BitdataField(pos, id + i, allocType.storedType(i).canonType(m), offset, width);
      offset += width;
    }
    BitdataLayout layout = new BitdataLayout(pos, id, bt, tagbits, fields, p);
    layout.addCfun();
    layout.setMaskTest(mt);
    return layout;
  }

  /**
   * Return the ith main constructor function in the bitdata type that is associated with this
   * constructor.
   */
  Cfun bitdataRewrite(BitdataRep r) {
    return r.bitdataCfun(num);
  }

  /** Return the block that implements this constructor under the given BitdataRep. */
  Block bitdataConsBlock(BitdataRep r) {
    return r.bitdataConsBlock(num);
  }

  /**
   * Return the block that implements selection of the nth component of this constructor under the
   * given BitdataRep.
   */
  Block bitdataSelBlock(BitdataRep r, int n) {
    return r.bitdataSelBlock(num, n);
  }

  /**
   * Use the given MergeMap to determine how this constructor function should be translated in the
   * output program.
   */
  Cfun lookup(MergeMap mmap) {
    // Find translation for this datatype, if any
    DataType dt = mmap.mappingFor(dn);
    return (dt == null) ? this : dt.cfuns[num];
  }

  Code repTransformAssert(RepTypeSet set, Atom a, Code c) {
    return (this == Ref) ? c : dn.repTransformAssert(set, this, a, c);
  }

  /** Find block that implements the mask test predicate for this constructor/layout. */
  Block maskTestBlock() {
    return dn.maskTestBlock(num);
  }

  Tail repTransformDataAlloc(RepTypeSet set, RepEnv env, Atom[] args) {
    return (this == Null)
        ? new Return(Word.Zero)
        : (this == Ref) ? new Return(args) : dn.repTransformDataAlloc(set, this, args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, int n, Atom a) {
    return (this == Ref)
        ? new Return(a.repAtom(set, env))
        : dn.repTransformSel(set, env, this, n, a);
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, int n, Atom a, Code c) {
    return dn.repTransformSel(set, env, vs, this, n, a, c);
  }

  /**
   * Calculate the offset of the field that was originally stored at offset n. The offset may change
   * if there were changes in the number of words used for any of the preceding fields.
   */
  int repOffset(int n) {
    return allocType.repOffset(n);
  }

  /** Returns the LLVM type for value that is returned by a function. */
  llvm.Type retType(LLVMMap lm) {
    return lm.toLLVM(allocType.resultType());
  }

  /**
   * Calculate a structure type describing the layout of a data value built with a specific
   * constructor.
   */
  llvm.Type cfunLayoutTypeCalc(LLVMMap lm) {
    return allocType.cfunLayoutTypeCalc(lm);
  }
}
