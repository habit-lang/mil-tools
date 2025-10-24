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
import mil.*;

public abstract class TypeExp {

  /** Find a position for this type expression. */
  public abstract Position position();

  public abstract TypeExp tidyInfix(TyconEnv env) throws Failure;

  /**
   * Determine a suitable fixity for this type expression. If the expression already has an
   * associated type constructor For type constructors, then we use the fixity associated with that
   * (if there is one). If the expression is an application, then we look for a fixity in the
   * function part. But if no suitable fixity can be found, then we just use Fixity.unspecified.
   */
  public Fixity getFixity() {
    return Fixity.unspecified;
  }

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructors to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public TypeExp scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    return this.tidyInfix(env).scopeTypeRewrite(canAdd, params, env, arity);
  }

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public abstract TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure;

  public abstract Kind inferKind() throws KindMismatchFailure;

  public void checkKind(Kind k) throws KindMismatchFailure {
    Kind k1 = this.inferKind();
    if (!k1.unify(k)) {
      throw new KindMismatchFailure(this, k, k1);
    }
  }

  public Type toType(Prefix prefix) throws Failure {
    throw new TypeSyntaxFailure(this);
  }

  public Scheme toScheme(TyconEnv env) throws Failure {
    TyvarEnv params = new TyvarEnv(); // Collect implicitly quantified variables
    TypeExp t = this.scopeType(true, params, env, 0); // Run scope analysis on the type expression
    t.checkKind(KAtom.STAR); // Run kind inference
    Prefix prefix = params.toPrefix(); // Calculate a prefix for the type scheme
    return prefix.forall(t.toType(prefix));
  }

  /**
   * Analyze a type parameter to check basic syntax and return a corresponding Tyvar. A Kind
   * parameter is used to track the declared kind of the parameter (if specified) or else is set to
   * null, indicating that the required kind is not yet known. The kind of the returned Tyvar will
   * be set to the declared kind, if known, or else to a fresh kind variable to be bound during kind
   * inference.
   */
  public Tyvar scopeTypeParam(Kind kind) throws Failure {
    throw new TypeParamSyntaxFailure(this);
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public abstract CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends);

  void checkKind(Handler handler, Kind kind) {
    try {
      this.checkKind(kind);
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /** Validate this type expression as a valid alignment, and return that alignment as a long. */
  public long calcAlignment() throws Failure {
    Type alignType = this.toType(null).simplifyNatType(null);
    BigInteger alignBig = alignType.getNat();
    if (alignBig == null) {
      throw new Failure(position(), "Cannot determine alignment from " + alignType);
    } else if (alignBig.signum() <= 0 || alignBig.compareTo(Word.maxSigned()) > 0) {
      throw new Failure(
          position(), "Alignment " + alignBig + " is out of range (1 to " + Word.maxSigned() + ")");
    }
    return alignBig.longValue();
  }

  /** Check that a specified alignment is divisible by the minimum alignment. */
  public void checkAlignment(long alignment, long minAlignment) throws Failure {
    if ((alignment % minAlignment) != 0) {
      throw new Failure(
          position(),
          "Declared alignment "
              + alignment
              + " is not a multiple of minimal alignment "
              + minAlignment);
    }
  }
}
