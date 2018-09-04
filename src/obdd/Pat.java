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

import java.math.BigInteger;

/** A main program for @Pat@, for the purposes of testing and illustration. */
public class Pat {

  protected int width;

  protected OBDD bdd;

  /** Default constructor. */
  public Pat(int width, OBDD bdd) {
    this.width = width;
    this.bdd = bdd;
  }

  /** Return the width of this bit pattern. */
  public int getWidth() {
    return width;
  }

  /** Test to see if two patterns define the same set of bit vectors. */
  public boolean same(Pat that) {
    return this.width == that.width && this.bdd.same(that.bdd);
  }

  /** A pattern representing the single bit pattern 0. */
  public static final Pat ZERO = new Pat(1, new ITE(0, OBDD.FALSE, OBDD.TRUE));

  /** A pattern representing the single bit pattern 1. */
  public static final Pat ONE = new Pat(1, new ITE(0, OBDD.TRUE, OBDD.FALSE));

  /**
   * Returns a bit pattern that represents the singleton set corresponding to the specified boolean.
   */
  public static Pat bool(boolean b) {
    return b ? Pat.ONE : Pat.ZERO;
  }

  /** Returns a pattern that matches all bit patterns of a given width. */
  public static Pat all(int width) {
    return new Pat(width, OBDD.TRUE);
  }

  /** Test to see if this pattern represents a set of all bit patterns of some given width. */
  public boolean isAll() {
    return bdd.isConst(true);
  }

  /** Returns a pattern that doesn't match any bit patterns of the specified width. */
  public static Pat empty(int width) {
    return new Pat(width, OBDD.FALSE);
  }

  /**
   * Test to see if this pattern represents the empty set of all bit patterns of some given width.
   */
  public boolean isEmpty() {
    return bdd.isConst(false);
  }

  /** Find the total number of elements in the corresponding set of bit vectors. */
  public BigInteger size() {
    return width == 0 ? BigInteger.ZERO : bdd.size(width);
  }

  /** Calculate the complement/negation of a given pattern. */
  public Pat not() {
    return new Pat(width, bdd.not());
  }

  /**
   * Calculate the conjunction/and/intersection of two patterns. We assume that the two patterns
   * have the same width.
   */
  public Pat and(Pat that) {
    return new Pat(width, bdd.and(that.bdd));
  }

  /**
   * Calculate the disjunction/or/union of two patterns. We assume that the two patterns have the
   * same width.
   */
  public Pat or(Pat that) {
    return new Pat(width, bdd.or(that.bdd));
  }

  public boolean ordered() {
    return bdd.ordered(width);
  }

  public boolean includes(long i) {
    return bdd.includes(i);
  }

  public long minimum() {
    return bdd.minimum();
  }

  public long smallestOutside() {
    return bdd.smallestOutside();
  }

  /** Determine whether two patterns (assumed to be of the same width) are disjoint. */
  public boolean disjoint(Pat that) {
    return bdd.and(that.bdd).isConst(false);
  }

  /**
   * Determine whether one pattern (this) is a superset of another (that), again assuming
   * this.width==that.width.
   */
  public boolean superset(Pat that) {
    return bdd.and(that.bdd.not()).isConst(false);
  }

  /**
   * Construct a new version of this pattern with extra @padding@ bits on the left (most significant
   * bits).
   */
  public Pat padLeft(int padding) {
    return new Pat(padding + width, bdd);
  }

  /**
   * Pad this pattern with (arbitrary) bits on the left to ensure the specified overall width
   * (assumes that this.width <= width).
   */
  public Pat padLeftTo(int width) {
    int padding = width - this.width;
    return (padding > 0) ? new Pat(width, bdd) : this;
  }

  /**
   * Construct a new version of this pattern with extra @padding@ bits on the right (least
   * significant bits).
   */
  public Pat padRight(int padding) {
    return padding == 0 ? this : new Pat(width + padding, this.bdd.shiftLeft(padding));
  }

  /**
   * Bit pattern concatenation. p.concat(q) represents the set of bit vectors whose left (most
   * significant) portion matches p and whose right (least significant) portion matches q.
   */
  public Pat concat(Pat that) {
    return new Pat(this.width + that.width, this.bdd.shiftLeft(that.width).and(that.bdd));
  }

  /**
   * Bit pattern concatenation for an array of bit patterns. TODO: There is likely a more efficient
   * way to implement this ...
   */
  public static Pat concat(Pat[] pats) {
    int i = pats.length;
    if (i == 0) {
      return Pat.all(0);
    } else {
      Pat p = pats[--i];
      while (--i >= 0) {
        p = pats[i].concat(p);
      }
      return p;
    }
  }

  /**
   * Returns a bit pattern of the specified width representing the singleton set that contains the
   * given val (modulo 2^width).
   */
  public static Pat intmod(int width, long val) {
    return new Pat(width, OBDD.intmod(width, val));
  }

  public static Pat intmod(int width, BigInteger val, int offset) {
    return new Pat(width + offset, OBDD.intmod(width, val, offset));
  }

  /**
   * Generate a pattern that matches unsigned integers of the given width that are greater than the
   * specified value, val. TODO: This likely won't work correctly as written for large values of
   * width (close to the word size) and val (in the area where signed and unsigned representations
   * differ).
   */
  public static Pat greater(int width, long val) {
    if (val < 0) {
      return Pat.all(width);
    }
    if (val >= (1L << width)) {
      return Pat.empty(width);
    }
    OBDD bdd = OBDD.FALSE;
    for (int i = 0; i < width; i++) {
      if ((val & 1) == 0) {
        bdd = new ITE(i, OBDD.TRUE, bdd);
      } else if (bdd != OBDD.FALSE) {
        bdd = new ITE(i, bdd, OBDD.FALSE);
      }
      val >>= 1;
    }
    return new Pat(width, bdd);
  }

  /**
   * Generate a pattern that matches unsigned integers of the given width that are greater than or
   * equal to the specified value, val.
   */
  public static Pat greaterEq(int width, long val) {
    return greater(width, val - 1);
  }

  /**
   * Generate a pattern that matches unsigned integers of the given width that are less than the
   * specified value, val.
   */
  public static Pat less(int width, long val) {
    return greaterEq(width, val).not();
  }

  /**
   * Generate a pattern that matches unsigned integers of the given width that are less than or
   * equal to the specified value, val.
   */
  public static Pat lessEq(int width, long val) {
    return greater(width, val).not();
  }

  /** Generate a pattern that matches any nonzero bit pattern of the given width. */
  public static Pat nonzero(int width) {
    OBDD bdd = OBDD.FALSE;
    for (int i = 0; i < width; i++) {
      bdd = new ITE(i, OBDD.TRUE, bdd);
    }
    return new Pat(width, bdd);
  }

  /** Generate a pattern that matches the zero bit pattern of the given width. */
  public static Pat zero(int width) {
    OBDD bdd = OBDD.TRUE;
    for (int i = 0; i < width; i++) {
      bdd = new ITE(i, OBDD.FALSE, bdd);
    }
    return new Pat(width, bdd);
  }

  public String[] showBits() {
    final int N = Math.min(10, bdd.countLines());
    String[] lines = new String[N];
    int next = bdd.showBits(width, lines, 0);
    if (next > N) {
      lines[N - 1] = "etc... (" + (1 + next - N) + " more lines)";
    }
    return lines;
  }

  public void toDot(String name) {
    bdd.toDot("tmp/" + name + ".dot");
    System.out.println();
    System.out.println(name + ":");
    display();
  }

  public void display() {
    String[] lines = this.showBits();
    for (int i = 0; i < lines.length; i++) {
      System.out.println("  " + lines[i]);
    }
  }

  public MaskTestPat masktest(boolean op) {
    return new MaskTestPat(this, op);
  }

  Pat masktest(String id, Pat butnot) {
    this.toDot(id + "-orig.dot");
    MaskTestPat test = new MaskTestPat(this, false); // look for a masktest predicate using ==
    if (!test.disjoint(butnot)) {
      test = new MaskTestPat(this, true); // look for a masktest predicate using /=
      if (!test.disjoint(butnot)) {
        System.out.println("no masktest predicate for " + id);
        return null; // none found :-(
      }
    }
    test.toDot(id + "-test.dot");
    test = test.blur(butnot);
    test.toDot(id + "-blurred.dot");
    return test;
  }

  /**
   * A method for enumerating and printing the set of values that are described by this bit pattern.
   */
  void walk() {
    OBDD d = bdd;
    System.out.print("{");
    while (!d.isConst(false)) {
      long m = d.minimum();
      System.out.print(" " + m);
      d = d.and(OBDD.intmod(width, m).not());
    }
    System.out.println(" }");
  }

  public static void main(String[] args) {
    Pat r = Pat.intmod(3, 1).or(Pat.intmod(3, 2)).or(Pat.intmod(3, 4));
    r.toDot("r.dot");
    Pat req = r.masktest(false);
    req.toDot("req.dot");
    Pat rneq = r.masktest(true);
    rneq.toDot("rneq.dot");
    Pat s = Pat.intmod(3, 6).or(Pat.intmod(3, 5)).or(Pat.intmod(3, 3));
    s.toDot("s.dot");
    Pat seq = s.masktest(false);
    seq.toDot("seq.dot");
    Pat sneq = s.masktest(true);
    sneq.toDot("sneq.dot");

    System.out.println(
        "disjoint req s = " + req.disjoint(s) + ", disjoint rneq s = " + rneq.disjoint(s));
    System.out.println(
        "disjoint seq r = " + seq.disjoint(r) + ", disjoint sneq r = " + sneq.disjoint(r));

    r.masktest("r", s);
    s.masktest("s", r);

    Pat r1 = Pat.ONE.concat(r.concat(Pat.ONE)); /*r.concat(Pat.ONE);*/
    r1.toDot("r1.dot");
    Pat r1eq = r1.masktest(false);
    r1eq.toDot("r1eq.dot");
    Pat r1neq = r1.masktest(true);
    r1neq.toDot("r1neq.dot");
    Pat s1 = Pat.ZERO.concat(s.concat(Pat.ZERO)); /*s.concat(Pat.ZERO);*/
    s1.toDot("s1.dot");
    Pat s1eq = s1.masktest(false);
    s1eq.toDot("s1eq.dot");
    Pat s1neq = s1.masktest(true);
    s1neq.toDot("s1neq.dot");

    System.out.println(
        "disjoint r1eq s1 = " + r1eq.disjoint(s1) + ", disjoint r1neq s1 = " + r1neq.disjoint(s1));
    System.out.println(
        "disjoint s1eq r1 = " + s1eq.disjoint(r1) + ", disjoint s1neq r1 = " + s1neq.disjoint(r1));

    r1.masktest("r1", s1);
    s1.masktest("s1", r1);

    Pat two = Pat.intmod(3, 2);
    Pat six = Pat.intmod(3, 6);
    two.concat(six).toDot("two+six.dot");
    two.toDot("two.dot");
    two.padLeft(3).toDot("twopaddleft.dot");
    two.padRight(3).toDot("twopaddright.dot");
    two.and(six).toDot("twoandsix.dot");
    two.and(six).walk();
    two.or(six).toDot("twoorsix.dot");
    two.or(six).walk();
    two.not().toDot("nottwo.dot");
    two.not().walk();
    for (int i = 0; i <= 8; i++) {
      Pat.less(3, i).toDot("lessThan" + i + ".dot");
      Pat.less(3, i).walk();
      System.out.println("Smallest outside is: " + Pat.less(3, i).smallestOutside());
    }
    Pat.less(24, 0xaaaaaa).toDot("lessThan0xaa_aa_aa.dot");
    Pat.less(8, 0xaa).toDot("lessThan0xaa.dot");

    Pat.intmod(8, 34).or(Pat.intmod(8, 73)).toDot("34or73.dot");
    Pat.intmod(8, 34).or(Pat.intmod(8, 73)).walk();

    Pat less54 = Pat.less(8, 54);
    Pat gte54 = Pat.greaterEq(8, 54);
    less54.toDot("less54.dot");
    gte54.toDot("gte54.dot");
    less54.or(gte54).toDot("everything.dot");

    Pat a = Pat.all(8);
    Pat e = Pat.empty(8);
    Pat t = Pat.intmod(8, 73);
    int n = 34;
    for (int i = 0; i < 256; i++) {
      // System.out.print(i + " ");
      a.toDot("a.dot");
      Pat g = Pat.greaterEq(8, i);
      if (!g.same(a)) {
        System.out.println("incremental construction of g fails for i=" + i);
      }
      a = a.and(Pat.intmod(8, i).not());

      e.toDot("e.dot");
      Pat l = Pat.less(8, i);
      if (!l.same(e)) {
        System.out.println("incremental construction of l fails for i=" + i);
      }
      e = e.or(Pat.intmod(8, i));

      t.toDot("t.dot");
      t.walk();
      System.out.println("Smallest outside is: " + t.smallestOutside());
      t = t.or(Pat.intmod(8, n));
      n = (n * 17 + 131) & 255;

      //    try { java.lang.Thread.sleep(200); } catch (Exception ex) { }
    }

    for (int i = 0; i < 256; i++) {
      t.toDot("t.dot");
      t = t.or(Pat.intmod(8, i));
      //    try { java.lang.Thread.sleep(200); } catch (Exception ex) { }
    }
  }
}
