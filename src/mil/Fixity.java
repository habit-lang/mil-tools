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

public class Fixity {

  /** Specifies the minimum allowed precedence value. */
  public static final int LOW_PREC = 0;

  /** Specifies the maximum allowed precedence value. */
  public static final int HIGH_PREC = 9;

  /**
   * Specifies a left associative operator. If <code>+</code> is a left associative operator, then
   * <code>a+b+c</code> should be parsed as <code>(a+b)+c</code>.
   */
  public static final int LEFT = 1;

  /**
   * Specifies a nonassociative operator. If <code>+</code> is a nonassociative operator, then
   * <code>a+b+c</code> should be treated as an error (ambiguous use of operators).
   */
  public static final int NONASS = 2;

  /**
   * Specifies a right associative operator. If <code>+</code> is a right associative operator, then
   * <code>a+b+c</code> should be parsed as <code>a+(b+c)</code>.
   */
  public static final int RIGHT = 3;

  /** Records an associativity value (LEFT, NONASS, or RIGHT). */
  private int assoc;

  /** Return the associativity property of this fixity object. */
  public int getAssoc() {
    return assoc;
  }

  /** Records a precedence value. */
  private int prec;

  /** Return the precedence of this fixity object. */
  public int getPrec() {
    return prec;
  }

  /**
   * Construct a fixity object with specified precedence and associativity.
   *
   * @param assoc should be one of LEFT, NONASS, or RIGHT.
   * @param prec precedence level; any integer value is accepted, but values are always trimmed to
   *     numbers in the range LOW_PREC to HIGH_PREC.
   */
  public Fixity(int assoc, int prec) {
    this.assoc = assoc;
    this.prec = Math.max(LOW_PREC, Math.min(HIGH_PREC, prec));
  }

  /** Default fixity for things used as operators where no fixity has been provided. */
  public static final Fixity unspecified = new Fixity(NONASS, HIGH_PREC);

  /**
   * Use precedences to decide which of two operators should be applied first. If possible, we apply
   * the operator with the highest precedence first. If the two operators have the same precedence,
   * and are both left assoc (resp. right assoc) then we choose the left (resp. right) one first. If
   * all else fails, we determine that the use of the operators together is ambiguous.
   *
   * @param this the fixity of the left operator.
   * @param that the fixity of the right operator.
   * @return one of:
   *     <ul>
   *       <li><code>LEFT</code>, meaning that the left operator should be applied first.
   *       <li><code>RIGHT</code>, meaning that the right operator should be applied first.
   *       <li><code>NONASS</code>, meaning that the expression is ambiguous.
   *     </ul>
   */
  public int which(Fixity that) {
    if (this.prec > that.prec) {
      return LEFT;
    } else if (this.prec < that.prec) {
      return RIGHT;
    } else if (this.assoc == LEFT && that.assoc == LEFT) {
      return LEFT;
    } else if (this.assoc == RIGHT && that.assoc == RIGHT) {
      return RIGHT;
    } else {
      return NONASS;
    }
  }

  /** Test to see whether fixity objects are the same. */
  public boolean sameAs(Fixity that) {
    return (this.assoc == that.assoc) && (this.prec == that.prec);
  }

  /** Construct a printable representation of this fixity value. */
  public String toString() {
    switch (assoc) {
      case LEFT:
        return "infixl " + prec;
      case NONASS:
        return "infix " + prec;
      case RIGHT:
        return "infixr " + prec;
      default:
        return "invalid fixity";
    }
  }

  /** Precedence value to indicate that parens are not required. */
  public static final int NEVER = Integer.MIN_VALUE;

  /** Precedence value to indicate that parens are required. */
  public static final int ALWAYS = Integer.MAX_VALUE;

  /** Return the precedence that a left operand must have to avoid the need for parentheses. */
  public int leftPrec() {
    return (assoc == LEFT) ? prec : (prec + 1);
  }

  /** Return the precedence that a right operand must have to avoid the need for parentheses. */
  public int rightPrec() {
    return (assoc == RIGHT) ? prec : (prec + 1);
  }
}
