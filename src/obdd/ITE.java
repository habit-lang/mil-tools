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

class ITE extends OBDD {

  private int var;

  private OBDD ifTrue;

  private OBDD ifFalse;

  /** Default constructor. */
  ITE(int var, OBDD ifTrue, OBDD ifFalse) {
    this.var = var;
    this.ifTrue = ifTrue;
    this.ifFalse = ifFalse;
  }

  /**
   * A smart constructor for building OBDDs: c.ite(t,f) builds an OBDD for the logical expression
   * (if c then t else f) that ensures correct ordering of tests (child nodes can only test lower
   * numbered variables than their parents) and eliminates the need for a test if the t and f trees
   * are equal.
   */
  public OBDD ite(OBDD ifTrue, OBDD ifFalse) {
    // The resulting OBDD will make a decision based on newVar, the highest numbered variable from
    // this, ifTrue,
    // and ifFalse:
    int newVar = Math.max(var, Math.max(ifTrue.testVar(), ifFalse.testVar()));

    // The true and false branches of the resulting OBDD are obtained using recursive calls to
    // ite(), with
    // appropriate specializations for the value of newVar (which will be true on the ifTrue branch,
    // and false
    // on the ifFalse branch):
    OBDD newIfTrue =
        this.with(newVar, true).ite(ifTrue.with(newVar, true), ifFalse.with(newVar, true));
    OBDD newIfFalse =
        this.with(newVar, false).ite(ifTrue.with(newVar, false), ifFalse.with(newVar, false));

    // If the resulting true and false branches are the same, then there is no need for an ITE and
    // we can just use
    // one of the branches as given (i.e., without an additional test).  Otherwise, we construct a
    // new ITE node.
    return newIfTrue.same(newIfFalse) ? newIfTrue : new ITE(newVar, newIfTrue, newIfFalse);
  }

  /**
   * Return the number of the highest variable number that is tested in this OBDD, or zero if this
   * is a constant BDD.
   */
  public int testVar() {
    return var;
  }

  /**
   * A worker function for use in the ite() method. d.with(v, b) takes an OBDD d with all variables
   * <= v and returns a potentially simplified OBDD for the same formula with the special case where
   * v has been set to b. If the root variable in d is less than v, then the whole expression is
   * independent of the value of that variable (thanks to the OBDD invariant) and the result is just
   * the original input, d, without any modifications.
   */
  OBDD with(int var, boolean val) {
    return (this.var == var) ? (val ? this.ifTrue : this.ifFalse) : this;
  }

  /**
   * Check that an OBDD structure satisfies the variable ordering invariant. (Used for debugging.)
   */
  boolean ordered(int w) {
    return (var < w) && ifFalse.ordered(var) && ifTrue.ordered(var);
  }

  /** Dump a textual description of an OBDD tree on System.out for debugging purposes. */
  void dump() {
    System.out.print("(");
    ifTrue.dump();
    System.out.print(" " + Integer.toString(var) + " ");
    ifFalse.dump();
    System.out.print(")");
  }

  int toDot(PrintWriter out, int next) {
    out.println(next + "[label=\"" + var + "?\"];");
    return ifFalse.toDot(out, next, "0", ifTrue.toDot(out, next, "1", next + 1));
  }

  /**
   * Test for a specific bit pattern in the set represented by an OBDD (assumes that the argument
   * bit pattern is in the correct range for the width of the OBDD.
   */
  boolean includes(long i) {
    return (((1L << var) & i) == 0) ? ifFalse.includes(i) : ifTrue.includes(i);
  }

  /**
   * Find bit pattern for the smallest number that is included in a given OBDD. Assumes that the
   * given OBDD is not empty and that the required bit pattern can fit within a single machine word
   * (in particular, the largest variable index that is tested in the bdd is less the word size).
   */
  long minimum() {
    return ifFalse.isConst(false) ? ((1L << var) | ifTrue.minimum()) : ifFalse.minimum();
  }

  /**
   * Find the smallest long value whose bit pattern is not included in the set represented by this
   * OBDD (which, of course, should not be the set of all bit patterns; i.e., its complement should
   * be non empty).
   */
  long smallestOutside() {
    return ifFalse.isConst(true)
        ? ((1L << var) | ifTrue.smallestOutside())
        : ifFalse.smallestOutside();
  }

  /** Test to determine whether two OBDD structures are the same. */
  public boolean same(OBDD that) {
    return that.isITE(this);
  }

  public boolean isITE(ITE that) {
    return that.var == this.var && that.ifTrue.same(this.ifTrue) && that.ifFalse.same(this.ifFalse);
  }

  BigInteger size(int w) {
    return ifTrue.size(var).add(ifFalse.size(var)).shiftLeft(w - 1 - var);
  }

  /**
   * Copy an OBDD value, incrementing each variable reference by the specified padding value
   * (internal use only).
   */
  OBDD shiftLeft(int padding) {
    return new ITE(var + padding, ifTrue.shiftLeft(padding), ifFalse.shiftLeft(padding));
  }

  /**
   * Calculate the total number of output lines that would be needed to describe this BDD in full.
   */
  int countLines() {
    return ifTrue.countLines() + ifFalse.countLines();
  }

  /**
   * A worker function to output a textual description of this BDD into the given array of lines,
   * starting at the line specified by next and returning the first unused line number (if there is
   * one) as a result.
   */
  int showBits(int width, String[] lines, int next) {
    if (width - 1 > var) {
      int end = this.showBits(width - 1, lines, next);
      for (int i = next; i < end && i < lines.length; i++) {
        lines[i] = "_" + lines[i];
      }
      return end;
    } else {
      int endFalse = ifFalse.showBits(width - 1, lines, next);
      int endTrue = ifTrue.showBits(width - 1, lines, endFalse);
      for (int i = next; i < endFalse && i < lines.length; i++) {
        lines[i] = "0" + lines[i];
      }
      for (int i = endFalse; i < endTrue && i < lines.length; i++) {
        lines[i] = "1" + lines[i];
      }
      return endTrue;
    }
  }

  BigInteger mask(boolean op) {
    OBDD child = ifFalse.isConst(op) ? ifTrue : ifFalse;
    return child.mask(op).setBit(var);
  }

  BigInteger bits(boolean op) {
    return ifFalse.isConst(op) ? ifTrue.bits(op).setBit(var) : ifFalse.bits(op);
  }

  OBDD masktest(boolean op) {
    if (ifTrue.isConst(op)) {
      return new ITE(var, ifTrue, ifFalse.masktest(op));
    } else if (ifFalse.isConst(op)) {
      return new ITE(var, ifTrue.masktest(op), ifFalse);
    } else {
      return ifTrue.or(ifFalse).masktest(op);
    }
  }

  OBDD blur() {
    return ifTrue.or(ifFalse);
  }
}
