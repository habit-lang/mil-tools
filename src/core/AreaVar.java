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

public class AreaVar extends Name {

  /** Default constructor. */
  public AreaVar(Position pos, String id) {
    super(pos, id);
  }

  void addArea(Handler handler, MILEnv milenv, Type type) {
    if (milenv.findTop(id) == null) {
      // TODO: The final code generator will need a strategy for allocating the areas that
      // are declared here as externals ...
      milenv.addTop(id, new TopExt(type, new External(pos, id, type, null, null)));
    } else {
      handler.report(
          new Failure(
              pos, "area definition conflicts with previous definition for \"" + id + "\""));
    }
  }

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    /* default is to do nothing */
  }

  public void inferTypes(Handler handler, Type type) throws Failure {
    /* default is to do nothing */
  }

  public void lift(LiftEnv lenv) {
    /* default is to do nothing */
  }
}
