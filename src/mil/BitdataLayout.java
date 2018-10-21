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
import java.io.PrintWriter;
import java.math.BigInteger;

/** Represents a type constructor for a specific bitdata layout. */
public class BitdataLayout extends DataName {

  /** The bitdata type for this layout. */
  private BitdataType bt;

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
      BitdataType bt,
      BigInteger tagbits,
      BitdataField[] fields,
      obdd.Pat pat) {
    super(pos, id);
    this.bt = bt;
    this.tagbits = tagbits;
    this.fields = fields;
    this.pat = pat;
  }

  public BitdataField[] getFields() {
    return fields;
  }

  private obdd.MaskTestPat maskTest;

  public void setMaskTest(obdd.MaskTestPat maskTest) {
    this.maskTest = maskTest;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return KAtom.STAR;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    return fields.length;
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    if (args == 0) {
      bt.write(tw, prec, 0);
      tw.write(".");
      tw.write(id);
    } else {
      applic(tw, prec, args, 0);
    }
  }

  /** Find the Bitdata Layout associated with values of this type, or else return null. */
  public BitdataLayout bitdataLayout() {
    return this;
  }

  /** Return the bit pattern for this object. */
  public obdd.Pat getPat() {
    return pat;
  }

  /**
   * Add the constructor for this BitdataLayout using the details specified in the array of fields.
   */
  public void addCfun() {
    Type[] stored = new Type[fields.length];
    for (int i = 0; i < fields.length; i++) {
      stored[i] = fields[i].getType();
    }
    cfuns =
        new Cfun[] {new Cfun(pos, bt + "." + id, this, 0, new AllocType(stored, this.asType()))};
  }

  /** Return the constructor function for this layout. */
  public Cfun getCfun() {
    return cfuns[0];
  }

  public int getWidth() {
    return pat.getWidth();
  }

  public boolean isNullary() {
    return fields.length == 0;
  }

  public AllocType cfunType() {
    return new AllocType((isNullary() ? Type.noTypes : new Type[] {this.asType()}), bt.asType());
  }

  public void debugDump() {
    debug.Log.println(
        "Constructor "
            + id
            + ": width "
            + pat.getWidth()
            + " ["
            + Word.numWords(pat.getWidth())
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

  /** Print a definition for this bitdata type using source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    /* Do not show BitdataLayouts as separate types. */
  }

  void dumpBitdataLayout(PrintWriter out) {
    out.print(id);
    out.print(" [ ");
    int offset = pat.getWidth();
    for (int i = 0; i < fields.length; i++) {
      offset = fields[i].dumpBitdataField(out, tagbits, offset);
      if (offset > 0) { // still bits/fields to show ?
        out.print(" | ");
      }
    }
    if (offset > 0) { // low order tagbits
      out.print(Bits.toString(tagbits, offset));
    }
    out.println(" ]");
    out.print("    -- ");
    out.print(maskTest.toString(id));
    out.println();
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  Tycon makeCanonTycon(TypeSet set) {
    bt.canonTycon(set); // force the calculation of the associated canonical BitdataType ...
    return set.mapsTyconTo(
        this); // ... to ensure that the associated layout has been added to the set
    // (even if it hasn't been fully initialized yet).
  }

  /** Make a new version of this bitdata layout using types that are canonical wrt the given set. */
  BitdataLayout makeCanonBitdataLayout(TypeSet set, BitdataType newBt) {
    BitdataLayout nlayout =
        new BitdataLayout(pos, id, newBt, tagbits, fields, pat); // use old fields to begin ...
    nlayout.maskTest = maskTest;
    nlayout.maskTestBlock = maskTestBlock;
    nlayout.constructorBlock = constructorBlock;
    set.mapTycon(this, nlayout); // Add mapping from old layout to canonical version
    return nlayout;
  }

  /**
   * Replace the list of fields in this layout with canonical versions. Separated from
   * makeCanonBitdataLayout so that we can build all of the layouts (and add their new
   * implementations to the tyconMap) before we attempt to calculate canonical versions of field
   * types.
   */
  void makeCanonFields(TypeSet set) {
    BitdataField[] nfields = new BitdataField[fields.length];
    for (int i = 0; i < fields.length; i++) {
      nfields[i] = fields[i].makeCanonBitdataField(set);
    }
    fields = nfields; // Switch to the new list of fields
    addCfun(); // and then calculate the new Cfun
  }

  static Cfun[] calcCfuns(BitdataLayout[] layouts) {
    Cfun[] cfuns = new Cfun[layouts.length];
    for (int i = 0; i < layouts.length; i++) {
      BitdataLayout layout = layouts[i];
      cfuns[i] = new Cfun(layout.pos, layout.id, layout.bt, i, layout.cfunType());
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
    return bt.repCalc();
  }

  Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c) {
    return c;
  }

  Block maskTestBlock() {
    return maskTestBlock;
  }

  Tail repTransformDataAlloc(RepTypeSet set, Cfun cf, Atom[] args) {
    return construct(args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, Cfun cf, int n, Atom a) {
    return fields[n].repTransformSel(set, env, a);
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c) {
    return new Bind(vs, repTransformSel(set, env, cf, n, a), c);
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return bt.bitSize();
  }

  /**
   * Determine whether a selector from this layout will (in general) require a masking operation.
   */
  boolean selectNeedsMask() {
    return tagbits.signum() != 0 || fields.length != 1;
  }

  private Block constructorBlock;

  /** Return a call to the constructor block for this layout. */
  public BlockCall construct(Atom[] args) {
    return new BlockCall(constructorBlock, args);
  }

  /** Generate code for a constructor for this layout. */
  public void generateConstructor(Cfun cf) {
    int total = getWidth(); // number of bits in output
    Temp[] params;
    Code code;
    if (total == 0) {
      params = Temp.makeTemps(fields.length); // Can assume all/any fields must have width 0
      code = new Done(new DataAlloc(Cfun.Unit).withArgs());
    } else {
      int n = Word.numWords(total); // number of words in output
      Temp[][] args = new Temp[fields.length][]; // args for each input
      Temp[] ws = Temp.makeTemps(n);
      code = new Done(new Return(Temp.clone(ws)));
      // (Clone ws so that we are free to change its elements without modifying the code in c.)

      // Add code to set each field (assuming that each field is already zero-ed out):
      for (int k = 0; k < fields.length; k++) {
        int w = fields[k].getWidth();
        if (w > 0) { // add arguments for this field, and code to insert into result
          args[k] = Temp.makeTemps(Word.numWords(w));
          code = fields[k].genUpdateZeroedField(total, ws, args[k], code);
        } else { // create an argument variable, but no code
          args[k] = Temp.makeTemps(1);
        }
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
    Temp[] as = Temp.makeTemps(Word.numWords(u)); // as :: Bit u
    Temp[] bs = Temp.makeTemps(Word.numWords(v)); // bs :: Bit v
    Temp[] ws = Temp.makeTemps(Word.numWords(u + v));
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
              ? new Return(Flag.True)
              : maskNat.testBit(0) // nonzero mask ==> depends on vs[0]
                  ? ((eq != bitsNat.testBit(0)) ? new Return(vs) : Prim.bnot.withArgs(vs[0]))
                  : new Return(
                      Flag.fromBool(eq == bitsNat.testBit(0))); // zero mask ==> const function
      maskTestBlock = new Block(cf.getPos(), "masktest_" + cf, vs, new Done(t));
    } else {
      int n = Word.numWords(total); // number of words in output
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
        BuiltinPosition.pos,
        name,
        params,
        new Bind(w, Prim.and.withArgs(params[0], params[1]), new Done(p.withArgs(w, params[2]))));
  }

  void calculateBitdataBlocks(Cfun cf) {
    generateConstructor(cf);
    generateMaskTest(cf);
    for (int i = 0; i < fields.length; i++) {
      fields[i].calculateBitdataBlocks(this);
    }
  }
}
