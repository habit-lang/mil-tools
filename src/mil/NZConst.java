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

public class NZConst extends Const {

  private int val;

  /** Default constructor. */
  public NZConst(int val) {
    this.val = val;

    if (val == 0) {
      debug.Internal.error("NZConst with zero value");
    }
  }

  public int getVal() {
    return val;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return val + "nz";
  }

  /**
   * Test to see if two atoms are the same. For a pair of IntConst objects, this means that the two
   * objects have the same val. For any other pair of Atoms, we expect the objects themselves to be
   * the same.
   */
  public boolean sameAtom(Atom that) {
    return that.sameNZConst(this);
  }

  public boolean sameNZConst(NZConst c) {
    return this.val == c.val;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return DataName.nzword.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return new IntValue(val);
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
