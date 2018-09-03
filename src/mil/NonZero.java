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

public class NonZero extends Const {

  private long val;

  public NonZero(long val) {
    this.val = Word.fromLong(val);
    if (this.val == 0) {
      debug.Internal.error("NonZero with zero value");
    }
  }

  public long getVal() {
    return val;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return val + "nz";
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameNonZero(this);
  }

  /** Test to determine whether this Atom refers to the given non zero word value. */
  public boolean sameNonZero(NonZero c) {
    return this.val == c.val;
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Tycon.nzword.asType();
  }

  /** Find the Value for a given mil constant. */
  Value constValue() {
    return new WordValue(val);
  }

  /**
   * Return the nonzero value associated with this atom; a return of zero indicates that the atom
   * was not a NonZero.
   */
  long getNonZero() {
    return val;
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
