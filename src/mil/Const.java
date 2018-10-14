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

public abstract class Const extends Atom {

  /** Apply a TempSubst to this Atom. */
  public Atom apply(TempSubst s) {
    return this;
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  void copyTo(int dst, MachineBuilder builder) {
    builder.gcopy(constValue(), dst);
  }

  /** Generate code to load the data for this atom into the value register. */
  void load(MachineBuilder builder) {
    builder.gload(constValue());
  }

  /** Find the Value for a given mil constant. */
  abstract Value constValue();

  boolean isStatic() {
    return true;
  }

  Atom isKnown() {
    return this;
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    instantiate().canonType(set);
  }

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    return this;
  }

  /** Return the representation vector for this Atom. */
  Type[] repCalc() {
    return null;
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return null;
  }

  /** Construct an array of Atoms that represents the bit vector with the given value and width. */
  public static Atom[] atoms(BigInteger v, int w) {
    if (w == 0) {
      return new Atom[] {Top.Unit};
    } else if (w == 1) {
      return new Flag[] {Flag.fromBool(v.compareTo(BigInteger.ZERO) != 0)};
    } else {
      Word[] as = new Word[Word.numWords(w)];
      int wordsize = Word.size();
      int i = 0; // index into array as (least significant word first)
      while (w > 0) { // while there are still bits to write
        long bits = Word.fromBig(v); // get least significant bits
        if ((w -= wordsize) < 0) { // truncate if necessary
          bits &= (1L << (wordsize + w)) - 1;
        }
        as[i++] = new Word(bits); // save word value
        v = v.shiftRight(wordsize); // discard least significant bits
      }
      return as;
    }
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return true;
  }
}
