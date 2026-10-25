/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import lc.TopBindings;
import mil.*;

abstract class BitdataRegionExp {

  /**
   * Perform scope analysis on portions of a type constructor definition, returning a list with the
   * elements from defns that it references.
   */
  abstract void scopeDefn(Handler handler, TyvarEnv params, TyconDeps tdeps, TyconEnv env);

  public abstract void kindInfer(Handler handler);

  /** Records the type of the value(s) in this region. */
  protected Type type;

  /** Records the bit/byte size of the value(s) in this region. */
  protected Type size;

  /** Records the offset for the start of this region. */
  protected int offset;

  /**
   * Validate the type and size of this region expression and add a term to the specified linear
   * equation to characterize the overall size (possibly including multiple fields).
   */
  abstract void addTermTo(LinearEqn eqn) throws Failure;

  /**
   * Calculate a bit pattern for this region, and all of the regions to its right, given a bit
   * pattern for the latter. Also calculates the offset for each region within the overall layout. A
   * null result indicates that we were not (yet) able to determine a bit pattern for this region.
   */
  abstract obdd.Pat calcPat(obdd.Pat pat) throws Failure;

  /**
   * Calculate the tagbits for this region, and all of the regions to its right, given the tagbits
   * for the latter. Assumes that the offsets for each tagbits region have already been calculated
   * as the result of a previous calcPat call.
   */
  abstract BigInteger calcTagbits(BigInteger tagbits);

  /** Count the number of fields within this bitdata or structure region. */
  abstract int numFields();

  /**
   * Fill in the fields of the array, with n fields currently unfilled, moving from right (least
   * significant bit) to left (most significant bit).
   */
  abstract int collectFields(BitdataField[] fs, int next) throws Failure;

  public TopBindings coreBindings(TopBindings tbs) {
    return tbs;
  }
}
