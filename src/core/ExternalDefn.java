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
import mil.*;

public class ExternalDefn extends CoreDefn {

  private ExternalId[] extids;

  private TypeExp texp;

  /** Default constructor. */
  public ExternalDefn(Position pos, ExternalId[] extids, TypeExp texp) {
    super(pos);
    this.extids = extids;
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
    /* Nothing to do in this case */
  }

  public void kindInfer(Handler handler) {
    /* no kind inference required here */
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    /* Nothing to do here */
  }

  public void addToMILEnv(Handler handler, MILEnv milenv) {
    try {
      Scheme scheme = texp.toScheme(milenv.getTyconEnv());
      for (int i = 0; i < extids.length; i++) {
        Top ext = new TopExt(extids[i].toExternal(milenv, scheme));
        if (milenv.addTop(ext.getId(), ext) != null) {
          MILEnv.multipleDefns(pos, "external value", ext.getId());
        }
      }
    } catch (Failure f) {
      handler.report(f);
    }
  }
}
