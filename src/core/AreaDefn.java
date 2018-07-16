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
package core;

import compiler.*;
import lc.Env;
import lc.LiftEnv;
import mil.*;

public class AreaDefn extends CoreDefn {

  private AreaVar[] areas;

  private TypeExp texp;

  /** Default constructor. */
  public AreaDefn(Position pos, AreaVar[] areas, TypeExp texp) {
    super(pos);
    this.areas = areas;
    this.texp = texp;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    /* No tycons introduced here ... */
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) {
    try {
      calls(texp.scopeTycons(null, env, defns, null));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  public void kindInfer(Handler handler) {
    /* no kind inference required here */
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    /* Nothing to do here */
  }

  private Type alignment;

  private Type areaType;

  private Type refType;

  public void addToMILEnv(Handler handler, MILEnv milenv) {
    try {
      texp.scopeType(null, milenv.getTyconEnv(), 0);
      texp.checkKind(KAtom.STAR);
      refType = texp.toType(null);
      alignment = new TVar(Tyvar.nat);
      areaType = new TVar(Tyvar.area);
      Type at = new TAp(new TAp(DataName.aref.asType(), alignment), areaType); // ARef
      if (!at.match(null, refType, null)) {
        throw new Failure(texp.position(), "area definition requires a reference type");
      }
      alignment = alignment.simplifyNatType(null);
      debug.Log.println(
          "area type is "
              + areaType.skeleton()
              + ", alignment="
              + alignment
              + ", reference type "
              + refType);
      if (alignment.getNat() == null) {
        // TODO: It's not clear how we can produce this error condition in practice; should it
        // be an internal error instead?
        throw new Failure(texp.position(), "alignment not determined");
      }
      for (int i = 0; i < areas.length; i++) {
        areas[i].addArea(handler, milenv, refType);
      }
    } catch (Failure f) {
      handler.report(f);
    }
  }

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    for (int i = 0; i < areas.length; i++) {
      areas[i].inScopeOf(handler, milenv, env);
    }
  }

  public void inferTypes(Handler handler) throws Failure {
    for (int i = 0; i < areas.length; i++) {
      areas[i].inferTypes(handler, areaType);
    }
  }

  public void lift(LiftEnv lenv) {
    for (int i = 0; i < areas.length; i++) {
      areas[i].lift(lenv);
    }
  }
}
