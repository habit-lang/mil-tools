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

public class FlagConst extends Const {

  private boolean val;

  /** Default constructor. */
  public FlagConst(boolean val) {
    this.val = val;
  }

  public boolean getVal() {
    return val;
  }

  public static final FlagConst True = new FlagConst(true);

  public static final FlagConst False = new FlagConst(false);

  public static FlagConst fromBool(boolean b) {
    return b ? True : False;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return val ? "flag1" : "flag0";
  }

  /**
   * Test to see if two atoms are the same. For a pair of IntConst objects, this means that the two
   * objects have the same val. For any other pair of Atoms, we expect the objects themselves to be
   * the same.
   */
  public boolean sameAtom(Atom that) {
    return that.sameFlagConst(this);
  }

  public boolean sameFlagConst(FlagConst c) {
    return this.val == c.val;
  }

  /** Test to determine whether this Atom is a flag constant (or not). */
  public FlagConst isFlagConst() {
    return this;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return DataName.flag.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return BoolValue.make(val);
  }

  /**
   * A simple test for MIL code fragments that return a known FlagConst, returning either the
   * constant or null.
   */
  FlagConst returnsFlagConst() {
    return this;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return val ? 71 : -11;
  }

  /**
   * Calculate a static value for this atom, or return null if the result must be determined at
   * runtime.
   */
  llvm.Value calcStaticValue() {
    return new llvm.Bool(val);
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm) {
    return val ? llvm.Bool.TRUE : llvm.Bool.FALSE;
  }
}
