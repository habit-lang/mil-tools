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
import compiler.Position;
import core.*;
import java.math.BigInteger;

/** Represents the type constructor for a specific bitdata layout. */
public class BitdataLayout extends DataName {

  /** The bitdata type for this layout. */
  private BitdataName bn;

  /** The tagbits for this layout. */
  private BigInteger tagbits;

  /** The list of fields within this bitdata value. */
  private BitdataField[] fields;

  /** A description of all valid bit patterns for this layout. */
  private obdd.Pat pat;

  /** Default constructor. */
  public BitdataLayout(
      Position pos,
      String id,
      Kind kind,
      int arity,
      BitdataName bn,
      BigInteger tagbits,
      BitdataField[] fields,
      obdd.Pat pat) {
    super(pos, id, kind, arity);
    this.bn = bn;
    this.tagbits = tagbits;
    this.fields = fields;
    this.pat = pat;

    Type[] stored = new Type[fields.length];
    for (int i = 0; i < fields.length; i++) {
      stored[i] = fields[i].getType();
    }
    // TODO: use a different id?
    cfuns = new Cfun[] {new Cfun(pos, id, this, 0, new AllocType(stored, this.asType()))};
  }

  public BitdataLayout(
      Position pos,
      String id,
      BitdataName bn,
      BigInteger tagbits,
      BitdataField[] fields,
      obdd.Pat pat) {
    this(pos, id, KAtom.STAR, 0, bn, tagbits, fields, pat);
  }

  void write(TypeWriter tw, int prec, int args) {
    if (args == 0) {
      bn.write(tw, prec, 0);
      tw.write(".");
      tw.write(id);
    } else {
      applic(tw, prec, args, 0);
    }
  }

  public int getWidth() {
    return pat.getWidth();
  }

  public Type bitSize() {
    return bn.bitSize();
  }

  public BitdataField[] getFields() {
    return fields;
  }

  public boolean isNullary() {
    return fields.length == 0;
  }

  private obdd.MaskTestPat maskTest;

  public void setMaskTest(obdd.MaskTestPat maskTest) {
    this.maskTest = maskTest;
  }

  public AllocType cfunType() {
    return new AllocType((isNullary() ? Type.noTypes : new Type[] {this.asType()}), bn.asType());
  }

  /** Return the bit pattern for this object. */
  public obdd.Pat getPat() {
    return pat;
  }

  /** Return the constructor function for this layout. */
  public Cfun getCfun() {
    return cfuns[0];
  }

  public void debugDump() {
    debug.Log.println(
        "Constructor "
            + id
            + ": width "
            + pat.getWidth()
            + " ["
            + Type.numWords(pat.getWidth())
            + " word(s)], tagbits 0x"
            + tagbits.toString(16)
            + " and "
            + fields.length
            + " field(s):");
    for (int i = 0; i < fields.length; i++) {
      debug.Log.print("  " + i + ": ");
      fields[i].debugDump();
    }
    debug.Log.println("  mask-test " + maskTest.toString(id));
  }

  /**
   * Find the Bitdata Layout associated with values of this type, if there is one, or else return
   * null. TODO: perhaps this code should be colocated with bitdataName()?
   */
  public BitdataLayout bitdataLayout() {
    return this;
  }

  DataName canonDataName(TypeSet set) {
    // We do not need to calculate a new version of the type in these cases because we know that
    // none of the
    // Cfun types will change (they are all of the form T.Lab -> T).
    return this;
  }

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a nonrecursive type that only has one constructor).
   */
  public boolean isNewtype() { // Don't treat bitdata types as newtypes
    return false;
  }

  Type specializeTycon(MILSpec spec, Type inst) {
    return inst;
  }

  DataName specializeDataName(MILSpec spec, Type inst) {
    // Do not specialize bitdata types
    return this;
  }

  BitdataRep findRep(BitdataMap m) {
    return null;
  }

  static Cfun[] calcCfuns(BitdataLayout[] layouts) {
    Cfun[] cfuns = new Cfun[layouts.length];
    for (int i = 0; i < layouts.length; i++) {
      BitdataLayout layout = layouts[i];
      cfuns[i] = new Cfun(layout.pos, layout.id, layout.bn, i, layout.cfunType());
    }
    return cfuns;
  }

  /**
   * Generate a block for constructing a value with this bitdata layout and the given constructor
   * function.
   */
  Block makeConsBlock(Cfun cf) {
    Temp[] args = Temp.makeTemps(fields.length);
    Temp v = new Temp();
    return new Block(pos, args, new Bind(v, cfuns[0].withArgs(args), new Done(cf.withArgs(v))));
  }

  /**
   * Generate a block of code for selecting the nth component for a bitdata value with this layout
   * that was built using the given constructor function.
   */
  Block makeSelBlock(Cfun cf, int n) {
    Temp[] args = Temp.makeTemps(1);
    Temp v = new Temp();
    return new Block(
        pos, args, new Bind(v, new Sel(cf, 0, args[0]), new Done(new Sel(cfuns[0], n, v))));
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return bn.repCalc();
  }

  Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c) {
    return c;
  }

  Block maskTestBlock() {
    return maskTestBlock;
  }

  Tail repTransformDataAlloc(RepTypeSet set, Cfun cf, Atom[] args) {
    return new BlockCall(constructorBlock, args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, Cfun cf, int n, Atom a) {
    return fields[n].repTransformSel(set, env, a);
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c) {
    return new Bind(vs, repTransformSel(set, env, cf, n, a), c);
  }

  private Block constructorBlock;

  /** Generate code for a constructor for this layout. */
  public void generateConstructor(Cfun cf) {
    int total = getWidth(); // number of bits in output
    Temp[] params;
    Code code;
    if (total == 0) {
      params = Temp.makeTemps(fields.length); // Can assume all/any fields must have width 0
      code = new Done(new DataAlloc(Cfun.Unit).withArgs());
    } else {
      int n = Type.numWords(total); // number of words in output
      Temp[][] args = new Temp[fields.length][]; // args for each input
      Temp[] ws = Temp.makeTemps(n);
      code = new Done(new Return(Temp.clone(ws)));
      // (Clone ws so that we are free to change its elements without modifying the code in c.)

      // Add code to set each field (assuming that each field is already zero-ed out):
      for (int k = 0; k < fields.length; k++) {
        args[k] = Temp.makeTemps(Type.numWords(fields[k].getWidth()));
        code = fields[k].genUpdateZeroedField(total, ws, args[k], code);
      }

      // Set initial value for tagbits:
      code = initialize(total, ws, tagbits, code);

      // Collect all of the arguments:
      params = Temp.concat(args);
    }
    constructorBlock = new Block(cf.getPos(), "construct_" + cf, params, code);
  }

  /**
   * Prepend the given code sequence with an initializer that sets the Temps in ws to the given bits
   * value.
   */
  static Code initialize(int total, Temp[] ws, BigInteger bits, Code code) {
    return new Bind(ws, new Return(Const.atoms(bits, total)), code);
  }

  static Block generateBitConcat(Position pos, int u, int v) { // :: Bit u -> Bit v -> Bit (u+v)
    Temp[] as = Temp.makeTemps(Type.numWords(u)); // as :: Bit u
    Temp[] bs = Temp.makeTemps(Type.numWords(v)); // bs :: Bit v
    Temp[] ws = Temp.makeTemps(Type.numWords(u + v));
    return new Block(
        pos,
        Temp.append(as, bs),
        initialize(
            u + v,
            ws,
            BigInteger.ZERO,
            genUpdateZeroedBitField(
                0,
                v,
                u + v,
                ws,
                bs,
                genUpdateZeroedBitField(
                    v, u, u + v, ws, as, new Done(new Return(Temp.clone(ws)))))));
  }

  static Code genUpdateZeroedBitField(
      int offset, int width, int total, Temp[] ws, Temp[] as, Code code) {
    return width == 1
        ? BitdataField.genUpdateZeroedFieldBit(offset, width, total, ws, as, code)
        : BitdataField.genUpdateZeroedFieldLo(offset, width, total, ws, as, code);
  }

  private Block maskTestBlock;

  /**
   * Generate the implementation of a mask test predicate for values of this layout using the
   * previously calculated MaskTestPat value. The generated implementation allows the mask and bits
   * to be spread across multiple words. The entry block that is returned has one Word argument for
   * each entry in the mask (and bits) array. The generated code tests one Word at a time using
   * either a suitable call to the appropriate bmaskeq or bmaskneq block, each of which are defined
   * below.
   */
  void generateMaskTest(Cfun cf) {
    int total = getWidth(); // number of bits in output
    BigInteger maskNat = maskTest.getMask();
    BigInteger bitsNat = maskTest.getBits();
    boolean eq = maskTest.getOp();
    if (total < 2) { // special case for width 0 and width 1 types
      Temp[] vs = Temp.makeTemps(1);
      Tail t =
          total == 0 // width 0 case
              ? new Return(FlagConst.True)
              : maskNat.testBit(0) // nonzero mask ==> depends on vs[0]
                  ? ((eq != bitsNat.testBit(0)) ? new Return(vs) : Prim.bnot.withArgs(vs[0]))
                  : new Return(
                      FlagConst.fromBool(eq == bitsNat.testBit(0))); // zero mask ==> const function
      maskTestBlock = new Block(cf.getPos(), "masktest_" + cf, vs, new Done(t));
    } else {
      int n = Type.numWords(total); // number of words in output
      Atom[] mask = Const.atoms(maskNat, total);
      Atom[] bits = Const.atoms(bitsNat, total);
      maskTestBlock = eq ? Block.returnFalse : Block.returnTrue; // base case, if no data to compare

      for (int i = 1; i <= n; i++) {
        Temp[] vs = Temp.makeTemps(i); // i parameters
        Atom[] as = new Atom[] {vs[0], mask[n - i], bits[n - i]};
        Code c;
        if (i == 1) {
          // This branch is used when we are testing the last word of the input, so the final result
          // will be
          // determined exclusively by the result of this comparison.
          c = new Done(new BlockCall(eq ? bmaskneq : bmaskeq, as));
        } else {
          // This branch is used when there are still other words to compare.  Each of these tests
          // uses a call to
          // bmaskeq.  If the result is false, then we can return immediately.  If the result is
          // true, then we will
          // need to consider subsequent words anyway (either to confirm that all bits or equal, or
          // because we are
          // still looking for a place where the input differs from what is required).
          Temp t = new Temp();
          c =
              new Bind(
                  t,
                  new BlockCall(bmaskeq, as),
                  new If(
                      t,
                      new BlockCall(maskTestBlock, Temp.tail(vs)),
                      new BlockCall(eq ? Block.returnTrue : Block.returnFalse, Atom.noAtoms)));
        }
        maskTestBlock = new Block(cf.getPos(), vs, c);
      }
    }
  }

  public static Block bmaskeq = masktestBlock("bmaskeq", Prim.eq);

  public static Block bmaskneq = masktestBlock("bmaskneq", Prim.neq);

  /**
   * Make a block of the following form for implementing a single word masktest predicate with mask
   * m, target t, and equality/inequality test p: b :: [Word, Word, Word] >>= [Flag] b[v, m, t] = w
   * <- and((v, m)); p((w,t))
   */
  static Block masktestBlock(String name, Prim p) {
    Temp[] params = Temp.makeTemps(3);
    Temp w = new Temp();
    return new Block(
        BuiltinPosition.position,
        name,
        params,
        new Bind(w, Prim.and.withArgs(params[0], params[1]), new Done(p.withArgs(w, params[2]))));
  }

  void calculateBitdataBlocks(Cfun cf) {
    generateConstructor(cf);
    generateMaskTest(cf);
    for (int i = 0; i < fields.length; i++) {
      fields[i].calculateBitdataBlocks(cf, this);
    }
  }
}
