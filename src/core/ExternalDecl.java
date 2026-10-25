/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

public class ExternalDecl extends CoreDefn {

  private ExtImpId[] extids;

  private TypeExp texp;

  /** Default constructor. */
  public ExternalDecl(Position pos, ExtImpId[] extids, TypeExp texp) {
    super(pos);
    this.extids = extids;
    this.texp = texp;
  }

  /** Add environment entries for the type constructors introduced in this definition. */
  public void introduceTycons(Handler handler, TyconEnv env) {
    /* No tycons introduced here ... */
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeDefn(Handler handler, TyconDeps tdeps, TyconEnv env) throws Failure {
    /* Nothing to do in this case */
  }

  public void kindInfer(Handler handler) {
    /* no kind inference required here */
  }

  /** Default kind variables to STAR, unless they have already been bound. */
  public void defaultKinds() {
    /* do nothing */
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    /* Nothing to do here */
  }

  /**
   * Extend the specified MIL environment with entries for any functions or values introduced in
   * this definition.
   */
  public void addToMILEnv(Handler handler, CoreProgram prog, MILEnv milenv) {
    try {
      // Environment for implicitly quantified tyvars
      TyvarEnv params = new TyvarEnv();

      // Environment for tycons
      TyconEnv env = milenv.getTyconEnv();

      // Validate type component of declaration
      texp = texp.scopeType(params, env);

      // Record number of type variables found
      int len = params.getLen();

      texp.checkKind(KAtom.STAR);
      // Validate any generator arguments
      for (int i = 0; i < extids.length; i++) {
        extids[i].scopeExtImpId(params, env);
      }

      // Report new type variables found as not in scope
      NotInScopeTyvarFailure.beyond(handler, params, len);

      // Calculate the prefix for this declaration
      Prefix prefix = params.toPrefix();
      Scheme scheme = prefix.forall(texp.toType(prefix));
      for (int i = 0; i < extids.length; i++) {
        Top ext = new TopExt(extids[i].toExternal(prefix, prog, scheme));
        if (milenv.addTop(ext.getId(), ext) != null) {
          MILEnv.multipleDefns(pos, "external value", ext.getId());
        }
      }
    } catch (Failure f) {
      handler.report(f);
    }
  }
}
