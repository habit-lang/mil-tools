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
package core;

import compiler.*;
import java.math.BigInteger;
import mil.*;

/** Represents arithmetic operations on natural numbers. */
public abstract class NatArithOp {

  public abstract String toString();

  public abstract BigInteger op(BigInteger m, BigInteger n) throws ArithmeticException;

  private static NatArithOp[] ops = {
    new NatPlusOp(),
    new NatMinusOp(),
    new NatMultOp(),
    new NatDivOp(),
    new NatPowOp(),
    new NatLshlOp(),
    new NatShrOp()
  };

  public static NatArithOp isOp(String s) {
    for (int i = 0; i < ops.length; i++) {
      if (s.equals(ops[i].toString())) {
        return ops[i];
      }
    }
    return null;
  }
}
