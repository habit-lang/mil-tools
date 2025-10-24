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
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    texp = texp.tidyInfix(env);
    return texp.scopeTyconsType(handler, params, env, defns, depends);
  }

  public void kindInfer(Handler handler) {
    texp.checkKind(handler, KAtom.AREA);
  }

  protected Type start;

  Type getStart() {
    return start;
  }

  String makeLabel() {
    return (fields == null)
        ? "padding"
        : (fields.length == 1) ? fields[0].getId() : "region for " + fields[0].getId();
  }

  LinearEqn structRegionEqn(Position pos, Type end, String endlabel) throws Failure {
    start = new TVar(Tyvar.nat); // We don't know the start of this region yet
    type = texp.toType(null); // But we should know the type ...
    size = type.byteSize(null); // .. and can look up it's width (which could be a variable)
    if (size == null) {
      throw new NoByteLevelRepresentationFailure(texp.position(), type);
    }
    // Build equation:  coeff * size = end - start
    int coeff = (fields == null) ? 1 : fields.length;
    String name = makeLabel();
    String label = "Size of " + name;
    LinearEqn eqn = new LinearEqn(pos, label);
    eqn.addTerm(start, "Offset of " + name);
    eqn.addTerm(coeff, size.simplifyNatType(null), label);
    eqn.addRhsTerm(end, endlabel);
    return eqn;
  }

  protected Type type;

  protected Type size;

  protected int offset;

  void validate() throws Failure {
    BigInteger nat = size.simplifyNatType(null).getNat();
    if (nat == null) {
      throw new FieldSizeNotDeterminedFailure(texp.position(), type);
    }
    width = nat.intValue();
    if ((nat = start.simplifyNatType(null).getNat()) == null) {
      throw new OffsetNotDeterminedFailure(texp.position(), type);
    }
    offset = nat.intValue();
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
