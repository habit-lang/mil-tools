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

public class ApTypeExp extends TypeExp {

  private TypeExp l;

  private TypeExp r;

  /** Default constructor. */
  public ApTypeExp(TypeExp l, TypeExp r) {
    this.l = l;
    this.r = r;
  }

  /** Find a position for this type expression. */
  public Position position() {
    return l.position();
  }

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    l = l.tidyInfix(env);
    r = r.tidyInfix(env);
    return this;
  }

  /**
   * Determine a suitable fixity for this type expression. If the expression already has an
   * associated type constructor For type constructors, then we use the fixity associated with that
   * (if there is one). If the expression is an application, then we look for a fixity in the
   * function part. But if no suitable fixity can be found, then we just use Fixity.unspecified.
   */
  public Fixity getFixity() {
    return l.getFixity();
  }

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    // descend tree with one extra argument
    l = l.scopeType(params, tdeps, env, arity + 1);
    // reset the arity count for each argument
    r = r.scopeType(params, tdeps, env, 0);
    return this;
  }

  public Kind inferKind() throws Failure {
    Kind rng = new KVar();
    l.checkKind(new KFun(r.inferKind(), rng));
    return rng;
  }

  public Type toType(Prefix prefix) throws Failure {
    return new TAp(l.toType(prefix), r.toType(prefix));
  }

  /**
   * Analyze the left hand side of a data definition. It must be possible to determine the name and
   * arity of a datatype without relying on fixity information. Uses of multiple infix operators
   * without parentheses to show exact grouping will result in the construction of an InfixTypeExp
   * object during parsing, which will trigger a syntax error here. (In practice, using multiple
   * infix operators in the header of a data definition would not be syntactically valid anyway.)
   */
  String analyzeDataDefnHead(TyvarEnv params) throws Failure {
    // Analyze left-to-right to preserve order in source expr.
    String id = l.analyzeDataDefnHead(params);
    r.addTypeParam(params);
    return id;
  }
}
