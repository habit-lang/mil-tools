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

public class Word extends Const {

  private long val;

  public Word(long val) {
    this.val = fromLong(val);
  }

  /**
   * Truncate the given long value to be within the range allowed by a Word of width Type.WORDSIZE.
   * (The latter should be either 32 or 64.) This allows us to store use long values in Word objects
   * (so that we can represent Word constants when Type.WORDSIZE==64), but to limit the range to
   * that of an int object (when necessary, so that we can represent Word constants when
   * Type.WORDSIZE==32).
   */
  public static long fromLong(long val) {
    int offset = 64 - Type.WORDSIZE;
    return (val << offset) >> offset;
  }

  public long getVal() {
    return val;
  }

  public static final Word Zero = new Word(0);

  /** Generate a printable description of this atom. */
  public String toString() {
    return "" + val;
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameWord(this);
  }

  /** Test to determine whether this Atom refers to the given Word value. */
  public boolean sameWord(Word c) {
    return this.val == c.val;
  }

  /** Test to determine whether this Atom is an integer constant (or not). */
  public Word isWord() {
    return this;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Tycon.word.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return new WordValue(val);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return (int) val;
  }

  public static long fromBig(BigInteger v) {
    if (Type.WORDSIZE == 32) {
      return (long) v.intValue();
    } else if (Type.WORDSIZE == 64) {
      return v.longValue();
    } else {
      debug.Internal.error("Unrecognized WORDSIZE in fromBig");
      return 0; /* not reached */
    }
  }

  /**
   * Calculate a static value for this atom, or return null if the result must be determined at
   * runtime.
   */
  llvm.Value calcStaticValue() {
    return new llvm.Word(val);
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm) {
    return new llvm.Word(val);
  }
}
