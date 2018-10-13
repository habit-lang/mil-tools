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

class EVar extends PosExpr {

  protected Var v;

  /** Default constructor. */
  EVar(Position pos, Var v) {
    super(pos);
    this.v = v;
  }

  void display(Screen s) {
    s.print(v.getId());
  }

  void displayParen(Screen s) {
    display(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EVar: " + v.getId());
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) {
    return v.add(null);
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
  void findAmbigTVars(TVars gens) throws Failure { // v
    // TODO: find a less ad hoc way to format this error message
    String extras = TVars.listAmbigTVars(type.tvars(gens), gens);
    if (extras != null) { // TODO: use a handler rather than an exception?
      throw new Failure(
          pos,
          "\""
              + v.getId()
              + "\" used at type "
              + type.skeleton()
              + " with no way to determine "
              + extras);
    }
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // v
    return type = v.instantiate();
  }

  Expr lift(LiftEnv lenv) { // v
    Lifting l = v.findLifting(lenv);
    return (l == null) ? this : l.replacement(pos, type);
  }

  /** Compile an expression into an Atom. */
  Code compAtom(final CGEnv env, final AtomCont ka) {
    return ka.with(v.lookup(env));
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) {
    return this.compAtom(
        env,
        new AtomCont() {
          Code with(final Atom la) {
            return kt.with(new Return(la));
          }
        });
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { //  v
    Temp t = new Temp();
    return new Bind(
        t,
        new Sel(Cfun.Proc, 0, v.lookup(env)), // t <- Proc 0 v
        kt.with(new Enter(t, Atom.noAtoms))); // t @ []
  }
}
