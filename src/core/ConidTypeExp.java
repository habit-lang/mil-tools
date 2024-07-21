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

public class ConidTypeExp extends PosTypeExp {

  private String id;

  /** Default constructor. */
  public ConidTypeExp(Position pos, String id) {
    super(pos);
    this.id = id;
  }

  private Tycon tycon = null;

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructors to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public void scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity) throws Failure {
    if ((tycon = TyconEnv.findTycon(id, env)) == null) {
      throw new NotInScopeTyconFailure(pos, id);
    }
    // extended below ...

    // The following test should be unnecessary given the subsequent use of kind inference but may
    // result
    // in friendlier error diagnostics.
    if (arity > tycon.getArity()) {
      throw new TooManyTyconArgsFailure(pos, tycon, arity);
    }
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
  public CoreDefns scopeTycons(TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    // One of the locally defined types?
    CoreDefn defn = CoreDefns.find(id, defns);
    if (defn != null && (tycon = defn.getTycon()) != null) {
      return defn.uniqueCons(depends);
    }
    // Defined in the enclosing type environment?
    if ((tycon = TyconEnv.findTycon(id, env)) == null) {
      throw new NotInScopeTyconFailure(pos, id);
    }
    return depends;
  }
}
