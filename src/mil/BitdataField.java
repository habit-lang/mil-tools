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
import java.math.BigInteger;

/** Represents a single field of a particular bitdata type that can be accessed using its name. */
public class BitdataField extends Name {

  /** Specifies the type of values in this field. */
  private Type type;

  /** The offset (measured in bits from least significant) to the start of this field. */
  private int offset;

  /** The width (in bits) of this field. */
  private int width;

  /** Default constructor. */
  public BitdataField(Position pos, String id, Type type, int offset, int width) {
    super(pos, id);
    this.type = type;
    this.offset = offset;
    this.width = width;
  }

  public Type getType() {
    return type;
  }

  public int getOffset() {
    return offset;
  }

  public int getWidth() {
    return width;
  }

  public void debugDump() {
    debug.Log.println(id + " :: " + type + " -- offset=" + offset + ", width=" + width);
  }

  int dumpBitdataField(PrintWriter out, BigInteger tagbits, int lastOffset) {
    int end = offset + width; // bit position where this field ends
    if (lastOffset > end) { // display any leading tagbits
      out.print(Bits.toString(tagbits.shiftRight(end), lastOffset - end));
      out.print(" | ");
    }
    out.print(id);
    out.print(" :: ");
    out.print(type.toString());
    return offset;
  }

  Tail repTransformSel(RepTypeSet set, RepEnv env, Atom a) {
    return new BlockCall(selectorBlock, a.repAtom(set, env));
  }

  private static Code prim(Temp[] ws, int n, Prim p, Atom a, Code c) {
    Temp u = ws[n];
    return new Bind(u, p.withArgs(ws[n] = new Temp(), a), c);
  }

  private static Code copy(Temp[] ws, int n, Atom a, Code c) {
    Temp u = ws[n];
    ws[n] = new Temp();
    return new Bind(u, new Return(a), c);
  }

  private Block selectorBlock;

  /**
   * Generate code for a selector for this field, given total size of the enclosing bitdata type.
   */
  void generateSelector(BitdataLayout layout) {
    selectorBlock = generateBitSelector(pos, type.useBitdataLo(), offset, width, layout.getWidth());
  }

  static Block generateBitSelector(Position pos, boolean useLo, int offset, int width, int total) {
    Temp[] params;
    Code code;
    if (width == 0) {
      params = Temp.makeTemps(total == 0 ? 1 : Word.numWords(total));
      code = new Done(new DataAlloc(Cfun.Unit).withArgs());
    } else {
      params = Temp.makeTemps(Word.numWords(total)); // input parameters
      Temp[] ws = Temp.makeTemps(Word.numWords(width)); // output parameters
      code = new Done(new Return(Temp.clone(ws))); // final return
      code =
          width == 1
              ? selectorBit(offset, total, params, ws, code)
              : useLo
                  ? selectorLo(offset, width, total, params, ws, code)
                  : selectorHi(offset, width, total, params, ws, code);
    }
    return new Block(pos, params, code); // create the new block
  }

  /**
   * Generate selector code for a bitdata field that contains only one bit; result should be of type
   * Flag.
   */
  private static Code selectorBit(int offset, int total, Temp[] params, Temp[] ws, Code code) {
    if (total == 1) { // special case if whole object is just a single bit
      return copy(ws, 0, params[0], code);
    } else {
      int wordsize = Word.size();
      int j = offset / wordsize; // number of word containing the bit we're interested in
      int o = offset % wordsize; // offset of the bit we're interested in ...
      Temp a = new Temp();
      return new Bind(
          a,
          Prim.and.withArgs(params[j], 1L << o),
          new Bind(ws[0], Prim.neq.withArgs(a, Word.Zero), code));
    }
  }

  /** Generate selector code for a bitdata field whose type uses the lo bits representation. */
  private static Code selectorLo(
      int offset, int width, int total, Temp[] params, Temp[] ws, Code code) {
    int wordsize = Word.size();
    int n = ws.length; // number of words in output
    int w = n * wordsize - width; // unused bits in most sig output word
    int o = offset % wordsize; // offset within each word
    int j = offset / wordsize; // starting word offset in params

    if (width + offset < total && 0 < w) { // add mask on msw, if needed
      code = prim(ws, n - 1, Prim.and, new Word((1L << (wordsize - w)) - 1), code);
    }

    for (int i = n - 1; i >= 0; i--) { // extract each word of the result
      if (o == 0) {
        // field is aligned on a word boundary in the input, so we can just copy words:
        code = copy(ws, i, params[i + j], code);
      } else if ((i == n - 1) && w >= o) {
        // in last word, and all remaining bits are included in params[i+j]:
        code = new Bind(ws[i], Prim.lshr.withArgs(params[i + j], o), code);
      } else {
        // the bits for this result word are a combination of two adjacent parameters:
        Temp a = new Temp();
        Temp b = new Temp();
        code =
            new Bind(
                a,
                Prim.lshr.withArgs(params[i + j], o),
                new Bind(
                    b,
                    Prim.shl.withArgs(params[i + j + 1], wordsize - o),
                    new Bind(ws[i], Prim.or.withArgs(a, b), code)));
      }
    }
    return code;
  }

  /** Generate selector code for a bitdata field whose type uses the hi bits representation. */
  private static Code selectorHi(
      int offset, int width, int total, Temp[] params, Temp[] ws, Code code) {
    int wordsize = Word.size();
    int n = ws.length; // number of words in output
    int w = n * wordsize - width; // unused bits in least sig word
    int o = (wordsize - (width + offset) % wordsize) % wordsize; // offset to msb in input
    int j = (width + offset - 1) / wordsize - (n - 1); // offset from output to input

    if (0 < w) { // add mask on lsw, if needed
      code = prim(ws, 0, Prim.and, new Word((~0) << w), code);
    }

    for (int i = n - 1; i >= 0; i--) { // extract each word of the result
      if (o == 0) {
        // field is aligned on a word boundary in the input, so we can just copy words:
        code = copy(ws, i, params[i + j], code);
      } else if ((i == 0) && (w >= o)) {
        // in last word, and all remaining bits are included in params[i+j]:
        code = new Bind(ws[i], Prim.shl.withArgs(params[i + j], o), code);
      } else {
        // the bits for this result word are a combination of two adjacent parameters:
        Temp a = new Temp();
        Temp b = new Temp();
        code =
            new Bind(
                a,
                Prim.shl.withArgs(params[i + j], o),
                new Bind(
                    b,
                    Prim.lshr.withArgs(params[i + j - 1], wordsize - o),
                    new Bind(ws[i], Prim.or.withArgs(a, b), code)));
      }
    }
    return code;
  }

  private Prim updatePrim;

  public Prim getUpdatePrim() {
    return updatePrim;
  }

  /**
   * Generate code for an update operator for this field, given total size of the enclosing bitdata
   * type.
   */
  public void generateUpdate(BitdataLayout layout) {
    int total = layout.getWidth(); // number of bits in output
    Block impl;
    if (total == 0) { // If total==0, then any fields must also have zero width
      impl = new Block(pos, Temp.makeTemps(2), new Done(Cfun.Unit.withArgs()));
    } else { // General case, total>0
      int n = Word.numWords(total); // number of words in output
      Temp[] ws = Temp.makeTemps(n); // arguments to hold full bitdata value
      Temp[] args; // arguments to hold new field value
      Code code;
      if (width == 0) { // If width==0, then update just returns original value
        args = Temp.makeTemps(1);
        code = new Done(new Return(ws));
      } else { // If width>0, do the proper logic to update the field
        args = Temp.makeTemps(Word.numWords(width));
        code =
            genMaskField(
                total,
                ws,
                genUpdateZeroedField(total, ws, args, new Done(new Return(Temp.clone(ws)))));
      }
      impl = new Block(pos, Temp.append(ws, args), code);
    }
    Type lt = layout.asType();
    BlockType bt = new BlockType(Type.tuple(lt, getType()), Type.tuple(lt));
    updatePrim = new Prim.blockImpl("update_" + id, Prim.PURE, bt, impl);
  }

  /**
   * Generate code to update a zero-ed field within a bitdata type, assuming a given total width for
   * the whole bitdata value; an array of words, ws, for the full bitdata value; and an array of
   * words, as, for the field value. The generated code is prefixed on to the given code.
   */
  Code genUpdateZeroedField(int total, Temp[] ws, Temp[] as, Code code) {
    return width == 1
        ? genUpdateZeroedFieldBit(offset, width, total, ws, as, code)
        : type.useBitdataLo()
            ? genUpdateZeroedFieldLo(offset, width, total, ws, as, code)
            : genUpdateZeroedFieldHi(offset, width, total, ws, as, code);
  }

  /**
   * Generate code to update an already zero-ed single bit field within a bitdata value, given an
   * input of type Flag.
   */
  static Code genUpdateZeroedFieldBit(
      int offset, int width, int total, Temp[] ws, Temp[] as, Code code) {
    if (total == 1) { // special case if whole object is just a single bit
      return copy(ws, 0, as[0], code);
    } else {
      int wordsize = Word.size();
      int j = offset / wordsize; // number of word containing the bit we're interested in
      int o = offset % wordsize; // offset of the bit we're interested in ...
      Temp a = new Temp();
      Temp b = new Temp();
      return new Bind(
          a,
          Prim.flagToWord.withArgs(as[0]),
          new Bind(b, Prim.shl.withArgs(a, o), prim(ws, j, Prim.or, b, code)));
    }
  }

  /**
   * Generate code to update an already zero-ed field within a bitdata value given an update value
   * that uses the lo bits representation.
   */
  static Code genUpdateZeroedFieldLo(
      int offset, int width, int total, Temp[] ws, Temp[] as, Code code) {
    int wordsize = Word.size();
    int n = as.length; // number of words for field
    int w = n * wordsize - width; // unused bits in last word
    int o = offset % wordsize;
    int j = offset / wordsize;
    for (int i = 0; i < n; i++) {
      if (o == 0) {
        code =
            (i < (n - 1) || w == 0)
                ? copy(ws, i + j, as[i], code) // ws[i+j] = as[i]
                : prim(ws, i + j, Prim.or, as[i], code); // ws[i+j] |= as[i]
      } else {
        Temp a = new Temp();
        code =
            new Bind(
                a,
                Prim.shl.withArgs(as[i], o), // a <- shl((as[i],o))
                prim(ws, i + j, Prim.or, a, code)); // ws[i+j] |= a
        if (i < (n - 1) || o > w) { // field extends to the next word?
          // There are (wordsize-w) bits of useful information in as[n-1], but only (wordsize-o)
          // bits available to
          // store it in ws[i+j].  So if (wordsize-w)>(wordsize-o), or equivalently, o>w, then we
          // will need to make
          // use of bits in ws[i+j+1] too ...
          a = new Temp(); // a <- lshr((as[i],W-o))
          code =
              new Bind(
                  a,
                  Prim.lshr.withArgs(as[i], wordsize - o),
                  prim(ws, i + j + 1, Prim.or, a, code)); // ws[i+j+1] |= a
        }
      }
    }
    return code;
  }

  /**
   * Generate code to update an already zero-ed field within a bitdata value given an update value
   * that uses the hi bits representation.
   */
  static Code genUpdateZeroedFieldHi(
      int offset, int width, int total, Temp[] ws, Temp[] as, Code code) {
    int wordsize = Word.size();
    int n = as.length; // number of words for field
    int w = n * wordsize - width; // unused bits in last word
    int e = offset + width; // offset to msb after field
    int o = (wordsize - e % wordsize) % wordsize; // offset from the top of each word
    int j = (e - 1) / wordsize - (n - 1); // offset from output to input

    for (int i = 0; i < n; i++) {
      if (o == 0) {
        code =
            (i > 0 || w == 0)
                ? copy(ws, i + j, as[i], code) // ws[i+j] = as[i]
                : prim(ws, i + j, Prim.or, as[i], code); // ws[i+j] |= as[i]
      } else {
        Temp a = new Temp();
        code =
            new Bind(
                a,
                Prim.lshr.withArgs(as[i], o), // a <- lshr((as[i],o))
                prim(ws, i + j, Prim.or, a, code)); // ws[i+j] |= a
        if (i > 0 || o > w) { // field extends to next word?
          a = new Temp(); // a <- shl((as[i],W-o))
          code =
              new Bind(
                  a,
                  Prim.shl.withArgs(as[i], wordsize - o),
                  prim(ws, i + j - 1, Prim.or, a, code)); // ws[i+j-1] |= a
        }
      }
    }
    return code;
  }

  /**
   * Generate code to mask out a specified field within the a bitdata value. This is used to set all
   * the bits for that field to zero so that we can then use one of the update operations above to
   * implement a full update. The distinction between single bit, lo bits, and hi bits
   * representations is not relevant here because we are zeroing all of the bits in this field.
   */
  Code genMaskField(int total, Temp[] ws, Code code) {
    if (width > 0) {
      int wordsize = Word.size();
      int o = offset % wordsize; // offset to lowest bit of field within lowest word
      int j = offset / wordsize; // index of lowest word of ws containing field bits
      int e = offset + width; // index of high bit after field
      int p = e % wordsize; // offset to highest bit of field within highest word
      int k = (e - 1) / wordsize; // index of highest word of ws containing field bits

      long lomask = (o == 0) ? 0 : ((1L << o) - 1); // mask to preserve low bits
      long himask = (p == 0) ? 0 : ((-1L) << p); // mask to preserve high bits
      int q = total - k * wordsize;
      if (q < wordsize) {
        himask &= (1L << q) - 1;
      }

      if (j == k) {
        code = prim(ws, j, Prim.and, new Word(lomask | himask), code);
      } else {
        if (o != 0) {
          code = prim(ws, j, Prim.and, new Word(lomask), code);
        }
        for (int i = j + 1; i < k; i++) {
          code = copy(ws, i, Word.Zero, code);
        }
        if (p != 0) {
          code = prim(ws, k, Prim.and, new Word(himask), code);
        }
      }
    }
    return code;
  }

  void calculateBitdataBlocks(BitdataLayout layout) {
    generateSelector(layout);
    generateUpdate(layout);
  }
}
