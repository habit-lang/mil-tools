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

public class StructFieldExp extends Name {

  /** Default constructor. */
  public StructFieldExp(Position pos, String id) {
    super(pos, id);
  }

  StructField makeField(Type type, int offset, int width) {
    return new StructField(pos, id, type, offset, width);
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
