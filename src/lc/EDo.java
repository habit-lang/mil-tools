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

class EDo extends PosExpr {

  private Expr e;

  /** Default constructor. */
  EDo(Position pos, Expr e) {
    super(pos);
    this.e = e;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EDo");
    e.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // do e
    return e.inScopeOf(handler, milenv, env);
  }

  /**
   * Test to determine whether this expression can be used on the right hand side of a binding in a
   * recursive binding group.
   */
  boolean isSafeToRecurse() {
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
  void findAmbigTVars(TVars gens) throws Failure { // do e
    e.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // do e
    type = new TAp(DataType.proc.asType(), new TVar(Tyvar.res));
    e.checkType(tis, type);
    return type;
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // do e
    t.unify(pos, new TAp(DataType.proc.asType(), new TVar(Tyvar.res)));
    e.checkType(tis, type = t);
  }

  Expr lift(LiftEnv lenv) { // do e
    e = e.lift(lenv);
    return this;
  }

  /**
   * Compile an expression into a Tail. The continuation kt maps tails (of the same type as this
   * expression) to code sequences (that return a value of the type specified by kty).
   */
  Code compTail(final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // do e
    // returns t <- k{...}; kt.with(Proc(t))
    // where k{...} [] = b[...]
    //       b[...]    = monadic code for e
    Type rty = type.argOf(null); // Result type for this expression (i.e., e :: Proc rty)
    Type cty = Type.milfun(Type.empty, Type.tuple(rty));
    Temp t = new Temp(cty);
    return new Bind(
        t,
        new ClosAlloc(
            new LCClosureDefn(
                pos,
                cty,
                Temp.noTemps,
                new BlockCall(
                    new LCBlock(
                        pos, rty, e.compTailM(env, MILProgram.abort, rty, TailCont.done))))),
        kt.with(Cfun.Proc.withArgs(t)));
  }

  /**
   * Compile a monadic expression into a Tail. If this is an expression of type Proc T, then the
   * continuation kt maps tails (that produce values of type T) to code sequences (that return a
   * value of the type specified by kty).
   */
  Code compTailM(final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // do e
    return e.compTailM(env, abort, kty, kt);
  }
}
