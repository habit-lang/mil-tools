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
package lc;

import compiler.*;
import core.*;
import mil.*;

/** A base class for representing the abstract syntax of LC expressions. */
abstract class Expr {

  /** Return a source code position for this expression. */
  abstract Position getPos();

  /**
   * Check that this expression is a simple identifier, returning the associated string, or else
   * triggering a ParseFailure.
   */
  String mustBeId() throws ParseFailure {
    throw new ParseFailure(getPos(), "Identifier required");
  }

  /**
   * Check that this expression is a valid left hand side (an application of an identifier to a
   * sequence arguments, each of which must be an identifier) and return the total number of
   * arguments, given that args of those arguments have already been checked.
   */
  int mustBeLhs(int args) throws Failure {
    throw new ParseFailure(getPos(), "invalid syntax for the left hand side of a definition");
  }

  /**
   * Construct an equation using this expression as the left hand side, filling in the arguments as
   * we find them in vs, and using rhs as the right hand side.
   */
  Equation makeEquation(Position pos, DefVar[] vs, int n, Expr rhs) throws Failure {
    debug.Internal.error("makeEquation fails; was mustBeLhs called first?");
    return null;
  }

  /**
   * Construct a LamVar from an Expr, allowing only identifiers, parentheses, and type signatures.
   * The argument is used to specify an existing type signature and should initially be set to null.
   */
  LamVar asLamVar(TypeExp texp) throws Failure {
    throw new Failure(getPos(), "Syntax error in variable binding");
  }

  public static final String trueName = "True";

  public static final Expr trueCon = new EId(BuiltinPosition.pos, trueName);

  public static final String falseName = "False";

  public static final Expr falseCon = new EId(BuiltinPosition.pos, falseName);

  /**
   * The abstract syntax for LC does not have an if-then-else construct, so we provide the following
   * helper for constructing an equivalent LC expression using ECase.
   */
  public static Expr ifthenelse(
      Position pos, Expr test, Position post, Expr trueBranch, Position posf, Expr falseBranch) {
    return new ECase(
        pos,
        test,
        new EAlt[] {
          new EPatAlt(post, trueBranch, trueName, Var.noVars),
          new EPatAlt(posf, falseBranch, falseName, Var.noVars)
        });
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  abstract void indent(IndentOutput out, int n);

  public void indent(IndentOutput out, int n, String label) {
    if (type != null) {
      label += " :: " + type.skeleton();
    }
    out.indent(n, label);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  abstract DefVars inScopeOf(Handler handler, MILEnv milenv, Env env);

  /**
   * Test to determine whether this expression can be used on the right hand side of a binding in a
   * recursive binding group.
   */
  public boolean isSafeToRecurse() {
    return false;
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
  abstract void findAmbigTVars(TVars gens) throws Failure;

  /** Record the type of this expression as calculated by type inference. */
  protected Type type;

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  abstract Type inferType(TVarsInScope tis) throws Failure;

  Type explicitlyTyped(Position pos, TVarsInScope tis, Scheme declared) throws Failure {
    if (declared == null) {
      // ignore annotation if type scheme was invalid
      return inferType(tis);
    } else {
      Type type = declared.instantiate();
      checkType(tis, type);
      TVars fixed = TVarsInScope.tvarsInScope(tis);
      Scheme scheme = type.generalize(type.generics(fixed));
      if (!declared.alphaEquiv(scheme)) {
        throw new Failure(
            pos,
            "Declared type \""
                + declared
                + "\" is more general than inferred type \""
                + scheme
                + "\"");
      }
      return type;
    }
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // default, used for EVar, ELit, EType
    inferType(tis).unify(getPos(), t);
  }

  abstract Expr lift(LiftEnv lenv);

  /**
   * Generate code for an expression to be bound to a top-level variable, forcing the result to be a
   * tail by introducing a new Block, if necessary.
   */
  Tail compTopLevel(Position pos) {
    return compTail(null, MILProgram.abort, type, TailCont.done).forceTail(pos, type);
  }

  /**
   * Compile an expression into an Atom. The continuation ka expects an Atom (of the same type as
   * this expression) and produces a code sequence (that returns a value of the type kty).
   */
  Code compAtom(final CGEnv env, final Type kty, final AtomCont ka) {
    return compTail(
        env,
        MILProgram.abort,
        kty,
        new TailCont() {
          Code with(final Tail t) {
            Temp v = new Temp(type);
            return new Bind(v, t, ka.with(v));
          }
        });
  }

  /**
   * Compile an expression into a Tail. The continuation kt maps tails (of the same type as this
   * expression) to code sequences (that return a value of the type specified by kty).
   */
  abstract Code compTail(final CGEnv env, final Block abort, final Type kty, final TailCont kt);

  Code compLet(CGEnv env, BindingSCCs sccs, Block abort, Type kty, TailCont kt) {
    if (sccs == null) {
      return this.compTail(env, abort, kty, kt);
    } else {
      Binding b = sccs.head.getNonRecBinding();
      Temp t = new Temp(b.instantiate());
      return b.compBinding(
          env, t, kty, this.compLet(new CGEnvVar(env, b, t), sccs.next, abort, kty, kt));
    }
  }

  /**
   * Compile a monadic expression into a Tail. If this is an expression of type Proc T, then the
   * continuation kt maps tails (that produce values of type T) to code sequences (that return a
   * value of the type specified by kty).
   */
  abstract Code compTailM(final CGEnv env, final Block abort, final Type kty, final TailCont kt);

  Code compLetM(CGEnv env, BindingSCCs sccs, Block abort, Type kty, TailCont kt) {
    if (sccs == null) {
      return this.compTailM(env, abort, kty, kt);
    } else {
      Binding b = sccs.head.getNonRecBinding();
      Temp t = new Temp(b.instantiate());
      return b.compBinding(
          env, t, kty, this.compLetM(new CGEnvVar(env, b, t), sccs.next, abort, kty, kt));
    }
  }
}
