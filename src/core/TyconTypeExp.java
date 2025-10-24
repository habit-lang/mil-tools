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

public class TyconTypeExp extends PosTypeExp {

  private Tycon tycon;

  /** Default constructor. */
  public TyconTypeExp(Position pos, Tycon tycon) {
    super(pos);
    this.tycon = tycon;
  }

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    return this;
  }

  /**
   * Determine a suitable fixity for this type expression. If the expression already has an
   * associated type constructor For type constructors, then we use the fixity associated with that
   * (if there is one). If the expression is an application, then we look for a fixity in the
   * function part. But if no suitable fixity can be found, then we just use Fixity.unspecified.
   */
  public Fixity getFixity() {
    return tycon == null ? Fixity.unspecified : tycon.getFixity();
  }

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    return tycon.scopeTycon(pos, arity);
  }

  public Kind inferKind() throws KindMismatchFailure {
    // check for null to allow for earlier failure in scope analysis
    return (tycon == null) ? new KVar() : tycon.getKind();
  }

  public Type toType(Prefix prefix) throws Failure {
    // TODO: Similar to the problem for VaridTypeExp, could tycon be null here?
    // TODO: bogus Type.gen(0) should be replaced with something better.
    return tycon == null ? Type.gen(0) : tycon.asType();
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    return depends;
  }
}
