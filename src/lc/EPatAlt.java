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

class EPatAlt extends EAlt {

  private String id;

  private DefVar[] vs;

  /** Default constructor. */
  EPatAlt(Position pos, Expr e, String id, DefVar[] vs) {
    super(pos, e);
    this.id = id;
    this.vs = vs;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, "EPatAlt");
    out.indent(n + 1, DefVar.toString(id + " ", vs));
    e.indent(out, n + 2);
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
      result = Type.fun(vs[i].freshType(Tyvar.star), result);
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

  Alts compAlt(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join) { // cf vs -> e
    Temp[] ts = DefVar.freshTemps(vs);
    Code body =
        e.compTail(
            new CGEnvVars(env, vs, ts),
            abort,
            jty,
            new TailCont() {
              Code with(final Tail t) {
                return new Bind(r, t, new Done(new BlockCall(join)));
              }
            });
    return makeAlt(dv, ts, next, jty, body);
  }

  CfunAlt makeAlt(Atom dv, Temp[] ts, Alts next, Type jty, Code code) {
    // Add selectors to extract components:
    for (int i = ts.length - 1; i >= 0; i--) {
      code = new Bind(ts[i], new Sel(cf, i, dv), code);
    }
    return new CfunAlt(cf, new BlockCall(new LCBlock(pos, jty, code)), next);
  }

  Alts compAltM(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join) { // cf vs -> e
    Temp[] ts = DefVar.freshTemps(vs);
    Code body =
        e.compTailM(
            new CGEnvVars(env, vs, ts),
            abort,
            jty,
            new TailCont() {
              Code with(final Tail t) {
                return new Bind(r, t, new Done(new BlockCall(join)));
              }
            });
    return makeAlt(dv, ts, next, jty, body);
  }
}
