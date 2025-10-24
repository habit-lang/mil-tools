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

public class VaridTypeExp extends PosTypeExp {

  private String id;

  /** Default constructor. */
  public VaridTypeExp(Position pos, String id) {
    super(pos);
    this.id = id;
  }

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    return this;
  }

  private Tyvar tyvar = null;

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    if (params == null || (tyvar = params.find(id)) == null) {
      if (canAdd) {
        params.add(tyvar = new Tyvar(pos, id, new KVar()));
      } else {
        throw new NotInScopeTyvarFailure(pos, id);
      }
    }
    return this;
  }

  public Kind inferKind() throws KindMismatchFailure {
    // check for null to allow for earlier failure in scope analysis
    return (tyvar == null) ? new KVar() : tyvar.getKind();
  }

  public Type toType(Prefix prefix) throws Failure {
    // TODO: Is it possible for the find call to return a negative number here (as a result of a
    // previously
    // detected scope analysis error that has not, however, halted the analysis)?  Answer: Yes,
    // which is why
    // there is a call to Math.max() here for now ... but figure a better way to handle this!
    return Type.gen(prefix == null ? 0 : Math.max(0, prefix.find(tyvar)));
  }

  /**
   * Analyze a type parameter to check basic syntax and return a corresponding Tyvar. A Kind
   * parameter is used to track the declared kind of the parameter (if specified) or else is set to
   * null, indicating that the required kind is not yet known. The kind of the returned Tyvar will
   * be set to the declared kind, if known, or else to a fresh kind variable to be bound during kind
   * inference.
   */
  public Tyvar scopeTypeParam(Kind kind) throws Failure {
    return new Tyvar(pos, id, (kind == null) ? new KVar() : kind);
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    if (params == null || (tyvar = params.find(id)) == null) {
      handler.report(new NotInScopeTyvarFailure(pos, id));
    }
    return depends;
  }
}
