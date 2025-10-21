/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

class EVarAlt extends EAlt {

  private DefVar v;

  /** Default constructor. */
  EVarAlt(Position pos, Expr e, DefVar v) {
    super(pos, e);
    this.v = v;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, "EVarAlt");
    out.indent(n + 1, v.toString());
    e.indent(out, n + 2);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { //  v -> e
    return DefVars.remove(v, e.inScopeOf(handler, milenv, new VarEnv(env, v)));
  }

  /**
   * Infer the type of value that is returned by this expression, also ensuring that the type of
   * value that is matched can be unified with the specified domtype.
   */
  Type inferAltType(TVarsInScope tis, Type domtype) throws Failure { // alternative, v -> e
    v.freshType(Tyvar.star)
        .unify(pos, domtype); // TODO: there should be a more direct way to set v's type
    return e.inferType(new TVISVar(tis, v));
  }

  /**
   * Check that this alternative will match a value of the specified domtype and return a value of
   * the given resulttype.
   */
  void checkAltType(TVarsInScope tis, Type domtype, Type resulttype)
      throws Failure { // alternative, v -> e
    v.freshType(Tyvar.star)
        .unify(pos, domtype); // TODO: there should be a more direct way to set v's type
    e.checkType(new TVISVar(tis, v), resulttype);
  }

  Alts compAlt(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join) { // v -> e
    Temp tv = v.freshTemp();
    Code body =
        new Bind(
            tv,
            new Return(dv),
            e.compTail(
                new CGEnvVar(env, v, tv),
                abort,
                jty,
                new TailCont() {
                  Code with(final Tail t) {
                    return new Bind(r, t, new Done(new BlockCall(join)));
                  }
                }));
    return new DefAlt(new BlockCall(new LCBlock(pos, jty, body)));
  }

  Alts compAltM(
      final CGEnv env,
      final Block abort,
      final Atom dv,
      final Temp r,
      final Alts next,
      final Type jty,
      final Block join) { // v -> e
    Temp tv = v.freshTemp();
    Code body =
        new Bind(
            tv,
            new Return(dv),
            e.compTailM(
                new CGEnvVar(env, v, tv),
                abort,
                jty,
                new TailCont() {
                  Code with(final Tail t) {
                    return new Bind(r, t, new Done(new BlockCall(join)));
                  }
                }));
    return new DefAlt(new BlockCall(new LCBlock(pos, jty, body)));
  }
}
