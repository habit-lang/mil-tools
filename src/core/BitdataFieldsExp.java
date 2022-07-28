/*
    Copyright 2018-19 Mark P Jones, Portland State University

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

class BitdataFieldsExp extends BitdataRegionExp {

  private BitdataFieldExp[] fields;

  private TypeExp texp;

  /** Default constructor. */
  BitdataFieldsExp(BitdataFieldExp[] fields, TypeExp texp) {
    this.fields = fields;
    this.texp = texp;
  }

  /**
   * Perform scope analysis on portions of a type constructor definition, returning a list with the
   * elements from defns that it references.
   */
  CoreDefns scopeTycons(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    return texp.scopeTycons(handler, params, env, defns, depends);
  }

  public void kindInfer(Handler handler) {
    texp.checkKind(handler, KAtom.STAR);
  }

  /**
   * Validate the type and size of this region expression and add a term to the specified linear
   * equation to characterize the overall size (possibly including multiple fields)
   */
  void addTermTo(LinearEqn eqn) throws Failure {
    type = texp.toType(null);
    size = type.bitSize(null);
    if (size == null) {
      throw new NoBitLevelRepresentationFailure(texp.position(), type);
    }
    eqn.addTerm(fields.length, size.simplifyNatType(null), type);
  }

  private int width;

  /**
   * Calculate a bit pattern for this region, and all of the regions to its right, given a bit
   * pattern for the latter. Also calculates the offset for each region within the overall layout. A
   * null result indicates that we were not (yet) able to determine a bit pattern for this region.
   */
  obdd.Pat calcPat(obdd.Pat pat) throws Failure {
    offset = pat.getWidth();
    obdd.Pat fpat = type.bitPat(null);
    if (fpat == null) {
      return null;
    }
    width = fpat.getWidth();
    for (int i = fields.length; --i >= 0; ) {
      pat = fpat.concat(pat);
    }
    return pat;
  }

  /**
   * Calculate the tagbits for this region, and all of the regions to its right, given the tagbits
   * for the latter. Assumes that the offsets for each tagbits region have already been calculated
   * as the result of a previous calcPat call.
   */
  BigInteger calcTagbits(BigInteger tagbits) {
    return tagbits;
  }

  /** Count the number of fields within this bitdata or structure region. */
  int numFields() {
    return fields.length;
  }

  /**
   * Fill in the fields of the array, with n fields currently unfilled, moving from right (least
   * significant bit) to left (most significant bit).
   */
  int collectFields(BitdataField[] fs, int next) throws Failure {
    int o = offset + width * fields.length;
    for (int i = 0; i < fields.length; i++) {
      o -= width;
      fs[next++] = fields[i].makeField(type, o, width);
    }
    return next;
  }

  public TopBindings coreBindings(TopBindings tbs) {
    for (int i = 0; i < fields.length; i++) {
      tbs = fields[i].coreBindings(tbs);
    }
    return tbs;
  }
}
