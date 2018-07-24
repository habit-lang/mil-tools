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

public class IntConst extends Const {

  private int val;

  /** Default constructor. */
  public IntConst(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }

  public static final IntConst Zero = new IntConst(0);

  /** Generate a printable description of this atom. */
  public String toString() {
    return "" + val;
  }

  /**
   * Test to see if two atoms are the same. For a pair of IntConst objects, this means that the two
   * objects have the same val. For any other pair of Atoms, we expect the objects themselves to be
   * the same.
   */
  public boolean sameAtom(Atom that) {
    return that.sameIntConst(this);
  }

  public boolean sameIntConst(IntConst c) {
    return this.val == c.val;
  }

  /** Test to determine whether this Atom is an integer constant (or not). */
  public IntConst isIntConst() {
    return this;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return DataName.word.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return new IntValue(val);
  }

  /**
   * Determine whether this src argument is a value base (i.e., a numeric or global/primitive
   * constant) that is suitable for use in complex addressing modes.
   */
  boolean isBase() {
    return true;
  }

  /** Determine whether this Atom argument is a zero value. */
  boolean isZero() {
    return val == 0;
  }

  /**
   * Determine whether this Atom argument is a value multiplier (i.e., a constant 2, 4, or, 8) for
   * use in complex addressing modes.
   */
  boolean isMultiplier() {
    return (val == 2 || val == 4 || val == 8);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return val;
  }

  /**
   * Construct an array of IntConsts that represents the least significant n words, from least to
   * highest, of the specified BigInteger.
   */
  public static IntConst[] words(BigInteger v, int n) {
    IntConst[] as = new IntConst[n];
    for (int i = 0; i < n; v = v.shiftRight(Type.WORDSIZE)) {
      as[i++] = new IntConst(v.intValue());
    }
    return as;
  }

  /**
   * Calculate a static value for this atom, or else return null if the result must be calculated at
   * runtime.
   */
  llvm.Value staticValueCalc() {
    return new llvm.Int(val);
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  llvm.Value toLLVM(TypeMap tm, VarMap vm) {
    return new llvm.Int(val);
  }
}
