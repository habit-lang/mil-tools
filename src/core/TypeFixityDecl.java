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

public class TypeFixityDecl extends CoreDefn {

  private Fixity fixity;

  private String[] ids;

  /** Default constructor. */
  public TypeFixityDecl(Position pos, Fixity fixity, String[] ids) {
    super(pos);
    this.fixity = fixity;
    this.ids = ids;
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
    // TODO: Need to handle these declarations here!
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
    /* Nothing to do here! */
  }
}
