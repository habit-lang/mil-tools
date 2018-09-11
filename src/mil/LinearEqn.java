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
import compiler.Failure;
import compiler.Position;
import core.*;
import java.math.BigInteger;

/**
 * Represents a linear equation with a collection of zero or more terms (known coefficients and
 * variables) on the left hand side and a constant value on the right hand side.
 */
public class LinearEqn {

  private Position pos;

  /** Default constructor. */
  public LinearEqn(Position pos) {
    this.pos = pos;
  }

  private Terms lhs = null;

  private BigInteger rhs = BigInteger.ZERO;

  Position getPos() {
    return pos;
  }

  /** Return a printable representation of this equation. */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    this.appendTo(buf);
    return buf.toString();
  }

  /** Append a string representation of this equation to the specified buffer. */
  void appendTo(StringBuilder buf) {
    // Display positive terms on the left hand side:
    boolean anylhs = false; // set to true when we produce lhs output
    for (Terms ts = lhs; ts != null; ts = ts.next) {
      if (ts.coeff.signum() > 0) { // print positive terms on the rhs
        if (anylhs) {
          buf.append(" + ");
        } else {
          anylhs = true;
        }
        ts.appendTo(buf);
      }
    }
    if (rhs.signum() < 0) { // if rhs is negative, print it on the lhs
      if (anylhs) {
        buf.append(" + ");
      } else {
        anylhs = true;
      }
      buf.append(rhs.negate().toString());
    }
    if (!anylhs) {
      buf.append("0");
    } // put a zero on the lhs if no other terms

    // Insert an equal sign:
    buf.append(" = ");

    // Display negative terms on the right hand side:
    boolean anyrhs = false; // set to true when we produce rhs output
    for (Terms ts = lhs; ts != null; ts = ts.next) { // print negative terms on the rhs
      if (ts.coeff.signum() < 0) {
        if (anyrhs) {
          buf.append(" + ");
        } else {
          anyrhs = true;
        }
        ts.appendTo(buf);
      }
    }
    if (rhs.signum() > 0) { // if rhs is positive, print it on the rhs
      if (anyrhs) {
        buf.append(" + ");
      } else {
        anyrhs = true;
      }
      buf.append(rhs.toString());
    }
    if (!anyrhs) {
      buf.append("0");
    } // put a zero on the rhs if no other terms
  }

  /**
   * Add a constant to the left hand side of this equation (which, in practice, is implemented by
   * subtracting that same value from the right hand side).
   */
  public void addConst(BigInteger c) {
    rhs = rhs.subtract(c);
  }

  /** Add a constant term that is specified by a long value. */
  public void addConst(long c) {
    addConst(BigInteger.valueOf(c));
  }

  /**
   * Add a term with coefficient m and Type t to the left hand side of this equation. This function
   * is written to maintain the invariant that every entry in the list of terms has a non-zero
   * coefficient, and that no variable/type is listed more than once.
   */
  public void addTerm(BigInteger m, Type t, Object hint) {
    if (m.signum() != 0) {
      t = t.simplifyNatType(null); // If this term is already instantiated
      BigInteger n = t.getNat();
      if (n != null) { // then add a constant term to the equation
        addConst(n.multiply(m));
        return;
      }
      Terms prev = null; // Otherwise, t is an unbound type variable
      for (Terms ts = lhs; ts != null; ts = ts.next) {
        if (t == ts.t) { // Found prior use of variable
          m = m.add(ts.coeff); // Calculate new coefficient
          if (m.signum() != 0) { // Update coefficient in list if it is nonzero
            ts.coeff = m;
          } else if (prev == null) { // We are removing first term from list
            lhs = ts.next;
          } else { // We are removing a later term from the list
            prev.next = ts.next;
          }
          return;
        }
        prev = ts;
      }
      lhs = new Terms(m, t, hint, lhs);
    }
  }

  /** Add a term for t with coefficient of one. */
  public void addTerm(Type t, Object hint) {
    addTerm(BigInteger.ONE, t, hint);
  }

  /** Add a term for t with coefficient of one on the right hand side of this equation. */
  public void addRhsTerm(Type t, Object hint) {
    addTerm(BigInteger.ONE.negate(), t, hint);
  }

  /** Add a term for t with a coefficient specified by a long value rather than a BigInteger. */
  public void addTerm(long m, Type t, Object hint) {
    addTerm(BigInteger.valueOf(m), t, hint);
  }

  /**
   * Determine whether this equation has been solved, eliminating any terms that have been bound to
   * specific numeric values, and then solving the equation if only one or no terms remains. If two
   * or more terms remain, however, then the equation is unsolved and we return false.
   */
  boolean solved() throws Failure {
    // Run through the left hand side terms, updating the equation for any terms that have been
    // bound to numeric values:
    Terms prev = null;
    Terms ts = lhs;
    while (ts != null) {
      ts.t = ts.t.simplifyNatType(null);
      BigInteger n = ts.t.getNat();
      if (n == null) { // This type is a variable
        prev = ts;
        ts = ts.next;
      } else { // This type is a number:
        addConst(n.multiply(ts.coeff)); // - add the corresponding constant term
        ts = ts.next; // - and remove this term from the list
        if (prev == null) {
          lhs = ts;
        } else {
          prev.next = ts;
        }
      }
    }

    // Look for equations with one or less terms on the left hand side:
    if (lhs == null) { // If lhs is empty, then the right hand side should be zero.
      if (rhs.signum() != 0) {
        throw new WidthsDifferFailure(pos, rhs);
      }
      return true;
    } else if (lhs.next == null) { // If lhs has exactly one variable, find its value:
      if (lhs.coeff.signum() < 0) {
        rhs = rhs.negate();
        lhs.coeff = lhs.coeff.negate();
      }
      BigInteger[] dr = rhs.divideAndRemainder(lhs.coeff);
      if (dr[1].signum() != 0 || dr[0].signum() < 0) {
        throw new CannotSatisfyFailure(this);
      }
      lhs.t.bindNat(dr[0]);
      return true;
    }
    return false;
  }

  /**
   * Rewrite this equation to eliminate any use of the first variable in eqn. If the first variable
   * in eqn has coefficient c, and the same variable has coefficient d in this equation, then we can
   * replace this equation with (c * this - d * eqn), using (k * ...) to denote scalar
   * multiplication. To avoid using unnecessarily large scaling factors, we can use ((c/g) * this -
   * (d/g) * eqn) where g is the greatest common divisor of c and d. By construction, the resulting
   * equation will not include the first variable from eqn. (Of course, if that variable wasn't in
   * this equation to begin with, then no action is required.)
   */
  void elimVar(LinearEqn eqn) {
    BigInteger c = eqn.lhs.coeff; // coefficient of first variable in eqn
    BigInteger d = this.coeffOf(eqn.lhs.t); // coefficient of same variable in this
    if (d != null) {
      BigInteger g = c.gcd(d); // calculate the gcd, ...
      c = c.divide(g); // ... scale the two coefficients,
      d = d.divide(g); // ... and negate the second

      rhs = rhs.multiply(c); // scale this equation by c
      for (Terms ts = lhs; ts != null; ts = ts.next) {
        ts.coeff = ts.coeff.multiply(c);
      }
      addConst(eqn.rhs.multiply(d)); // subtract a version of eqn scaled by d
      d = d.negate();
      for (Terms ts = eqn.lhs; ts != null; ts = ts.next) {
        addTerm(ts.coeff.multiply(d), ts.t, ts.hint);
      }
    }
  }

  /** Return the coefficient of t in this equation, or null if t is not mentioned. */
  BigInteger coeffOf(Type t) {
    for (Terms ts = lhs; ts != null; ts = ts.next) {
      if (t == ts.t) {
        return ts.coeff;
      }
    }
    return null;
  }
}
