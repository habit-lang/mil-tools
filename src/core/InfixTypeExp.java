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

class InfixTypeExp extends TypeExp {

  private TypeOps typeOps;

  private TypeExp t;

  /** Default constructor. */
  InfixTypeExp(TypeOps typeOps, TypeExp t) {
    this.typeOps = typeOps;
    this.t = t;
  }

  /** Find a position for this type expression. */
  public Position position() {
    return typeOps.position();
  }

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    typeOps.tidyInfix(env);
    if (t != null) {
      t = t.tidyInfix(env);
    }
    return typeOps.tidyInfix(t); // Removes the InfixTypeExp object
  }

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    debug.Internal.error("scopeType on InfixTypeExp");
    return this; // not reached
  }

  public Kind inferKind() throws KindMismatchFailure {
    // shouldn't occur; eliminated during scope analysis
    // TODO: report fatal error instead ...
    return null;
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    // Nothing to do here; should not be called.
    return depends;
  }
}
