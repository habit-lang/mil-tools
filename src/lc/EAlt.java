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

/** Represents an alternative in a case expression. */
class EAlt {

  private Position pos;

  private String id;

  private DefVar[] vs;

  private Expr e;

  /** Default constructor. */
  EAlt(Position pos, String id, DefVar[] vs, Expr e) {
    this.pos = pos;
    this.id = id;
    this.vs = vs;
    this.e = e;
  }

  void display(Screen s) {
    int ind = s.getIndent();
    s.print(id);
    s.print(" ");
    s.print(vs);
    s.print(" ->");
    s.indent(ind + 2);
    e.display(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, "EAlt");
    out.indent(n + 1, DefVar.toString(id + " ", vs));
    e.indent(out, n + 1);
  }

  private Cfun cf;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { //  cf vs -> e
    cf = milenv.findCfun(id);
    if (cf == null) {
      handler.report(new UnknownConstructorFailure(pos, id));
    } else if (cf.getArity() != vs.length) {
      handler.report(new ConstructorArgsFailure(pos, cf));
    }
    return DefVars.remove(vs, e.inScopeOf(handler, milenv, new VarsEnv(env, vs)));
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
  void findAmbigTVars(TVars gens) throws Failure { // cf vs -> e
    e.findAmbigTVars(gens);
  }

  /**
   * Infer the type of value that is returned by this expression, also ensuring that the type of
   * value that is matched can be unified with the specified domtype.
   */
  Type inferAltType(TVarsInScope tis, Type domtype) throws Failure { // alternative, cf vs -> e
    lambdaType(domtype) // bind types of vs by unifying with
        .unify(pos, cf.getDeclared().instantiate()); // a fresh instance of cf's type
    return e.inferType(new TVISVars(tis, vs));
  }

  /**
   * Set each of the variables in the given array to have a (distinct) fresh type variable as their
   * type and return a function type with an argument for each of the variables.
   */
  Type lambdaType(Type result) {
    for (int i = vs.length; --i >= 0; ) {
      result = Type.fun(vs[i].freshType(Tyvar.arg), result);
    }
    return result;
  }

  /**
   * Check that this alternative will match a value of the specified domtype and return a value of
   * the given resulttype.
   */
  void checkAltType(TVarsInScope tis, Type domtype, Type resulttype)
      throws Failure { // alternative, cf vs -> e
    lambdaType(domtype) // bind types of vs by unifying with
        .unify(pos, cf.getDeclared().instantiate()); // a fresh instance of cf's type
    e.checkType(new TVISVars(tis, vs), resulttype);
  }

  EAlt lift(LiftEnv lenv) { // cf vs -> e
    e = e.lift(lenv);
    return this;
  }

  Alt compAlt(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Block join) { // cf vs -> e
    Temp[] ts = DefVar.freshTemps(vs);
    return makeAlt(
        dv,
        ts,
        e.compTail(
            new CGEnvVars(env, vs, ts),
            abort,
            new TailCont() {
              Code with(final Tail t) {
                return new Bind(r, t, new Done(new BlockCall(join)));
              }
            }));
  }

  Alt makeAlt(Atom dv, Temp[] ts, Code code) {
    // Add selectors to extract components:
    for (int i = ts.length - 1; i >= 0; i--) {
      code = new Bind(ts[i], new Sel(cf, i, dv), code);
    }
    return new Alt(cf, new BlockCall(new Block(pos, code)));
  }

  Alt compAltM(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Block join) { // cf args -> e
    Temp[] ts = DefVar.freshTemps(vs);
    return makeAlt(
        dv,
        ts,
        e.compTailM(
            new CGEnvVars(env, vs, ts),
            abort,
            new TailCont() {
              Code with(final Tail t) {
                return new Bind(r, t, new Done(new BlockCall(join)));
              }
            }));
  }
}
