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

/** Represents an alternative in a case expression. */
abstract class EAlt {

  protected Position pos;

  protected Expr e;

  /** Default constructor. */
  EAlt(Position pos, Expr e) {
    this.pos = pos;
    this.e = e;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  abstract void indent(IndentOutput out, int n);

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  abstract DefVars inScopeOf(Handler handler, MILEnv milenv, Env env);

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
  void findAmbigTVars(TVars gens) throws Failure { // ... -> e
    e.findAmbigTVars(gens);
  }

  /**
   * Infer the type of value that is returned by this expression, also ensuring that the type of
   * value that is matched can be unified with the specified domtype.
   */
  abstract Type inferAltType(TVarsInScope tis, Type domtype) throws Failure;

  /**
   * Check that this alternative will match a value of the specified domtype and return a value of
   * the given resulttype.
   */
  abstract void checkAltType(TVarsInScope tis, Type domtype, Type resulttype) throws Failure;

  EAlt lift(LiftEnv lenv) { // ... -> e
    e = e.lift(lenv);
    return this;
  }

  abstract Alts compAlt(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join);

  abstract Alts compAltM(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join);
}
