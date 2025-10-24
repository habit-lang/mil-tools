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

public class KindAnnTypeExp extends PosTypeExp {

  private TypeExp t;

  private KindExp k;

  /** Default constructor. */
  public KindAnnTypeExp(Position pos, TypeExp t, KindExp k) {
    super(pos);
    this.t = t;
    this.k = k;
  }

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    t = t.tidyInfix(env);
    return this;
  }

  /**
   * Determine a suitable fixity for this type expression. If the expression already has an
   * associated type constructor For type constructors, then we use the fixity associated with that
   * (if there is one). If the expression is an application, then we look for a fixity in the
   * function part. But if no suitable fixity can be found, then we just use Fixity.unspecified.
   */
  public Fixity getFixity() {
    return t.getFixity();
  }

  private Kind kind = null;

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    kind = k.toKind();
    t = t.scopeTypeRewrite(canAdd, params, env, arity);
    return this;
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
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    try {
      kind = k.toKind();
    } catch (Failure f) {
      handler.report(f);
    }
    return t.scopeTyconsType(handler, params, env, defns, depends);
  }
}
