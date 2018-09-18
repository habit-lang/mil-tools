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
import compiler.Position;
import core.*;
import java.io.PrintWriter;
import java.math.BigInteger;
import obdd.MaskTestPat;
import obdd.Pat;

/** Names for constructor functions, each of which is associated with a specific DataName value. */
public class Cfun extends Name {

  private DataName dn;

  private int num;

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

  public Cfun[] getCfuns() {
    return dn.getCfuns();
  }

  /** A constructor for defining Cfuns that have BuiltinPosition. */
  public Cfun(String id, DataName dn, int num, AllocType allocType) {
    this(BuiltinPosition.pos, id, dn, num, allocType);
    // TODO: add this constructor to some builtin environment here?
  }

  private static final AllocType funcType =
      new PolyAllocType(
          new Type[] {Type.milfunTuple(Type.gen(0), Type.gen(1))},
          Type.fun(Type.gen(0), Type.gen(1)),
          new Prefix(new Tyvar[] {Tyvar.star, Tyvar.star}));

  public static final Cfun Func = new Cfun("Func", Tycon.arrow, 0, funcType);

  static {
    Tycon.arrow.setCfuns(new Cfun[] {Cfun.Func});
  }

  private static final AllocType procType =
      new PolyAllocType(
          new Type[] {Type.milfun(Type.empty, Type.tuple(Type.gen(0)))},
          Type.procOf(Type.gen(0)),
          new Prefix(new Tyvar[] {Tyvar.star}));

  public static final Cfun Proc = new Cfun("Proc", Tycon.proc, 0, procType);

  static {

    // debug.Log.println("Proc alloc type is: " + Proc.allocType);
    Tycon.proc.setCfuns(new Cfun[] {Cfun.Proc});
  }

  private static final AllocType unitType = new AllocType(Tycon.unit.asType());

  public static final Cfun Unit = new Cfun("Unit", Tycon.unit, 0, unitType);

  static {
    Tycon.unit.setCfuns(new Cfun[] {Cfun.Unit});
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

  public Top getTop() {
    return new TopDef(topLevel, 0);
  }

  public Scheme getDeclared() {
    return topLevel.getDeclared(0);
  }

  public Call withArgs(Atom[] args) {
    return new DataAlloc(this).withArgs(args);
  }

  public Call withArgs() {
    return withArgs(Atom.noAtoms);
  }

  public Call withArgs(Atom a) {
    return withArgs(new Atom[] {a});
  }

  public Call withArgs(Atom a, Atom b) {
    return withArgs(new Atom[] {a, b});
  }

  public Call withArgs(Atom a, long n) {
    return withArgs(new Atom[] {a, new Word(n)});
  }

  public Call withArgs(long n, Atom b) {
    return withArgs(new Atom[] {new Word(n), b});
  }

  /** Points to a top level definition corresponding to an LC function for this constructor. */
  private TopLevel topLevel;

  /** Return the top-level LC constructor function corresponding to this constructor. */
  public TopLevel getTopLevel() {
    return topLevel;
  }

  public void addTopLevel() {
    topLevel = new TopLevel(pos, id, new DataAlloc(this).maker(pos, getArity()));
    topLevel.setDeclared(0, allocType.toScheme());
  }

  AllocType instantiate() {
    return allocType.instantiate();
  }

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
   * equivalent program fragments have the same summary value.
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
   * canonical DataName).
   */
  Cfun canonCfun(TypeSet set) {
    return dn.canonDataName(set).getCfuns()[num];
  }

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a nonrecursive type that only has one constructor).
   */
  public boolean isNewtype() {
    return num == 0 && dn.isNewtype();
    // The first conjunct is implied by the second, but we include it for clarity and
    // to avoid a (likely more expensive) test on dn in many cases.
  }

  /** Return true if this is a single constructor type. */
  public boolean isSingleConstructor() {
    return dn.isSingleConstructor();
  }

  /**
   * Determine if this is a singleton type (i.e., a type with only one value), in which case we will
   * use the Unit type to provide a representation.
   */
  boolean isSingleton() {
    return dn.isSingleton();
  }

  private static int count = 0;

  Cfun makeSpecializeCfun(MILSpec spec, DataName newDn, Type inst) {
    AllocType at = allocType.instantiate();
    if (!at.resultMatches(inst)) {
      debug.Internal.error("failed to specialize allocType " + this + " :: " + at + " to " + inst);
    }
    return new Cfun(pos, id + count++, newDn, num, at.canonAllocType(spec));
  }

  Cfun specializeCfun(MILSpec spec, AllocType type, TVarSubst s) {
    Type inst = type.resultType().apply(s).canonArgs(null, spec, 0);
    return dn.specializeDataName(spec, inst).getCfuns()[num];
  }

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

  Code repTransformAssert(RepTypeSet set, Atom a, Code c) {
    return dn.repTransformAssert(set, this, a, c);
  }

  Block maskTestBlock() {
    return dn.maskTestBlock(num);
  }

  Tail repTransformDataAlloc(RepTypeSet set, Atom[] args) {
    return dn.repTransformDataAlloc(set, this, args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, int n, Atom a) {
    return dn.repTransformSel(set, env, this, n, a);
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

  llvm.Type dataPtrType(LLVMMap lm) {
    return lm.toLLVM(dn.asType());
  }

  /**
   * Calculate a structure type describing the layout of a data value built with a specific
   * constructor.
   */
  llvm.Type cfunLayoutTypeCalc(LLVMMap lm) {
    return allocType.cfunLayoutTypeCalc(lm);
  }
}
