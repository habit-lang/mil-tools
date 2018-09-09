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

class EStructInit extends EConstruct {

  /** Default constructor. */
  EStructInit(Position pos, String id, EField[] fields) {
    super(pos, id, fields);
  }

  void display(Screen s) { // id [ fields ]
    s.print(id);
    EField.display(s, "<-", fields);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) { // id [ fields ]
    indent(out, n, "EStructInit: " + id);
    EField.indent(out, n + 1, fields);
  }

  private StructType st;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) {
    Tycon tc = TyconEnv.findTycon(id, milenv.getTyconEnv());
    if (tc == null) {
      handler.report(new UnknownStructureFailure(pos, id));
    } else if ((st = tc.structType()) == null) {
      handler.report(new StructureRequiredFailure(pos, tc));
    }
    return EField.inScopeOf(handler, milenv, env, null, fields);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure {
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
    return Type.init(st.asType());
  }

  /** Compile an expression into a Tail. */
  Code compTail(
      final CGEnv env, final Block abort, final TailCont kt) { // id [ fields ] (struct initializer)
    if (fields.length == 0) {
      debug.Internal.error("code generation for structure initializers with no fields");
    }
    return EField.compInit(env, abort, st, fields, 0, fields.length - 1, kt);
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
