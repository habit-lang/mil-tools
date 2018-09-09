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

class ECase extends PosExpr {

  private Expr e;

  private EAlt[] alts;

  /** Default constructor. */
  ECase(Position pos, Expr e, EAlt[] alts) {
    super(pos);
    this.e = e;
    this.alts = alts;
  }

  void display(Screen s) {
    int ind = s.getIndent();
    s.print("case ");
    e.display(s);
    s.print(" of");
    for (int i = 0; i < alts.length; i++) {
      s.indent(ind + 2);
      alts[i].display(s);
    }
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "ECase");
    e.indent(out, n + 1);
    for (int i = 0; i < alts.length; i++) {
      alts[i].indent(out, n + 1);
    }
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { //  case e of alts
    DefVars fvs = e.inScopeOf(handler, milenv, env);
    for (int i = 0; i < alts.length; i++) {
      fvs = DefVars.add(alts[i].inScopeOf(handler, milenv, env), fvs);
    }
    return fvs;
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
  void findAmbigTVars(TVars gens) throws Failure { // case e of alts
    e.findAmbigTVars(gens);
    for (int i = 0; i < alts.length; i++) {
      alts[i].findAmbigTVars(gens);
    }
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // case e of alts
    // calculate type of discriminant:
    Type et = e.inferType(tis); // discriminant type

    if (alts == null || alts.length == 0) { // no alternatives =>
      return new TVar(Tyvar.res); //  result type unconstrained
    }

    type = alts[0].inferAltType(tis, et); // result type for first alternative
    for (int i = 1; i < alts.length; i++) { // should also be the result type of
      alts[i].checkAltType(tis, et, type); // the remaining alternatives ...
    }
    return type;
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // case e of alts
    Type et = e.inferType(tis); // calculate discriminant type
    if (alts != null) { // check each alternative:
      for (int i = 0; i < alts.length; i++) {
        alts[i].checkAltType(tis, et, t);
      }
    }
    type = t;
  }

  Expr lift(LiftEnv lenv) { // case e of alts
    e = e.lift(lenv);
    for (int i = 0; i < alts.length; i++) {
      alts[i] = alts[i].lift(lenv);
    }
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { //  case e of alts
    return e.compAtom(
        env,
        new AtomCont() {
          Code with(final Atom dv) {
            Temp rv = new Temp(); // holds the final result
            Block join = new Block(pos, kt.with(new Return(rv)));
            Alt[] talts = new Alt[alts.length];
            for (int i = 0; i < alts.length; i++) {
              talts[i] = alts[i].compAlt(env, abort, dv, rv, join);
            }
            return new Case(dv, talts, new BlockCall(abort));
          }
        });
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { //  case e of alts
    // TODO: too much cut and paste from compTail version
    return e.compAtom(
        env,
        new AtomCont() {
          Code with(final Atom dv) {
            Temp rv = new Temp(); // holds the final result
            Block join = new Block(pos, kt.with(new Return(rv)));
            Alt[] talts = new Alt[alts.length];
            for (int i = 0; i < alts.length; i++) {
              talts[i] = alts[i].compAltM(env, abort, dv, rv, join);
            }
            return new Case(dv, talts, new BlockCall(abort));
          }
        });
  }
}
