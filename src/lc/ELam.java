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

class ELam extends PosExpr {

  private DefVar[] vs;

  private Expr e;

  /** Default constructor. */
  ELam(Position pos, DefVar[] vs, Expr e) {
    super(pos);
    this.vs = vs;
    this.e = e;
  }

  void display(Screen s) {
    s.print("\\");
    s.print(vs);
    s.print(" -> ");
    e.display(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "ELam");
    out.indent(n + 1, DefVar.toString("\\", vs));
    e.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) {
    return DefVars.remove(vs, e.inScopeOf(handler, milenv, new VarsEnv(env, vs)));
  }

  /**
   * Test to determine whether this expression can be used on the right hand side of a binding in a
   * recursive binding group.
   */
  public boolean isSafeToRecurse() {
    return vs.length > 0 || e.isSafeToRecurse();
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
  void findAmbigTVars(TVars gens) throws Failure { // \vs -> e
    e.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // \vs -> e
    Type et = new TVar(Tyvar.res); // placeholder for body type
    type = lambdaType(et); // bind fresh type vars
    e.checkType(new TVISVars(tis, vs), et); // check body
    return type;
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

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // \vs -> e
    Type et = new TVar(Tyvar.res); // placeholder for body type
    lambdaType(et).unify(pos, type = t); // bind fresh type vars using t
    e.checkType(new TVISVars(tis, vs), et); // check body
  }

  public ELam(Position pos, DefVar[] vs, Expr e, Type type) {
    this(pos, vs, e);
    this.type = type;
  }

  Expr lift(LiftEnv lenv) { // \vs -> e
    e = e.lift(lenv);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { //  \vs -> e
    Temp[] ts = DefVar.freshTemps(vs);
    Call b =
        new BlockCall(
            new Block(
                pos, e.compTail(new CGEnvVars(env, vs, ts), MILProgram.abort, TailCont.done)));
    for (int i = vs.length - 1; i >= 0; i--) {
      Temp t = new Temp();
      Code c =
          new Bind(
              t,
              new ClosAlloc(new ClosureDefn(pos, new Temp[] {ts[i]}, b)),
              new Done(Cfun.Func.withArgs(t)));
      b = new BlockCall(new Block(pos, c));
    }
    return kt.with(b);
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(final CGEnv env, final Block abort, final TailCont kt) { //  \vs -> e
    debug.Internal.error("ELam does not have monadic type");
    return null;
  }
}
