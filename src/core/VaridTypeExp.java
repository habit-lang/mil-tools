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

public class VaridTypeExp extends PosTypeExp {

  private String id;

  /** Default constructor. */
  public VaridTypeExp(Position pos, String id) {
    super(pos);
    this.id = id;
  }

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    return this; // No infix operators in this type expression!
  }

  /** The Tyvar object for this type variable expression. */
  private Tyvar tyvar = null;

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    if (params == null) {
      throw new NotInScopeTyvarFailure(pos, id);
    } else if ((tyvar = params.find(id)) == null) {
      params.add(tyvar = new Tyvar(pos, id, new KVar()));
    }
    return this;
  }

  public Kind inferKind() throws Failure {
    // check for null to allow for earlier failure in scope analysis
    return (tyvar == null) ? new KVar() : tyvar.getKind();
  }

  public Type toType(Prefix prefix) throws Failure {
    // TODO: Is it possible for the find call to return a negative number
    // here (as a result of a previously detected scope analysis error that
    // has not, however, halted the analysis)?  Answer: Yes, which is why
    // there is a call to Math.max() here for now ... but figure a better
    // way to handle this!
    return Type.gen(prefix == null ? 0 : Math.max(0, prefix.find(tyvar)));
  }

  /**
   * Check that this type expression can be interpreted as a valid type parameter, for example, in a
   * datatype definition. The kind parameter is used to capture a kind declared through a kind
   * annotation as we descend the abstract syntax tree. It should initially be set to null, and
   * hence will still be null if we reach a variable name without seeing any kind annotations.
   */
  public void addTypeParam(TyvarEnv params, Kind kind) throws Failure {
    params.add(new Tyvar(pos, id, (kind == null) ? new KVar() : kind));
  }
}
