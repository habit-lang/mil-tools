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
import mil.*;

public class ApTypeExp extends TypeExp {

  private TypeExp l;

  private TypeExp r;

  /** Default constructor. */
  public ApTypeExp(TypeExp l, TypeExp r) {
    this.l = l;
    this.r = r;
  }

  /** Find a position for this type expression. */
  public Position position() {
    return l.position();
  }

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructors to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public void scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity) throws Failure {
    l.scopeType(canAdd, params, env, arity + 1);
    r.scopeType(canAdd, params, env, 0);
  }

  public Kind inferKind() throws KindMismatchFailure {
    Kind rng = new KVar();
    l.checkKind(new KFun(r.inferKind(), rng));
    return rng;
  }

  public Type toType(Prefix prefix) throws Failure {
    return new TAp(l.toType(prefix), r.toType(prefix));
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTycons(TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    return r.scopeTycons(params, env, defns, l.scopeTycons(params, env, defns, depends));
  }
}
