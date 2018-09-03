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

class DataConDefn extends Name {

  private TypeExp[] args;

  /** Default constructor. */
  DataConDefn(Position pos, String id, TypeExp[] args) {
    super(pos, id);
    this.args = args;
  }

  /**
   * Perform scope analysis on portions of a type constructor definition, returning a list with the
   * elements from defns that it references.
   */
  CoreDefns scopeTycons(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    for (int i = 0; i < args.length; i++) {
      depends = args[i].scopeTycons(handler, params, env, defns, depends);
    }
    return depends;
  }

  public void kindInfer(Handler handler) {
    for (int i = 0;
        i < args.length;
        i++) { // Every constructor argument should be a type of kind *.
      args[i].checkKind(handler, KAtom.STAR);
    }
  }

  Cfun calcCfun(Prefix prefix, DataType dt, Type result, int num) throws Failure {
    // Calculate an AllocType for this constructor:
    Type[] stored = new Type[args.length];
    for (int i = 0; i < args.length; ++i) {
      stored[i] = args[i].toType(prefix);
    }
    AllocType allocType = prefix.forall(stored, result);
    debug.Log.println(
        id + " :: " + allocType + " --  num = " + num + ", arity = " + allocType.getArity());
    return new Cfun(pos, id, dt, num, allocType);
  }
}
