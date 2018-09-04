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
package obdd;

import java.io.PrintWriter;
import java.math.BigInteger;

class ConstOBDD extends OBDD {

  private boolean val;

  /** Default constructor. */
  ConstOBDD(boolean val) {
    this.val = val;
  }

  /**
   * A smart constructor for building OBDDs: c.ite(t,f) builds an OBDD for the logical expression
   * (if c then t else f) that ensures correct ordering of tests (child nodes can only test lower
   * numbered variables than their parents) and eliminates the need for a test if the t and f trees
   * are equal.
   */
  public OBDD ite(OBDD ifTrue, OBDD ifFalse) {
    return val ? ifTrue : ifFalse;
  }

  /**
   * Check that an OBDD structure satisfies the variable ordering invariant. (Used for debugging.)
   */
  boolean ordered(int w) {
    return true;
  }

  /** Dump a textual description of an OBDD tree on System.out for debugging purposes. */
  void dump() {
    System.out.print(val ? "T" : "F");
  }

  int toDot(PrintWriter out, int next) {
    out.println(next + "[label=\"" + val + "\"];");
    return next + 1;
  }

  /** Test to see if this OBDD corresponds to a single boolean value. */
  public boolean isConst(boolean val) {
    return val == this.val;
  }

  /**
   * Test for a specific bit pattern in the set represented by an OBDD (assumes that the argument
   * bit pattern is in the correct range for the width of the OBDD.
   */
  boolean includes(long i) {
    return val;
  }

  /** Test to determine whether two OBDD structures are the same. */
  public boolean same(OBDD that) {
    return that.isConst(val);
  }

  BigInteger size(int w) {
    return val ? BigInteger.ZERO.setBit(w) : BigInteger.ZERO;
  }

  /**
   * Copy an OBDD value, incrementing each variable reference by the specified padding value
   * (internal use only).
   */
  OBDD shiftLeft(int padding) {
    return this;
  }

  /**
   * Calculate the total number of output lines that would be needed to describe this BDD in full.
   */
  int countLines() {
    return val ? 1 : 0;
  }

  /**
   * A worker function to output a textual description of this BDD into the given array of lines,
   * starting at the line specified by next and returning the first unused line number (if there is
   * one) as a result.
   */
  int showBits(int width, String[] lines, int next) {
    if (val) {
      if (next < lines.length) {
        StringBuilder buf = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
          buf.append('_');
        }
        lines[next] = buf.toString();
      }
      return next + 1;
    }
    return next;
  }

  BigInteger mask(boolean op) {
    return BigInteger.ZERO;
  }

  BigInteger bits(boolean op) {
    return BigInteger.ZERO;
  }

  OBDD masktest(boolean op) {
    return this;
  }

  OBDD blur() {
    return null;
  }
}
