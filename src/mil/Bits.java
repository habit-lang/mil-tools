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
import core.*;
import java.math.BigInteger;

public class Bits extends Const {

  private BigInteger val;

  private int width;

  /** Default constructor. */
  public Bits(BigInteger val, int width) {
    this.val = val;
    this.width = width;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return toString(val, width);
  }

  /** Generate a printable representation of a bit vector constant. */
  public static String toString(BigInteger nat, int width) {
    // TODO: does this produce correct results if width=0?
    StringBuilder buf = new StringBuilder();
    if (width > 0 && (width % 4) == 0) { // Use hexadecimal notation if we can
      buf.append('X');
      while (width > 0) {
        int d = nat.shiftRight(width -= 4).intValue() & 0xf; // TODO: clunky?
        buf.append((char) ((d < 10 ? ('0' + d) : ('a' + (d - 10))))); // TODO: builtin?
        if (width > 0 && ((width % 16) == 0)) {
          buf.append("_");
        }
      }
    } else { // Use binary notation if we must
      buf.append('B');
      while (width > 0) {
        buf.append(nat.testBit(--width) ? "1" : "0");
        if (width > 0 && ((width % 4) == 0)) {
          buf.append("_");
        }
      }
    }
    return buf.toString();
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameBits(this);
  }

  /** Test to determine whether this Atom refers to the specified bit vector constant. */
  public boolean sameBits(Bits c) {
    return this.width == c.width && this.val.equals(c.val);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Type.bit(width);
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return new WordValue(val.longValue());
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return val.intValue();
  }

  /** Return the representation vector for this Atom. */
  Type[] repCalc() {
    return Type.repBits(width);
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return Const.atoms(val, width);
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm) {
    return new llvm.Word(val.longValue());
  }
}
