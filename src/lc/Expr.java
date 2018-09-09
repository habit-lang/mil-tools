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
import debug.Screen;
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
          new EAlt(post, trueName, Var.noVars, trueBranch),
          new EAlt(posf, falseName, Var.noVars, falseBranch)
        });
  }

  abstract void display(Screen s);

  void displayParen(Screen s) {
    s.print("(");
    display(s);
    s.print(")");
  }

  void displayDo(Screen s) {
    display(s);
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

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // default, used for EVar, ELit, EType
    inferType(tis).unify(getPos(), t);
  }

  abstract Expr lift(LiftEnv lenv);

  /** Compile an expression into an Atom. */
  Code compAtom(final CGEnv env, final AtomCont ka) {
    return compTail(
        env,
        MILProgram.abort,
        new TailCont() {
          Code with(final Tail t) {
            Temp v = new Temp();
            return new Bind(v, t, ka.with(v));
          }
        });
  }

  /** Compile an expression into a Tail. */
  abstract Code compTail(final CGEnv env, final Block abort, final TailCont kt);

  Code compLet(CGEnv env, BindingSCCs sccs, Block abort, TailCont kt) {
    if (sccs == null) {
      return this.compTail(env, abort, kt);
    } else {
      Binding b = sccs.head.getNonRecBinding();
      Temp t = new Temp();
      return b.compBinding(env, t, this.compLet(new CGEnvVar(env, b, t), sccs.next, abort, kt));
    }
  }

  /** Compile a monadic expression into a Tail. */
  abstract Code compTailM(final CGEnv env, final Block abort, final TailCont kt);

  Code compLetM(CGEnv env, BindingSCCs sccs, Block abort, TailCont kt) {
    if (sccs == null) {
      return this.compTailM(env, abort, kt);
    } else {
      Binding b = sccs.head.getNonRecBinding();
      Temp t = new Temp();
      return b.compBinding(env, t, this.compLetM(new CGEnvVar(env, b, t), sccs.next, abort, kt));
    }
  }
}
