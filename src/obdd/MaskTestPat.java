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

public class MaskTestPat extends Pat {

  private BigInteger mask;

  private BigInteger bits;

  private boolean op;

  private MaskTestPat(int width, OBDD bdd, boolean op) {
    super(width, bdd);
    this.op = op;
    this.mask = bdd.mask(op);
    this.bits = bdd.bits(op);
  }

  public MaskTestPat(Pat p, boolean op) {
    this(p.width, p.bdd.masktest(op), op);
  }

  public BigInteger getMask() {
    return mask;
  }

  public BigInteger getBits() {
    return bits;
  }

  public boolean getOp() {
    return op;
  }

  public String toString(String name) {
    StringBuilder buf = new StringBuilder();
    buf.append("pred");
    buf.append(name);
    buf.append("(x :: Bit ");
    buf.append(width);
    buf.append(") = ");
    if (mask.signum() == 0) {
      buf.append(op ^ (bits.signum() == 0) ? "true" : "false");
    } else if (BigInteger.ZERO.setBit(width).subtract(mask).compareTo(BigInteger.ONE) == 0) {
      // If all bits are included in the mask, then we don't show it here.  But note that
      // some masking will still be required in a practical implementation if the pattern
      // width is not an exact multiple of the wordsize.
      buf.append("x ");
      buf.append(op ? "!=" : "==");
      buf.append(" 0b");
      buf.append(bits.toString(2));
    } else {
      buf.append("(x & 0b");
      buf.append(mask.toString(2));
      buf.append(") ");
      buf.append(op ? "!=" : "==");
      buf.append(" 0b");
      buf.append(bits.toString(2));
    }
    return buf.toString();
  }

  public MaskTestPat blur(Pat butnot) {
    OBDD nbdd = bdd;
    OBDD cand;
    while ((cand = nbdd.blur()) != null && cand.and(butnot.bdd).isConst(false)) {
      nbdd = cand;
    }
    return nbdd == bdd ? this : new MaskTestPat(width, nbdd, op);
  }
}
