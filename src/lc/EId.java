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

class EId extends EVar {

  private String id;

  /** Default constructor. */
  EId(Position pos, Var v, String id) {
    super(pos, v);
    this.id = id;
  }

  EId(Position pos, String id) {
    this(pos, null, id); // v to be filled in later
  }

  /**
   * Check that this expression is a simple identifier, returning the associated string, or else
   * triggering a ParseFailure.
   */
  String mustBeId() throws ParseFailure {
    return id;
  }

  /**
   * Check that this expression is a valid left hand side (an application of an identifier to a
   * sequence arguments, each of which must be an identifier) and return the total number of
   * arguments, given that args of those arguments have already been checked.
   */
  int mustBeLhs(int args) throws Failure {
    return args;
  }

  /**
   * Construct an equation using this expression as the left hand side, filling in the arguments as
   * we find them in vs, and using rhs as the right hand side.
   */
  Equation makeEquation(Position pos, DefVar[] vs, int n, Expr rhs) throws Failure {
    return new Equation(pos, id, vs == null ? rhs : new ELam(pos, vs, rhs));
  }

  /**
   * Construct a LamVar from an Expr, allowing only identifiers, parentheses, and type signatures.
   * The argument is used to specify an existing type signature and should initially be set to null.
   */
  LamVar asLamVar(TypeExp texp) throws Failure {
    return new LamVar(id, texp);
  }

  void display(Screen s) {
    s.print(id);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EId: " + id);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) {
    if ((v = Env.find(id, env)) == null) { // look in local environment
      Top top = milenv.findTop(id); // look in milenv
      if (top != null) {
        v = new DecVar(top.clone());
      } else {
        Cfun cf = milenv.findCfun(id); // look for a constructor?
        if (cf != null) {
          v = new DecVar(cf.getTop());
        } else {
          handler.report(new NotInScopeFailure(pos, id));
          return null;
        }
      }
    }
    return v.add(null);
  }

  EId(Position pos, Var v, Type type) {
    this(pos, v.toString());
    this.v = v;
    this.type = type;
  }
}
