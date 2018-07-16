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

class EField extends Name {

  private Expr e;

  /** Default constructor. */
  EField(Position pos, String id, Expr e) {
    super(pos, id);
    this.e = e;
  }

  Position getPosition() {
    return pos;
  }

  void display(Screen s) { // id = e
    s.print(id);
    s.print("=");
    e.display(s);
  }

  static void display(Screen s, EField[] fields) {
    s.print("[");
    if (fields != null && fields.length > 0) {
      fields[0].display(s);
      for (int i = 1; i < fields.length; i++) {
        s.print(", ");
        fields[i].display(s);
      }
    }
    s.print("]");
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, "EField: " + id);
    e.indent(out, n + 1);
  }

  static void indent(IndentOutput out, int n, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fields[i].indent(out, n);
    }
  }

  /**
   * Perform a scope analysis on this expression, creating a Temp object for each variable binding,
   * checking that all of the identifiers that it references correspond to bound variables, and
   * returning the set of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // id = e
    return e.inScopeOf(handler, milenv, env);
  }

  static DefVars inScopeOf(Handler handler, MILEnv milenv, Env env, DefVars fvs, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fvs = DefVars.add(fvs, fields[i].inScopeOf(handler, milenv, env));
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
  void findAmbigTVars(TVars gens) throws Failure { // id = e
    e.findAmbigTVars(gens);
  }

  static void findAmbigTVars(TVars gens, EField[] fields) throws Failure {
    for (int i = 0; i < fields.length; i++) {
      fields[i].findAmbigTVars(gens);
    }
  }

  int checkTypeConstruct(TVarsInScope tis, Cfun cf, BitdataField[] lfields) throws Failure {
    int p = BitdataField.index(id, lfields);
    if (p < 0) {
      throw new Failure(
          pos, "Constructor " + cf + " does not include a field with label \"" + id + "\"");
    }
    // !System.out.println("Found field \"" + id + "\" at position " + p);
    e.checkType(tis, lfields[p].getType());
    return p;
  }

  BitdataField checkTypeUpdate(TVarsInScope tis, Type et, BitdataField[] lfields) throws Failure {
    int p = BitdataField.index(id, lfields);
    if (p < 0) {
      throw new Failure(pos, "There is no \"" + id + "\" field for type " + et);
    }
    e.checkType(tis, lfields[p].getType());
    return lfields[p];
  }

  static void lift(LiftEnv lenv, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fields[i].lift(lenv);
    }
  }

  void lift(LiftEnv lenv) {
    e = e.lift(lenv);
  }

  Code compTemp(final CGEnv env, final AtomCont ka) {
    return e.compTemp(env, ka);
  }

  Code compUpdate(CGEnv env, Tail t, final BitdataField field, final TailCont kt) {
    final Temp a = new Temp(); // a holds value to be updated
    return new Bind(
        a,
        t,
        e.compTemp(
            env,
            new AtomCont() {
              Code with(final Atom b) { // b holds value to insert
                return kt.with(field.getUpdatePrim().withArgs(a, b));
              }
            }));
  }
}
