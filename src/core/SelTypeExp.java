/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
import mil.*;

public class SelTypeExp extends PosTypeExp {

  private TypeExp t;

  private String lab;

  /** Default constructor. */
  public SelTypeExp(Position pos, TypeExp t, String lab) {
    super(pos);
    this.t = t;
    this.lab = lab;
  }

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    t = t.tidyInfix(env);
    return this;
  }

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    t = t.scopeTypeRewrite(canAdd, params, env, arity);
    return this;
  }

  public Kind inferKind() throws KindMismatchFailure {
    t.checkKind(KAtom.STAR);
    return KAtom.STAR;
  }

  public Type toType(Prefix prefix) throws Failure {
    BitdataType bt = t.toType(prefix).bitdataType();
    if (bt == null) {
      throw new Failure(pos, "Expected bitdata type");
    } else {
      BitdataLayout[] layouts = bt.getLayouts();
      int i = Name.index(lab, layouts);
      if (i >= 0) {
        return layouts[i].asType();
      }
      throw new Failure(pos, "Label " + lab + " is not used in " + bt);
    }
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    return t.scopeTyconsType(handler, params, env, defns, depends);
  }
}
