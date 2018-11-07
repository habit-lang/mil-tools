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

public class SynonymDefn extends TyconDefn {

  private TypeExp rhs;

  /** Default constructor. */
  public SynonymDefn(Position pos, String id, TypeExp rhs) {
    super(pos, id);
    this.rhs = rhs;
  }

  private Synonym sn;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return sn;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    // TODO: sad to have to initialize the expansion to null here ...
    env.add(sn = new Synonym(pos, id, new KVar(), null));
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) {
    try {
      calls(rhs.scopeTycons(null, env, defns, null));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  public void kindInfer(Handler handler) {
    try {
      rhs.checkKind(sn.getKind());
      sn.setExpansion(rhs.toType(null));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  public void fixKinds() {
    sn.fixKinds();
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    // TODO: does this really belong in calcCfuns?
    try {
      int level = sn.findLevel();
      debug.Log.println("synonym " + id + " is at level " + level);
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Extend the specified MIL environment with entries for any functions/values introduced in this
   * definition.
   */
  public void addToMILEnv(Handler handler, MILEnv milenv) {
    /* Nothing to do here! */
  }
}
