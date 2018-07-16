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
package core;

import compiler.*;
import java.math.BigInteger;
import mil.*;

class BitdataTagbitsExp extends BitdataRegionExp {

  private Position pos;

  private BigInteger nat;

  private int width;

  /** Default constructor. */
  BitdataTagbitsExp(Position pos, BigInteger nat, int width) {
    this.pos = pos;
    this.nat = nat;
    this.width = width;
  }

  /**
   * Perform scope analysis on portions of a type constructor definition, returning a list with the
   * elements from defns that it references.
   */
  CoreDefns scopeTycons(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    return depends;
  }

  public void kindInfer(Handler handler) {
    /* nothing to do */
  }

  /**
   * Validate the type and size of this region expression and add a term to the specified linear
   * equation to characterize the overall size (possibly including multiple fields)
   */
  void addTermTo(LinearEqn eqn) throws Failure {
    size = (width >= 0) ? new TNat(width) : new TVar(Tyvar.nat);
    type = Type.bit(size);
    eqn.addTerm(size, nat);
  }

  /**
   * Calculate a bit pattern for this region, and all of the regions to its right, given a bit
   * pattern for the latter. Also calculates the offset for each region within the overall layout.
   */
  obdd.Pat calcPat(obdd.Pat pat) throws Failure {
    width = size.simplifyNatType(null).getNat().intValue();
    if (width < nat.bitLength()) {
      throw new LiteralOutOfRangeFailure(pos, nat, type);
    }
    offset = pat.getWidth();
    return obdd.Pat.intmod(width, nat, offset).and(pat);
  }

  /**
   * Calculate the tagbits for this region, and all of the regions to its right, given the tagbits
   * for the latter. Assumes that the offsets for each tagbits region have already been calculated
   * as the result of a previous calcPat call.
   */
  BigInteger calcTagbits(BigInteger tagbits) {
    return tagbits.or(nat.shiftLeft(offset));
  }

  /** Count the number of fields within this bitdata or structure region. */
  int numFields() {
    return 0;
  }

  /**
   * Fill in the fields of the array, with n fields currently unfilled, moving from right (least
   * significant bit) to left (most significant bit).
   */
  int collectFields(BitdataField[] fs, int next) throws Failure {
    return next;
  }
}
