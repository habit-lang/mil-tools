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

  /** Output information about this type for debugging purposes. */
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
    // unlikely to be used in practice, but technically possible
    this.dumpFixity(out);
    out.print("bitdata ");
    out.print(id);
    out.print(" /");
    out.println(bitSize.toString());
    for (int i = 0; i < layouts.length; i++) {
      out.print((i == 0) ? "  = " : "  | ");
      layouts[i].dumpBitdataLayout(out);
    }
    out.println();
    out.print("-- bit pattern:");
    out.println(pat.isRestricted() ? " (restricted)" : "");
    pat.dump(out);
    out.println();
  }

  /**
   * Generate a diagram for this type constructor using tikz syntax. TODO: Find a more appropriate
   * place for this code ...
   */
  void dumpTypeDiagram(PrintWriter out) {
    int bitSize = getPat().getWidth();
    out.println("\\section{Bitdata Type " + BitdataField.latexID(id) + "}");
    for (int i = 0; i < layouts.length; i++) {
      layouts[i].layoutDiagram(out, bitSize);
    }
    out.println();
  }

  /**
   * Generate LaTeX preamble code that defines the macros needed to typeset diagrams of
   * BitdataTypes.
   */
  static void dumpDiagramPreamble(PrintWriter out) {
    out.println("\\usepackage{tikz}");
    out.println("\\usepackage{fancyvrb,newverbs}");
    out.println();
    out.println("% General formatting for bitdata diagrams:");
    out.println("\\definecolor{bitdatacolor}{HTML}{E0F0E0}");
    out.println("\\newenvironment{bitdatapicture}%");
    out.println(
        "  {\\begin{center}\\begin{tikzpicture}"
            + "[scale=0.1,>=stealth,rectangle,minimum size=0.5cm]}%");
    out.println("  {\\end{tikzpicture}\\end{center}}");
    out.println();
    out.println("% Select where type labels appear:");
    out.println("\\def\\typelabelAbove#1#2{#1}");
    out.println("\\def\\typelabelBeside#1#2{#2}");
    out.println("\\def\\typelabelNone#1#2{}");
    out.println();
    out.println("% default position of type labels:");
    out.println("\\let\\typelabel\\typelabelAbove");
    out.println("% default position of label for short types:");
    out.println("\\let\\shorttypelabel\\typelabelBeside");
    out.println();
    out.println("% Select where field labels appear:");
    out.println("\\def\\fieldlabelUnder#1#2#3{#1}");
    out.println("\\def\\fieldlabelInside#1#2#3{#2}");
    out.println("\\def\\fieldlabelOver#1#2#3{#3}");
    out.println("\\def\\fieldlabelNone#1#2#3{}");
    out.println();
    out.println("% default position of field labels:");
    out.println("\\let\\fieldlabel\\fieldlabelUnder");
    out.println("% default position of label for short fields:");
    out.println("\\let\\shortfieldlabel\\fieldlabelOver");
    out.println();
    out.println("% Formatting of width annotations:");
    out.println("\\def\\widthSlash#1{\\Verb\"/#1\"}");
    out.println("\\def\\widthParen#1{\\Verb\"\\,(#1)\"}");
    out.println();
    out.println("%#1=typewidth, modify to show fieldwidths");
    out.println("\\def\\typewidth#1{\\ifnum#1>6\\relax{\\widthSlash{#1}}\\fi}");
    out.println("% #1=fieldwidth, modify to show fieldwidths");
    out.println("\\def\\fieldwidth#1{\\ifnum#1>4\\relax{\\widthSlash{#1}}\\fi}");
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  Tycon makeCanonTycon(TypeSet set) {
    // Do not make copies of enumerations
    if (isEnumeration()) {
      // (but still register them as being in use)
      set.addTycon(this);
      return this;
    }

    // Make new type, copying attributes of original
    BitdataType newBt = new BitdataType(pos, id);
    newBt.bitSize = bitSize;
    newBt.pat = pat;

    // Add mapping from old to new
    set.mapTycon(this, newBt);
    debug.Log.println("new version of BitdataType " + id + " is " + newBt);
    newBt.layouts = new BitdataLayout[layouts.length];

    // Create skeletons for canonical versions of layouts
    for (int i = 0; i < layouts.length; i++) {
      newBt.layouts[i] = layouts[i].makeCanonBitdataLayout(set, newBt);
    }

    // Add canonical versions of constructors ...
    newBt.cfuns = new Cfun[cfuns.length];
    for (int i = 0; i < cfuns.length; i++) {
      // ... and fill in field types with canonical versions
      newBt.cfuns[i] = cfuns[i].makeCanonCfun(set, newBt);
      newBt.layouts[i].makeCanonFields(set);
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

  /**
   * Find the block that implements the mask test predicate for the (num)th constructor of this
   * type.
   */
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
