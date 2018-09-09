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

class EAp extends Expr {

  private Expr f;

  private Expr x;

  /** Default constructor. */
  EAp(Expr f, Expr x) {
    this.f = f;
    this.x = x;
  }

  /** Return a source code position for this expression. */
  Position getPos() {
    return f.getPos();
  }

  /**
   * Check that this expression is a valid left hand side (an application of an identifier to a
   * sequence arguments, each of which must be an identifier) and return the total number of
   * arguments, given that args of those arguments have already been checked.
   */
  int mustBeLhs(int args) throws Failure {
    return f.mustBeLhs(args + 1);
  }

  /**
   * Construct an equation using this expression as the left hand side, filling in the arguments as
   * we find them in vs, and using rhs as the right hand side.
   */
  Equation makeEquation(Position pos, DefVar[] vs, int n, Expr rhs) throws Failure {
    vs[--n] = x.asLamVar(null);
    return f.makeEquation(pos, vs, n, rhs);
  }

  void display(Screen s) {
    f.display(s);
    s.space();
    x.displayParen(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EAp");
    f.indent(out, n + 1);
    x.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) {
    return DefVars.add(f.inScopeOf(handler, milenv, env), x.inScopeOf(handler, milenv, env));
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
  void findAmbigTVars(TVars gens) throws Failure { // f x
    f.findAmbigTVars(gens);
    x.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // f x
    type = new TVar(Tyvar.res);
    f.checkType(tis, Type.fun(x.inferType(tis), type));
    return type;
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // f x
    f.checkType(tis, Type.fun(x.inferType(tis), type = t));
  }

  EAp(Expr f, Expr x, Type type) {
    this(f, x);
    this.type = type;
  }

  Expr lift(LiftEnv lenv) { // f x
    f = f.lift(lenv);
    x = x.lift(lenv);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { //  f x
    return f.compAtom(
        env,
        new AtomCont() {
          Code with(final Atom fv) {
            final Temp ft = new Temp();
            return new Bind(
                ft,
                new Sel(Cfun.Func, 0, fv), // TODO: do we need an assert too?
                x.compAtom(
                    env,
                    new AtomCont() {
                      Code with(final Atom xv) {
                        return kt.with(new Enter(ft, xv));
                      }
                    }));
          }
        });
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { //  f x
    return this.compAtom(
        env,
        new AtomCont() {
          Code with(final Atom v) {
            Temp t = new Temp();
            return new Bind(
                t,
                new Sel(Cfun.Proc, 0, v), // TODO: combine with EVar code above?
                kt.with(new Enter(t, Atom.noAtoms)));
          }
        });
  }
}
