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

abstract class EField extends Name {

  protected Expr e;

  /** Default constructor. */
  EField(Position pos, String id, Expr e) {
    super(pos, id);
    this.e = e;
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

  int checkTypeStructInit(TVarsInScope tis, StructType st, StructField[] sfields) throws Failure {
    int p = Name.index(id, sfields);
    if (p < 0) {
      throw new Failure(
          pos, "Structure " + st + " does not include a field with label \"" + id + "\"");
    }
    e.checkType(tis, Type.init(sfields[p].getType()));
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

  static void lift(LiftEnv lenv, EField[] fields) {
    for (int i = 0; i < fields.length; i++) {
      fields[i].e = fields[i].e.lift(lenv);
    }
  }

  static Code compInit(
      final CGEnv env,
      final Block abort,
      final StructType st,
      final Type tyinitS,
      final EField[] inits,
      final int lo,
      final int hi,
      final Type kty,
      final TailCont kt) {
    if (lo == hi) {
      if (inits[lo] == null) {
        Atom i = new TopDef(st.getFields()[lo].getDefaultInit(), 0);
        return kt.with(st.initStructFieldPrim(lo).withArgs(i));
      }
      return inits[lo].e.compTail(
          env,
          abort,
          kty,
          new TailCont() {
            Code with(final Tail init) {
              Temp i = new Temp(Type.init(st.getFields()[lo].getType()));
              return new Bind(i, init, kt.with(st.initStructFieldPrim(lo).withArgs(i)));
            }
          });
    } else {
      final int mid = (lo + hi) / 2;
      return compInit(
          env,
          abort,
          st,
          tyinitS,
          inits,
          lo,
          mid,
          kty,
          new TailCont() {
            Code with(final Tail t1) {
              final Temp i1 = new Temp(tyinitS);
              return new Bind(
                  i1,
                  t1,
                  compInit(
                      env,
                      abort,
                      st,
                      tyinitS,
                      inits,
                      mid + 1,
                      hi,
                      kty,
                      new TailCont() {
                        Code with(final Tail t2) {
                          final Temp i2 = new Temp(tyinitS);
                          return new Bind(i2, t2, kt.with(Prim.initSeq.withArgs(i1, i2)));
                        }
                      }));
            }
          });
    }
  }

  Code compAtom(CGEnv env, Type kty, AtomCont ka) {
    return e.compAtom(env, kty, ka);
  }
}
