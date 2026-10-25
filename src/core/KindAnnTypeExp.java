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

public class KindAnnTypeExp extends PosTypeExp {

  private TypeExp t;

  private KindExp k;

  /** Default constructor. */
  public KindAnnTypeExp(Position pos, TypeExp t, KindExp k) {
    super(pos);
    this.t = t;
    this.k = k;
  }

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
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

  /** The Kind for this kind annotation. */
  private Kind kind = null;

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    kind = k.toKind();
    t = t.scopeType(params, tdeps, env, arity);
    return this;
  }

  public Kind inferKind() throws Failure {
    t.checkKind(kind);
    return kind;
  }

  public Type toType(Prefix prefix) throws Failure {
    // Kind annotations are discarded at this point
    return t.toType(prefix);
  }

  /**
   * Check that this type expression can be interpreted as a valid type parameter, for example, in a
   * datatype definition. The kind parameter is used to capture a kind declared through a kind
   * annotation as we descend the abstract syntax tree. It should initially be set to null, and
   * hence will still be null if we reach a variable name without seeing any kind annotations.
   */
  public void addTypeParam(TyvarEnv params, Kind kind) throws Failure {
    this.kind = k.toKind();
    if (kind != null && !kind.same(this.kind)) {
      throw new MultipleKindsSpecifiedFailure(this, this.kind, kind);
    }
    t.addTypeParam(params, this.kind);
  }
}
