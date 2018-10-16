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
import mil.*;

public class AreaVar extends Name {

  private Expr init;

  /** Default constructor. */
  public AreaVar(Position pos, String id, Expr init) {
    super(pos, id);
    this.init = init;
  }

  /** Identifies the MIL Area corresponding to this area variable declaration. */
  private MemArea area;

  void addToEnv(
      Handler handler, MILEnv milenv, long alignment, Type areaType, Type size, Type refType)
      throws Failure {
    if (milenv.findTop(id) != null) {
      handler.report(
          new Failure(
              pos, "area definition conflicts with previous definition for \"" + id + "\""));
    } else {
      // Create new area with an initializer to be filled in later ...
      area = new MemArea(pos, id, alignment, areaType, size);
      area.setDeclared(handler, pos, refType);
      milenv.addTop(id, new TopArea(refType, area)); // ... and add it to the MIL environment
    }
  }

  /**
   * Run scope analysis on a top level lc definition to ensure that all the items identified as
   * exports or entrypoints are in scope, either as a binding in this program, or as a Top that is
   * visible in the current environment.
   */
  void scopeTopDefn(Handler handler, MILEnv milenv, Env env) throws Failure {
    // Ignore results of top level inScopeOf() call
    init.inScopeOf(handler, milenv, env);
  }

  void inferTypes(Handler handler, Type initType) throws Failure {
    init.checkType(null, initType);
    init.findAmbigTVars(null);
  }

  void liftTopDefn(LiftEnv lenv) {
    init = init.lift(lenv);
  }

  /**
   * Generate a new TopLevel definition for this AreaVar's initializer, and store it in the
   * associated Area.
   */
  void compileAreaVar(Type initType) {
    // Create a new top level definition for this variable's initializer:
    Position ipos = init.getPos();
    TopLevel tl =
        new TopLevel(
            ipos,
            new TopLhs(),
            init.compTail(null, MILProgram.abort, TailCont.done).forceTail(ipos));
    area.setInit(new TopDef(initType, tl, 0));
  }
}
