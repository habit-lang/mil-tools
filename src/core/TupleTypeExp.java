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
import mil.*;

public class TupleTypeExp extends PosTypeExp {

  TypeExp[] texps;

  /** Default constructor. */
  public TupleTypeExp(Position pos, TypeExp[] texps) {
    super(pos);
    this.texps = texps;
  }

  /** Return the width (i.e., number of components) in this tuple type expression. */
  public int width() {
    return texps.length;
  }

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructors to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public void scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity) throws Failure {
    for (int i = 0; i < texps.length; i++) {
      texps[i].scopeType(canAdd, params, env, 0);
    }
  }

  public Kind inferKind() throws KindMismatchFailure {
    for (int i = 0; i < texps.length; i++) {
      texps[i].checkKind(KAtom.STAR);
    }
    return KAtom.TUPLE;
  }

  public Type toType(Prefix prefix) throws Failure {
    Type t = TupleCon.tuple(texps.length).asType();
    for (int i = 0; i < texps.length; i++) {
      t = new TAp(t, texps[i].toType(prefix));
    }
    return t;
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTycons(TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    for (int i = 0; i < texps.length; i++) {
      depends = texps[i].scopeTycons(params, env, defns, depends);
    }
    return depends;
  }
}
