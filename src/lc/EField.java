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

  static void display(Screen s, String sep, EField[] fields) {
    s.print("[");
    if (fields != null && fields.length > 0) {
      fields[0].display(s, sep);
      for (int i = 1; i < fields.length; i++) {
        s.print(", ");
        fields[i].display(s, sep);
      }
    }
    s.print("]");
  }

  void display(Screen s, String sep) {
    s.print(id);
    s.print(sep);
    e.display(s);
  }

  static void indent(IndentOutput out, int n, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      out.indent(n, "EField: " + fields[i].id);
      fields[i].e.indent(out, n + 1);
    }
  }

  static DefVars inScopeOf(Handler handler, MILEnv milenv, Env env, DefVars fvs, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fvs = DefVars.add(fvs, fields[i].e.inScopeOf(handler, milenv, env));
    }
    return fvs;
  }

  static void findAmbigTVars(TVars gens, EField[] fields) throws Failure {
    for (int i = 0; i < fields.length; i++) {
      fields[i].e.findAmbigTVars(gens);
    }
  }

  int checkTypeConstruct(TVarsInScope tis, Cfun cf, BitdataField[] lfields) throws Failure {
    int p = Name.index(id, lfields);
    if (p < 0) {
      throw new Failure(
          pos, "Constructor " + cf + " does not include a field with label \"" + id + "\"");
    }
    // !System.out.println("Found field \"" + id + "\" at position " + p);
    e.checkType(tis, lfields[p].getType());
    return p;
  }

  BitdataField checkTypeUpdate(TVarsInScope tis, Type et, BitdataField[] lfields) throws Failure {
    int p = Name.index(id, lfields);
    if (p < 0) {
      throw new Failure(pos, "There is no \"" + id + "\" field for type " + et);
    }
    e.checkType(tis, lfields[p].getType());
    return lfields[p];
  }

  int checkTypeStructInit(TVarsInScope tis, StructName sn, StructField[] sfields) throws Failure {
    int p = Name.index(id, sfields);
    if (p < 0) {
      throw new Failure(
          pos, "Structure " + sn + " does not include a field with label \"" + id + "\"");
    }
    e.checkType(tis, Type.init(sfields[p].getType()));
    return p;
  }

  static void lift(LiftEnv lenv, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fields[i].e = fields[i].e.lift(lenv);
    }
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
