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

/** A base class for definitions that are only valid at the top level in an LC program. */
public abstract class TopDefn {

  protected Position pos;

  /** Default constructor. */
  public TopDefn(Position pos) {
    this.pos = pos;
  }

  /**
   * Validate this top level definition and add corresponding entries to the environment, if
   * necessary.
   */
  void validateTopDefn(Handler handler, MILEnv milenv) throws Failure {
    /* Default behavior is to do nothing */
  }

  /**
   * Run scope analysis on a top level lc definition to ensure that all the items identified as
   * exports or entrypoints are in scope, either as a binding in this program, or as a Top that is
   * visible in the current environment.
   */
  abstract void scopeTopDefn(Handler handler, MILEnv milenv, Env env) throws Failure;

  /** Check types of expressions appearing in top-level definitions. */
  abstract void inferTypes(Handler handler) throws Failure;

  abstract void liftTopDefn(LiftEnv lenv);

  /** Generate code, if necessary, for top-level definitions. */
  void compileTopDefn() {
    /* Default is to do nothing */
  }

  abstract void addExports(MILProgram mil, MILEnv milenv);
}
