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
import obdd.Pat;

/**
 * Represents a type constructor that is introduced as a bitdata type. TODO: BitdataName shouldn't
 * require an arity; refactor to make DataName a common base class for BitdataName and DatatypeName?
 * Reconsider hierarchy!
 */
public class BitdataName extends DataName {

  /** Default constructor. */
  public BitdataName(Position pos, String id, Kind kind, int arity) {
    super(pos, id, kind, arity);
  }

  /** A constructor for defining names that have BuiltinPosition. */
  public BitdataName(String id, Kind kind, int arity) {
    this(BuiltinPosition.position, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataName bitdataName() {
    return this;
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return bitSize;
  }

  private Type bitSize;

  public void setBitSize(Type bitSize) {
    this.bitSize = bitSize;
  }

  private Pat pat;

  public void setPat(Pat pat) {
    this.pat = pat;
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    return pat;
  }

  private BitdataLayout[] layouts;

  public BitdataLayout[] getLayouts() {
    return layouts;
  }

  public void setLayouts(BitdataLayout[] layouts) {
    this.layouts = layouts;

    for (int i = 0; i < layouts.length; i++) {
      layouts[i].calculateBitdataBlocks(cfuns[i]);
    }
  }

  /** Return the bit pattern for this object. */
  public obdd.Pat getPat() {
    return pat;
  }

  public obdd.Pat getPat(int num) {
    return layouts[num].getPat();
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
}
