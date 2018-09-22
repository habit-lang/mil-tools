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

  /** Specifies the number of bits in every value of type Word. (Should be either 32 or 64.) */
  private static int size;

  /** The current Word size, expressed as a BigInteger. */
  private static BigInteger sizeBig;

  /** The current Word size, expressed as a type of kind nat. */
  private static Type sizeType;

  /** The maximum signed value that can be represented in a Word. */
  private static BigInteger maxSigned;

  /** The maximum unsigned value that can be represented in a Word. */
  private static BigInteger maxUnsigned;

  /** A bit pattern for all Word values. */
  private static obdd.Pat allPat;

  /** A bit pattern for all nonzero Word values. */
  private static obdd.Pat nonzeroPat;

  /** Set the current word size, and dependent variables. */
  public static void setSize(int size) {
    Word.size = size;
    Word.sizeBig = BigInteger.valueOf(size);
    Word.sizeType = new TNat(sizeBig);
    Word.maxSigned = BigInteger.ONE.shiftLeft(size - 1).subtract(BigInteger.ONE);
    Word.maxUnsigned = BigInteger.ONE.shiftLeft(size).subtract(BigInteger.ONE);
    Word.allPat = obdd.Pat.all(size);
    Word.nonzeroPat = obdd.Pat.nonzero(size);
  }

  static {

    /* Default word size is 32 bits. */
    setSize(32);
  }

  /** Return the current Word size. */
  public static int size() {
    return size;
  }

  /** Return the current Word size as a BigInteger. */
  public static BigInteger sizeBig() {
    return sizeBig;
  }

  /** Return the current Word size as a Type. */
  public static Type sizeType() {
    return sizeType;
  }

  /** Return the maximum signed value that can be represented in a Word. */
  public static BigInteger maxSigned() {
    return maxSigned;
  }

  /** Return the maximum unsigned value that can be represented in a Word. */
  public static BigInteger maxUnsigned() {
    return maxUnsigned;
  }

  /** Return the bit pattern for all Word values. */
  public static obdd.Pat allPat() {
    return allPat;
  }

  /** Return the bit pattern for all nonzero Word values. */
  public static obdd.Pat nonzeroPat() {
    return nonzeroPat;
  }

  /** The value of this Word constant. */
  private long val;

  /** Return the value of this Word constant. */
  public long getVal() {
    return val;
  }

  /**
   * Create a Word value from the given long constant, truncating as necessary to match Word.size.
   */
  public Word(long val) {
    this.val = fromLong(val);
  }

  /**
   * Truncate the given long value to be within the range allowed by a Word of width Word.size. (The
   * latter should be either 32 or 64.) This allows us to use long values in Word objects (so that
   * we can represent Word constants when Word.size()==64), but to limit the range to that of an int
   * (when necessary, so that we can represent Word constants when Word.size()==32).
   */
  public static long fromLong(long val) {
    int offset = 64 - Word.size;
    return (val << offset) >> offset;
  }

  public static long fromBig(BigInteger v) {
    if (size == 32) {
      return (long) v.intValue();
    } else if (size == 64) {
      return v.longValue();
    } else {
      debug.Internal.error("Unrecognized Word.size in fromBig");
      return 0; /* not reached */
    }
  }

  /** As a special case, to allow easy reuse, export a constant representing the value zero. */
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

  /** Return the number of words that are needed to hold a value with the specified bitsize. */
  public static int numWords(int numBits) {
    return (numBits + size - 1) / size;
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
