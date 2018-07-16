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

public class PrimTyconDefn extends TyconDefn {

  private int arity;

  private KindExp kexp;

  /** Default constructor. */
  public PrimTyconDefn(Position pos, String id, int arity, KindExp kexp) {
    super(pos, id);
    this.arity = arity;
    this.kexp = kexp;
  }

  private Tycon tycon;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return tycon;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    try {
      // TODO: why does this PrimTycon need an arity?
      // TODO: rewrite to clarify that the try..catch is only here for the toKind() call
      env.add(tycon = new PrimTycon(pos, id, kexp.toKind(), arity));
    } catch (Failure f) {
      handler.report(f);
    }
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
    /* Nothing to do here! */
  }
}
