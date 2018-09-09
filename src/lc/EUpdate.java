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

class EUpdate extends PosExpr {

  private Expr e;

  private EField[] fields;

  /** Default constructor. */
  EUpdate(Position pos, Expr e, EField[] fields) {
    super(pos);
    this.e = e;
    this.fields = fields;
  }

  void display(Screen s) { // e [ fields ]
    e.displayParen(s);
    EField.display(s, "=", fields);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) { // e [ fields ]
    indent(out, n, "EUpdate");
    e.indent(out, n + 1);
    EField.indent(out, n + 1, fields);
  }

  private Cfun cf;

  private BitdataField[] bfields;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // e [ fields ]
    return EField.inScopeOf(handler, milenv, env, e.inScopeOf(handler, milenv, env), fields);
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
  void findAmbigTVars(TVars gens) throws Failure { // e [ fields ]
    e.findAmbigTVars(gens);
    EField.findAmbigTVars(gens, fields);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // e [ fields ]
    // TODO: refactor to avoid duplication of ESelect code ...
    Type et = e.inferType(tis).skeleton();
    BitdataType bt = et.bitdataType();
    BitdataLayout layout;
    if (bt == null) {
      layout = et.bitdataLayout();
      if (layout == null) {
        throw new Failure(pos, "Invalid update: no layout for " + et);
      }
      cf = null; // et is a layout type, so no outer constructor is involved
    } else {
      BitdataLayout[] layouts = bt.getLayouts();
      if (layouts.length != 1) {
        throw new Failure(pos, "Invalid update: " + et + " has multiple constructors");
      }
      layout = layouts[0];
      cf = bt.getCfuns()[0]; // record outer constructor
    }
    BitdataField[] lfields = layout.getFields(); // Fields from layout
    bfields = new BitdataField[fields.length]; // Fields to update
    for (int i = 0; i < fields.length; i++) {
      bfields[i] = fields[i].checkTypeUpdate(tis, et, lfields);
      for (int j = 0; j < i; j++) { // check that this field has only been updated once
        if (bfields[i] == bfields[j]) {
          throw new Failure(pos, "Multiple updates to field \"" + fields[i] + "\"");
        }
      }
    }
    return et;
  }

  Expr lift(LiftEnv lenv) { // e [ fields ]
    e = e.lift(lenv);
    EField.lift(lenv, fields);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) { // e [ fields ]
    // sketch of generated code:
    //     compAtom[e]   $ \t ->
    //     Bind(r <- Sel cf 0 t,
    //     compAtom[e1]  $ \v1 ->
    //     Bind(t1 <- update_lab1((v1, t)),
    //     compAtom[e2]  $ \v2 ->
    //     Bind(t2 <- update_lab2((v2, t1)),
    //     ...
    //     compAtom[eN]  $ \vN ->
    //     Bind(tN <- update_labN((vN, tN-1)),
    //     kt(C(tN))
    if (cf == null) {
      return e.compTail(env, MILProgram.abort, new UpdateCont(env, 0, kt));
    } else {
      final TailCont kn =
          new UpdateCont(
              env,
              0, // continuation to restore outer cfun at the end
              new TailCont() {
                Code with(final Tail t) {
                  Temp a = new Temp();
                  return new Bind(a, t, kt.with(new DataAlloc(cf).withArgs(a)));
                }
              });
      final TailCont ks = new TailCont() { // continuation to strip outer cfun at start
            Code with(final Tail t) {
              Temp a = new Temp();
              return new Bind(a, t, kn.with(new Sel(cf, 0, a)));
            }
          };
      return e.compTail(env, MILProgram.abort, ks);
    }
  }

  private class UpdateCont extends TailCont {

    private CGEnv env;

    private int i;

    private TailCont kt;

    /** Default constructor. */
    private UpdateCont(CGEnv env, int i, TailCont kt) {
      this.env = env;
      this.i = i;
      this.kt = kt;
    }

    Code with(final Tail t) {
      return (i >= fields.length)
          ? kt.with(t)
          : fields[i].compUpdate(env, t, bfields[i], new UpdateCont(env, i + 1, kt));
    }
  }

  /** Compile a monadic expression into a Tail. */
  Code compTailM(
      final CGEnv env,
      final Block abort,
      final TailCont kt) { // id [ fields ], e [ fields ],  e . lab
    debug.Internal.error("values of this form do not have monadic type");
    return null;
  }
}
