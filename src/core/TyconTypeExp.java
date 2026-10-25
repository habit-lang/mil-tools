/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    return this; // No infix operators in this type expression!
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
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    return tycon.scopeTycon(pos, arity);
  }

  public Kind inferKind() throws Failure {
    // check for null to allow for earlier failure in scope analysis
    return (tycon == null) ? new KVar() : tycon.getKind();
  }

  public Type toType(Prefix prefix) throws Failure {
    // TODO: Similar to the problem for VaridTypeExp, could tycon be null here?
    // TODO: bogus Type.gen(0) should be replaced with something better.
    return tycon == null ? Type.gen(0) : tycon.asType();
  }
}
