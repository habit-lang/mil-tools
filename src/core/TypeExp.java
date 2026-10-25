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
import java.math.BigInteger;
import mil.*;

public abstract class TypeExp {

  /** Find a position for this type expression. */
  public abstract Position position();

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
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
   * Scope analysis on type expressions. Applies tidyInfix to resolve questions about order of
   * operations in expressions that use adjacent infix operators. This is followed by the main scope
   * analysis pass, which attempts to check that all of the type variables and constants defined
   * within a type expression are in scope. In particular, type variables are added to the params
   * environment, while type constructors are expected to have existing entries in the specified
   * TyconEnv. The tdeps parameter supports tracking of dependencies between the type constructors
   * defined in a group of CoreDefns, but is ignored if tdeps is set to null.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env) throws Failure {
    return this.tidyInfix(env).scopeType(params, tdeps, env, 0);
  }

  /** Convenience interface for scopeType when dependency tracking is not required (tdeps=null). */
  public TypeExp scopeType(TyvarEnv params, TyconEnv env) throws Failure {
    return scopeType(params, null, env);
  }

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public abstract TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure;

  public abstract Kind inferKind() throws Failure;

  public void checkKind(Kind kexpected) throws Failure {
    Kind kinferred = this.inferKind();
    if (!kinferred.unify(kexpected)) {
      throw new KindMismatchFailure(this, kexpected, kinferred);
    }
  }

  public abstract Type toType(Prefix prefix) throws Failure;

  public Scheme toScheme(TyconEnv env) throws Failure {
    // Collect implicitly quantified variables
    TyvarEnv params = new TyvarEnv();

    // Run scope analysis on the type expression
    TypeExp t = this.scopeType(params, env);

    // Run kind inference
    t.checkKind(KAtom.STAR);

    // Build type scheme
    Prefix prefix = params.toPrefix();
    return prefix.forall(this.toType(prefix));
  }

  /**
   * Analyze a type parameter to check basic syntax and add a corresponding Tyvar to the environment
   * of parameters.
   */
  public void addTypeParam(TyvarEnv params) throws Failure {
    addTypeParam(params, null);
  }

  /**
   * Check that this type expression can be interpreted as a valid type parameter, for example, in a
   * datatype definition. The kind parameter is used to capture a kind declared through a kind
   * annotation as we descend the abstract syntax tree. It should initially be set to null, and
   * hence will still be null if we reach a variable name without seeing any kind annotations.
   */
  public void addTypeParam(TyvarEnv params, Kind kind) throws Failure {
    throw new TypeParamSyntaxFailure(this);
  }

  /**
   * Analyze the left hand side of a data definition to find the name of the new type and to
   * populate an environment with its parameters.
   */
  String analyzeDataDefnLhs(TyvarEnv params) throws Failure {
    return analyzeDataDefnHead(params);
  }

  /**
   * Analyze the left hand side of a data definition. It must be possible to determine the name and
   * arity of a datatype without relying on fixity information. Uses of multiple infix operators
   * without parentheses to show exact grouping will result in the construction of an InfixTypeExp
   * object during parsing, which will trigger a syntax error here. (In practice, using multiple
   * infix operators in the header of a data definition would not be syntactically valid anyway.)
   */
  String analyzeDataDefnHead(TyvarEnv params) throws Failure {
    throw new DataLhsSyntaxFailure(this);
  }

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
