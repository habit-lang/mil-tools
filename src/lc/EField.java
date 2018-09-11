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

  int checkTypeStructInit(TVarsInScope tis, StructType st, StructField[] sfields) throws Failure {
    int p = Name.index(id, sfields);
    if (p < 0) {
      throw new Failure(
          pos, "Structure " + st + " does not include a field with label \"" + id + "\"");
    }
    e.checkType(tis, Type.init(sfields[p].getType()));
    return p;
  }

  static void lift(LiftEnv lenv, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fields[i].e = fields[i].e.lift(lenv);
    }
  }

  static Code compInit(
      final CGEnv env,
      final Block abort,
      final StructType st,
      final EField[] fields,
      final int lo,
      final int hi,
      final TailCont kt) {
    if (lo == hi) {
      // TODO: The generated code here is not correct:  We are trying to build an initializer for
      // some
      // structure type S, but the expression e :: Init T is an initializer for a single field f ::
      // T of S.
      // What we need here is an external or primitive of type  Init T -> #f -> Init S  that is
      // valid for
      // any combination of T, f, S such that f ::T is a field of S.  (Assuming Init a = [Ref a] ->>
      // [Unit],
      // the implementation of this function is straightforward:  \init. \#f. \r -> init (r + O),
      // where
      // O is the offset for f in S.  But we will need to add abstract syntax for #f types to be
      // able to
      // use this ...
      return fields[lo].e.compTail(
          env,
          abort,
          new TailCont() {
            Code with(final Tail init) {
              Temp i = new Temp();
              return new Bind(i, init, kt.with(st.initStructFieldPrim(lo).withArgs(i)));
            }
          });
    } else {
      final int mid = (lo + hi) / 2;
      return compInit(
          env,
          abort,
          st,
          fields,
          lo,
          mid,
          new TailCont() {
            Code with(final Tail t1) {
              final Temp i1 = new Temp();
              return new Bind(
                  i1,
                  t1,
                  compInit(
                      env,
                      abort,
                      st,
                      fields,
                      mid + 1,
                      hi,
                      new TailCont() {
                        Code with(final Tail t2) {
                          final Temp i2 = new Temp();
                          return new Bind(i2, t2, kt.with(Prim.initSeq.withArgs(i1, i2)));
                        }
                      }));
            }
          });
    }
  }

  Code compAtom(final CGEnv env, final AtomCont ka) {
    return e.compAtom(env, ka);
  }

  Code compUpdate(CGEnv env, Tail t, final BitdataField field, final TailCont kt) {
    final Temp a = new Temp(); // a holds value to be updated
    return new Bind(
        a,
        t,
        e.compAtom(
            env,
            new AtomCont() {
              Code with(final Atom b) { // b holds value to insert
                return kt.with(field.getUpdatePrim().withArgs(a, b));
              }
            }));
  }
}
