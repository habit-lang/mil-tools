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
package lc;

import compiler.*;
import core.*;
import mil.*;

class EType extends PosExpr {

  private Expr e;

  private TypeExp texp;

  /** Default constructor. */
  EType(Position pos, Expr e, TypeExp texp) {
    super(pos);
    this.e = e;
    this.texp = texp;
  }

  /**
   * Construct a LamVar from an Expr, allowing only identifiers, parentheses, and type signatures.
   * The argument is used to specify an existing type signature and should initially be set to null.
   */
  LamVar asLamVar(TypeExp texp) throws Failure {
    if (texp != null) {
      throw new Failure(texp.position(), "Multiple type annotations in variable binding");
    }
    return e.asLamVar(this.texp);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    String ts = (declared != null) ? declared.toString() : "_";
    indent(out, n, "EType :: " + ts);
    e.indent(out, n + 1);
  }

  private Scheme declared;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // e :: texp
    try {
      declared = texp.toScheme(milenv.getTyconEnv());
      texp = null;
    } catch (Failure f) {
      handler.report(f);
    }
    return e.inScopeOf(handler, milenv, env);
  }

  /**
   * Search for and report failures for "ambiguous" type variables in the types of identifiers that
   * are part of this AST node. A type variable is considered ambiguous if there is no way to
   * determine how it should be instantiated in any given use. The input argument specifies the list
   * of all generic type variables that appear in the type of the enclosing binding (whose
   * instantiation will be determined by the context in which the bound variable is used) as a
   * prefix of all of the "fixed" type variables (i.e., those that appear free in the environment,
   * and hence cannot be freely instantiated). Any type variables that do not appear in this list
   * may be considered ambiguous. Ambiguity arises in examples like "length Nil" where there is no
   * way to determine the type of the "Nil" list. Occurrences of ambiguous type variables must be
   * fixed by rewriting the code (for this example, "length Nil" is an unnecessarily complicated way
   * of writing "0") or by adding type information.
   */
  void findAmbigTVars(TVars gens) throws Failure { // e :: declared
    e.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // e :: declared
    return type = e.explicitlyTyped(pos, tis, declared);
  }

  Expr lift(LiftEnv lenv) { // e :: declared
    e = e.lift(lenv);
    return this;
  }

  /**
   * Compile an expression into a Tail. The continuation kt maps tails (of the same type as this
   * expression) to code sequences (that return a value of the type specified by kty).
   */
  Code compTail(
      final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // e :: declared
    return e.compTail(env, abort, kty, kt);
  }

  /**
   * Compile a monadic expression into a Tail. If this is an expression of type Proc T, then the
   * continuation kt maps tails (that produce values of type T) to code sequences (that return a
   * value of the type specified by kty).
   */
  Code compTailM(
      final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // e :: declared
    return e.compTailM(env, abort, kty, kt);
  }
}
