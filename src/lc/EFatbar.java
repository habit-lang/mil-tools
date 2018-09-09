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

class EFatbar extends PosExpr {

  private Expr l;

  private Expr r;

  /** Default constructor. */
  EFatbar(Position pos, Expr l, Expr r) {
    super(pos);
    this.l = l;
    this.r = r;
  }

  void display(Screen s) {
    int ind = s.getIndent();
    l.display(s);
    s.indent(ind - 2);
    s.print("| ");
    r.display(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EFatbar");
    l.indent(out, n + 1);
    r.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { //  l | r
    return DefVars.add(l.inScopeOf(handler, milenv, env), r.inScopeOf(handler, milenv, env));
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
  void findAmbigTVars(TVars gens) throws Failure { // l | r
    l.findAmbigTVars(gens);
    r.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // l | r
    type = l.inferType(tis); // find type of left
    r.checkType(tis, type); // and check for same on right
    return type;
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // l | r
    l.checkType(tis, type = t);
    r.checkType(tis, t);
  }

  Expr lift(LiftEnv lenv) { // l | r
    l = l.lift(lenv);
    r = r.lift(lenv);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { //  l | r
    final Temp rv = new Temp();
    final Block join = new Block(pos, kt.with(new Return(rv)));
    TailCont kt1 =
        new TailCont() {
          Code with(final Tail t) {
            return new Bind(rv, t, new Done(new BlockCall(join)));
          }
        };
    return l.compTail(env, new Block(pos, r.compTail(env, abort, kt1)), kt1);
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { //  l | r  ::  Proc argt
    // TODO: too much cut and paste from compTail version
    final Temp rv = new Temp();
    final Block join = new Block(pos, kt.with(new Return(rv)));
    TailCont kt1 =
        new TailCont() {
          Code with(final Tail t) {
            return new Bind(rv, t, new Done(new BlockCall(join)));
          }
        };
    return l.compTailM(env, new Block(pos, r.compTailM(env, abort, kt1)), kt1);
  }
}
