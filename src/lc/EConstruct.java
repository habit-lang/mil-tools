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

class EConstruct extends PosExpr {

  private String id;

  private EField[] fields;

  /** Default constructor. */
  EConstruct(Position pos, String id, EField[] fields) {
    super(pos);
    this.id = id;
    this.fields = fields;
  }

  void display(Screen s) { // id [ fields ]
    s.print(id);
    EField.display(s, fields);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) { // id [ fields ]
    indent(out, n, "EConstruct:" + id);
    EField.indent(out, n + 1, fields);
  }

  private Cfun cf;

  private BitdataType bt;

  private BitdataLayout layout;

  private int[] invmap;

  private StructType st;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // id [ fields ]
    if ((cf = milenv.findCfun(id)) != null) { // is this a bitdata constructor?
      if ((bt = cf.bitdataType()) == null) {
        handler.report(new BitdataRequiredFailure(pos, cf));
      } else {
        layout = bt.getLayouts()[cf.getNum()];
      }
    } else {
      Tycon tc = TyconEnv.findTycon(id, milenv.getTyconEnv());
      if (tc == null || (st = tc.structType()) == null) {
        handler.report(new UnknownConstructorFailure(pos, id));
      }
    }
    return EField.inScopeOf(handler, milenv, env, null, fields);
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
  void findAmbigTVars(TVars gens) throws Failure { // id [ fields ]
    EField.findAmbigTVars(gens, fields);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // id [ fields ]
    return (cf != null) ? inferTypeBitdata(tis) : inferTypeStructInit(tis);
  }

  Type inferTypeBitdata(TVarsInScope tis) throws Failure { // id [ fields ]
    BitdataField[] lfields = layout.getFields();
    // A mapping from field numbers in layout to field numbers in this expression:
    int[] map = new int[lfields.length];

    // Check that individual fields have the expected type:
    for (int i = 0; i < fields.length; i++) {
      int p = fields[i].checkTypeConstruct(tis, cf, lfields);
      if (map[p] != 0) {
        throw new Failure(
            fields[i].getPos(),
            "Constructor includes multiple definitions for field \"" + lfields[p] + "\"");
      }
      map[p] = 1 + i; // Add one to avoid confusion with 0th element ...
    }

    // Check that all fields are defined:
    invmap = new int[fields.length]; // inverse mapping from field # in AST to layout #
    for (int p = 0; p < lfields.length; p++) {
      if (map[p] == 0) {
        // TODO: report all missing fields in a single error message.
        throw new Failure(
            pos,
            "Constructor "
                + cf
                + " requires value for field "
                + lfields[p]
                + " :: "
                + lfields[p].getType());
      } else {
        invmap[map[p] - 1] = p;
      }
    }
    return type = bt.asType();
  }

  Type inferTypeStructInit(TVarsInScope tis) throws Failure {
    StructField[] sfields = st.getFields();
    // A mapping from field numbers in the structure to field numbers in this expression:
    int[] map = new int[sfields.length];

    // Check that individual fields have the expected type:
    for (int i = 0; i < fields.length; i++) {
      int p = fields[i].checkTypeStructInit(tis, st, sfields);
      if (map[p] != 0) {
        throw new Failure(
            fields[i].getPos(), "There are multiple initializers for field \"" + sfields[p] + "\"");
      }
      map[p] = 1 + i; // Add one to avoid confusion with 0th element ...
    }

    // Check that all fields are defined:
    for (int p = 0; p < sfields.length; p++) {
      if (map[p] == 0) {
        throw new Failure(
            pos,
            "Structure "
                + st
                + " requires initializer for field "
                + sfields[p]
                + " :: "
                + sfields[p].getType());
      }
    }
    return type = Type.init(st.asType());
  }

  Expr lift(LiftEnv lenv) { // id [ fields ]
    EField.lift(lenv, fields);
    return this;
  }

  /** Compile an expression into a Tail. */
  Code compTail(final CGEnv env, final Block abort, final TailCont kt) {
    if (cf == null) { // structure initializer
      // TODO: does this work correctly for structures with no fields?
      return EField.compInit(env, abort, st, fields, 0, fields.length - 1, kt);
    } else { // bitdata construction
      // sketch of generated code:
      //     comp[fexp1] $ \a1 ->
      //     ...
      //     comp[fexpN] $ \aN ->
      //     l <- LCF(a1,...,aN)    -- build layout (modulo invmap permutation)
      //     kt(C(l))               -- embed in bitdata type
      final Atom[] as = new Atom[fields.length];
      Temp l = new Temp();
      Code code = new Bind(l, layout.getCfuns()[0].withArgs(as), kt.with(cf.withArgs(l)));
      for (int i = fields.length; --i >= 0; ) {
        final Code c = code;
        final int j = invmap[i];
        code =
            fields[i].compAtom(
                env,
                new AtomCont() {
                  Code with(final Atom a) {
                    as[j] = a; // save variable
                    return c;
                  }
                });
      }
      return code;
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
