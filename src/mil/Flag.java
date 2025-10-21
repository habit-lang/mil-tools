/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

/** Represents a Flag (Boolean) constant. */
public class Flag extends Const {

  /** The value of this Flag constant. */
  private boolean val;

  /** Default constructor. */
  public Flag(boolean val) {
    this.val = val;
  }

  boolean getVal() {
    return val;
  }

  static final Flag True = new Flag(true);

  static final Flag False = new Flag(false);

  static Flag fromBool(boolean b) {
    return b ? True : False;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return val ? "flag1" : "flag0";
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  boolean sameAtom(Atom that) {
    return that.sameFlag(this);
  }

  /** Test to determine whether this Atom refers to the given flag constant. */
  boolean sameFlag(Flag c) {
    return this.val == c.val;
  }

  /** Test to determine whether this Atom is a flag constant (or not). */
  Flag isFlag() {
    return this;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Tycon.flag.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return BoolValue.make(val);
  }

  /**
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return this;
  }

  Atom isKnown(boolean allowWord) {
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
