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
import compiler.Position;
import core.*;
import java.io.PrintWriter;
import obdd.Pat;

/** Represents a type constructor that is introduced as a bitdata type. */
public class BitdataType extends DataName {

  /** Default constructor. */
  public BitdataType(Position pos, String id) {
    super(pos, id);
  }

  protected BitdataLayout[] layouts;

  public BitdataLayout[] getLayouts() {
    return layouts;
  }

  public void setLayouts(BitdataLayout[] layouts) {
    this.layouts = layouts;

    for (int i = 0; i < layouts.length; i++) {
      layouts[i].calculateBitdataBlocks(cfuns[i]);
    }
  }

  private Type bitSize;

  public void setBitSize(Type bitSize) {
    this.bitSize = bitSize;
  }

  private Pat pat;

  public void setPat(Pat pat) {
    this.pat = pat;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return KAtom.STAR;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    return 0;
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataType bitdataType() {
    return this;
  }

  /** Return the bit pattern for this object. */
  public obdd.Pat getPat() {
    return pat;
  }

  public obdd.Pat getPat(int num) {
    return layouts[num].getPat();
  }

  public void debugDump() {
    debug.Log.println(
        "bitdata " + id + ": size " + bitSize + ", with " + layouts.length + " layout(s):");
    for (int i = 0; i < layouts.length; i++) {
      debug.Log.print("  " + i + ": ");
      layouts[i].debugDump();
    }
  }

  /** Print a definition for this bitdata type using source level syntax. */
  void dumpTypeDefinition(PrintWriter out) {
    out.print("bitdata ");
    out.print(id);
    out.print(" /");
    out.println(bitSize.toString());
    for (int i = 0; i < layouts.length; i++) {
      out.print((i == 0) ? "  = " : "  | ");
      layouts[i].dumpBitdataLayout(out);
    }
    out.println();
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
    BitdataType newBt = new BitdataType(pos, id); // Make new type, copying attributes of original
    newBt.bitSize = bitSize;
    newBt.pat = pat;
    set.mapTycon(this, newBt); // Add mapping from old to new
    debug.Log.println("new version of BitdataType " + id + " is " + newBt);
    newBt.layouts = new BitdataLayout[layouts.length];
    for (int i = 0; i < layouts.length; i++) { // Create skeletons for canonical versions of layouts
      newBt.layouts[i] = layouts[i].makeCanonBitdataLayout(set, newBt);
    }
    newBt.cfuns = new Cfun[cfuns.length]; // Add canonical versions of constructors ...
    for (int i = 0; i < cfuns.length; i++) {
      newBt.cfuns[i] = cfuns[i].makeCanonCfun(set, newBt);
      newBt.layouts[i].makeCanonFields(set); // ... and fill in field types with canonical versions
    }
    return newBt;
  }

  /**
   * Determine whether a given type is an "enumeration", by which we mean that it has no parameters,
   * and no non-nullary constructors. Examples of such types include the Unit type, and simple
   * enumerations like the Booleans. It is not necessary to generate a new version of an enumeration
   * type in canonTycon: the result would be the same as the original, except for the change in
   * name.
   */
  boolean isEnumeration() {
    for (int i = 0; i < layouts.length; i++) {
      if (layouts[i] == null || layouts[i].getFields().length != 0) {
        return false;
      }
    }
    return true;
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return Type.repBits(pat.getWidth());
  }

  Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c) {
    return c;
  }

  Block maskTestBlock(int num) {
    return layouts[num].maskTestBlock();
  }

  Tail repTransformDataAlloc(RepTypeSet set, Cfun cf, Atom[] args) {
    BitdataLayout layout = layouts[cf.getNum()];
    return (layout.isNullary()) ? layout.repTransformDataAlloc(set, cf, args) : new Return(args);
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, Cfun cf, int n, Atom a) {
    return new Return(a.repAtom(set, env));
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c) {
    return new Bind(vs, repTransformSel(set, env, cf, n, a), c);
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return bitSize;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    return pat;
  }
}
