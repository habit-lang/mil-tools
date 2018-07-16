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

/**
 * Represents a list of terms on the left hand side of a LinearEqn. We maintain the invariant that
 * all coefficients are non zero and that no variable appears more than once. The Type values stored
 * in a Terms list are assumed to have kind nat; this implies that t must point either to a TNat or
 * to an unbound TVar (possibly passing through some TInd, TGen, and/or bound TVars objects).
 */
class Terms {

  BigInteger coeff;

  Type t;

  Object hint;

  Terms next;

  /** Default constructor. */
  Terms(BigInteger coeff, Type t, Object hint, Terms next) {
    this.coeff = coeff;
    this.t = t;
    this.hint = hint;
    this.next = next;
  }

  /** Append a string representation of this equation to the specified buffer. */
  void appendTo(StringBuilder buf) {
    BigInteger c = coeff.abs();
    if (c.compareTo(BigInteger.ONE) > 0) {
      buf.append(c.toString());
      buf.append(" ");
    }
    if (hint == null) {
      buf.append(t.toString());
    } else {
      buf.append("Size(");
      buf.append(hint.toString());
      buf.append(")");
    }
  }
}
