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
import mil.*;

public class KindAnnTypeExp extends PosTypeExp {

  private TypeExp t;

  private KindExp k;

  /** Default constructor. */
  public KindAnnTypeExp(Position pos, TypeExp t, KindExp k) {
    super(pos);
    this.t = t;
    this.k = k;
  }

  private Kind kind = null;

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructor to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public void scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity) throws Failure {
    kind = k.toKind();
    t.scopeType(canAdd, params, env, arity);
  }

  public Kind inferKind() throws KindMismatchFailure {
    t.checkKind(kind);
    return kind;
  }

  public Type toType(Prefix prefix) throws Failure {
    // Kind annotations are discarded at this point
    return t.toType(prefix);
  }

  /**
   * Analyze a type parameter to check basic syntax and return a corresponding Tyvar. A Kind
   * parameter is used to track the declared kind of the parameter (if specified) or else is set to
   * null, indicating that the required kind is not yet known. The kind of the returned Tyvar will
   * be set to the declared kind, if known, or else to a fresh kind variable to be bound during kind
   * inference.
   */
  public Tyvar scopeTypeParam(Kind kind) throws Failure {
    this.kind = k.toKind();
    if (kind != null && !kind.same(this.kind)) {
      throw new MultipleKindsSpecifiedFailure(this, this.kind, kind);
    }
    return t.scopeTypeParam(this.kind);
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTycons(TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    kind = k.toKind();
    return t.scopeTycons(params, env, defns, depends);
  }
}
