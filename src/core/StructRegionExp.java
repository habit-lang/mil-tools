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
import lc.TopBindings;
import mil.*;

class StructRegionExp {

  private StructFieldExp[] fields;

  private TypeExp texp;

  /** Default constructor. */
  StructRegionExp(StructFieldExp[] fields, TypeExp texp) {
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
    texp.checkKind(handler, KAtom.AREA);
  }

  protected Type type;

  protected Type size;

  protected int offset;

  /**
   * Validate the type and size of this region expression and add a term to the specified linear
   * equation to characterize the overall size (possibly including multiple fields)
   */
  void addTermTo(LinearEqn eqn) throws Failure {
    type = texp.toType(null);
    size = type.byteSize(null);
    if (size == null) {
      throw new NoByteLevelRepresentationFailure(texp.position(), type);
    }
    int coeff = (fields == null) ? 1 : fields.length;
    eqn.addTerm(coeff, size.simplifyNatType(null), type);
  }

  int calcOffset(int offset) throws Failure {
    this.offset = offset;
    BigInteger nat = size.getNat();
    if (nat == null) {
      throw new FieldSizeNotDeterminedFailure(texp.position(), type);
    }
    width = nat.intValue();
    return offset + (fields == null ? width : (width * fields.length));
  }

  int collectFields(StructField[] fs, int next) {
    if (fields != null) {
      int o = offset;
      for (int i = 0; i < fields.length; i++) {
        fs[next++] = fields[i].makeField(type, o, width);
        o += width;
      }
    }
    return next;
  }

  private int width;

  /** Count the number of fields within this bitdata or structure region. */
  int numFields() {
    return (fields == null) ? 0 : fields.length;
  }

  public TopBindings coreBindings(TopBindings tbs) {
    if (fields != null) {
      for (int i = 0; i < fields.length; i++) {
        tbs = fields[i].coreBindings(tbs);
      }
    }
    return tbs;
  }
}
