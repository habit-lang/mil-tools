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

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return null;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    /* No tycons introduced here ... */
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) {
    /* Nothing to do in this case */
  }

  public void kindInfer(Handler handler) {
    /* no kind inference required here */
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    /* Nothing to do here */
  }

  /**
   * Extend the specified MIL environment with entries for any functions/values introduced in this
   * definition.
   */
  public void addToMILEnv(Handler handler, CoreProgram prog, MILEnv milenv) {
    try {
      TyvarEnv params = new TyvarEnv(); // Environment for implicitly quantified tyvars
      TyconEnv env = milenv.getTyconEnv(); // Environment for tycons
      texp.scopeType(true, params, env, 0); // Validate type component of declaration
      texp.checkKind(KAtom.STAR);
      for (int i = 0; i < extids.length; i++) { // Validate any generator arguments
        extids[i].scopeExtImpId(params, env);
      }
      Prefix prefix = params.toPrefix(); // Calculate the prefix for this declaration
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
