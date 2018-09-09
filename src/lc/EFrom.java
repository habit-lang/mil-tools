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

class EFrom extends PosExpr {

  private DefVar v;

  private Expr e;

  private Expr e1;

  /** Default constructor. */
  EFrom(Position pos, DefVar v, Expr e, Expr e1) {
    super(pos);
    this.v = v;
    this.e = e;
    this.e1 = e1;
  }

  void display(Screen s) {
    int ind = s.getIndent();
    s.print("do ");
    displayDo(s);
  }

  void displayDo(Screen s) {
    int ind = s.getIndent();
    s.print(v.toString());
    s.print(" <- ");
    e.display(s);
    s.indent(ind);
    e1.displayDo(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EFrom");
    out.indent(n + 1, v.getId());
    e.indent(out, n + 1);
    e1.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // v <- e; e1
    return DefVars.add(
        e.inScopeOf(handler, milenv, env),
        v.remove(e1.inScopeOf(handler, milenv, new VarEnv(env, v))));
  }

  /**
   * Test to determine whether this expression can be used on the right hand side of a binding in a
   * recursive binding group.
   */
  public boolean isSafeToRecurse() {
    return true;
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
  void findAmbigTVars(TVars gens) throws Failure { // id<-e; e1
    e.findAmbigTVars(gens);
    e1.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // v <- e; e1
    Type proc = DataType.proc.asType();
    e.checkType(tis, new TAp(proc, v.freshType(Tyvar.arg)));
    type = new TAp(proc, new TVar(Tyvar.res));
    e1.checkType(new TVISVar(tis, v), type);
    return type;
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // v <- e; e1
    Type proc = DataType.proc.asType();
    e.checkType(tis, new TAp(proc, v.freshType(Tyvar.arg)));
    t.unify(pos, new TAp(proc, new TVar(Tyvar.res)));
    e1.checkType(new TVISVar(tis, v), type = t);
  }

  Expr lift(LiftEnv lenv) { // v <- e; e1
    e = e.lift(lenv);
    e1 = e1.lift(lenv);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { // v <- e; e1
    // returns t <- k{...}; kt.with(Proc(t))
    // where k{...} [] = b(...)
    //       b(...)    = monadic code for v <- e; e1
    Temp t = new Temp();
    return new Bind(
        t,
        new ClosAlloc(
            new ClosureDefn(
                pos,
                Temp.noTemps,
                new BlockCall(
                    new Block(pos, this.compTailM(env, MILProgram.abort, TailCont.done))))),
        kt.with(Cfun.Proc.withArgs(t)));
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { // v <- e; e1
    return e.compTailM(
        env,
        MILProgram.abort,
        new TailCont() {
          Code with(final Tail t) {
            Temp t1 = new Temp();
            return new Bind(t1, t, e1.compTailM(new CGEnvVar(env, v, t1), MILProgram.abort, kt));
          }
        });
  }
}
