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

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

/** Provides a representation for (ordered) binary decision diagrams. */
public abstract class OBDD {

  public static final OBDD TRUE = new ConstOBDD(true);

  public static final OBDD FALSE = new ConstOBDD(false);

  /**
   * A smart constructor for building OBDDs: c.ite(t,f) builds an OBDD for the logical expression
   * (if c then t else f) that ensures correct ordering of tests (child nodes can only test lower
   * numbered variables than their parents) and eliminates the need for a test if the t and f trees
   * are equal.
   */
  public abstract OBDD ite(OBDD ifTrue, OBDD ifFalse);

  /**
   * Return the number of the highest variable number that is tested in this OBDD, or zero if this
   * is a constant BDD.
   */
  public int testVar() {
    return 0;
  }

  /**
   * A worker function for use in the ite() method. d.with(v, b) takes an OBDD d with all variables
   * <= v and returns a potentially simplified OBDD for the same formula with the special case where
   * v has been set to b. If the root variable in d is less than v, then the whole expression is
   * independent of the value of that variable (thanks to the OBDD invariant) and the result is just
   * the original input, d, without any modifications.
   */
  OBDD with(int var, boolean val) {
    return this;
  }

  /**
   * Check that an OBDD structure satisfies the variable ordering invariant. (Used for debugging.)
   */
  abstract boolean ordered(int w);

  /** Dump a textual description of an OBDD tree on System.out for debugging purposes. */
  abstract void dump();

  public void toDot(String name) {
    try {
      PrintWriter out = new PrintWriter(name);
      out.println("digraph OBDD {");
      this.toDot(out, 0);
      out.println("}");
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to create dot output in \"" + name + "\" failed");
    }
  }

  abstract int toDot(PrintWriter out, int next);

  int toDot(PrintWriter out, int parent, String label, int next) {
    out.println(parent + " -> " + next + "[label=\"" + label + "\"];");
    return toDot(out, next);
  }

  /** Test to see if this OBDD corresponds to a single boolean value. */
  public boolean isConst(boolean val) {
    return false;
  }

  /**
   * Test for a specific bit pattern in the set represented by an OBDD (assumes that the argument
   * bit pattern is in the correct range for the width of the OBDD.
   */
  abstract boolean includes(long i);

  /**
   * Find bit pattern for the smallest number that is included in a given OBDD. Assumes that the
   * given OBDD is not empty and that the required bit pattern can fit within a single machine word
   * (in particular, the largest variable index that is tested in the bdd is less the word size).
   */
  long minimum() {
    return 0;
  }

  /**
   * Find the smallest long value whose bit pattern is not included in the set represented by this
   * OBDD (which, of course, should not be the set of all bit patterns; i.e., its complement should
   * be non empty).
   */
  long smallestOutside() {
    return 0;
  }

  /** Test to determine whether two OBDD structures are the same. */
  public abstract boolean same(OBDD that);

  public boolean isITE(ITE that) {
    return false;
  }

  public OBDD not() {
    return this.ite(OBDD.FALSE, OBDD.TRUE);
  }

  public OBDD and(OBDD that) {
    return this.ite(that, OBDD.FALSE);
  }

  public OBDD or(OBDD that) {
    return this.ite(OBDD.TRUE, that);
  }

  abstract BigInteger size(int w);

  /**
   * Copy an OBDD value, incrementing each variable reference by the specified padding value
   * (internal use only).
   */
  abstract OBDD shiftLeft(int padding);

  public static OBDD intmod(int width, long val) {
    OBDD bdd = OBDD.TRUE;
    for (int i = 0; i < width; i++) {
      bdd = ((val & 1) == 1) ? new ITE(i, bdd, OBDD.FALSE) : new ITE(i, OBDD.FALSE, bdd);
      val >>= 1;
    }
    return bdd;
  }

  public static OBDD intmod(int width, BigInteger val, int offset) {
    OBDD bdd = OBDD.TRUE;
    for (int i = 0; i < width; i++) {
      bdd = val.testBit(i) ? new ITE(offset, bdd, OBDD.FALSE) : new ITE(offset, OBDD.FALSE, bdd);
      offset++;
    }
    return bdd;
  }

  /**
   * Calculate the total number of output lines that would be needed to describe this BDD in full.
   */
  abstract int countLines();

  /**
   * A worker function to output a textual description of this BDD into the given array of lines,
   * starting at the line specified by next and returning the first unused line number (if there is
   * one) as a result.
   */
  abstract int showBits(int width, String[] lines, int next);

  abstract BigInteger mask(boolean op);

  abstract BigInteger bits(boolean op);

  abstract OBDD masktest(boolean op);

  abstract OBDD blur();
}
