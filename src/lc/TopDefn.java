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

public abstract class TopDefn {

  protected Position pos;

  /** Default constructor. */
  public TopDefn(Position pos) {
    this.pos = pos;
  }

  /**
   * Run scope analysis on a top level lc definition to ensure that all the items identified as
   * exports or entrypoints are in scope.
   */
  abstract void scopeTopDefn(Handler handler, MILEnv milenv, Env env) throws Failure;

  void liftTopDefn(LiftEnv lenv) {
    /* nothing to do here! */
  }

  abstract void addExports(MILProgram mil, MILEnv milenv);
}
