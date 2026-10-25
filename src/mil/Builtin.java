/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
package mil;

import compiler.*;
import core.*;

class Builtin extends MILEnvHash {

  /** Default constructor. */
  Builtin(TyconEnv tyconEnv) {
    super(tyconEnv);
  }

  void print() {
    System.out.println("Builtin: --------");
    printThis();
  }

  private Builtin() {
    super(TyconEnv.builtin);
    try {
      // Add definitions for built-in constructor functions:
      addCfunAndTop(Cfun.Unit);
      addCfunAndTop(Cfun.Null);
      addCfunAndTop(Cfun.Ref);
      addCfunAndTop(Cfun.Func); // TODO: maybe we shouldn't add the Top here ...
      addCfunAndTop(Cfun.Proc); // TODO: maybe we shouldn't add the Top here ...

      // Add definitions for built-in primitive functions:
      Prim.addBuiltins(this);

    } catch (Exception e) { // Should this be a more specific exception?
      debug.Internal.error("Failed to initialize primitives");
    }
  }

  public static final Builtin obj = new Builtin();

  /**
   * Add specified primitive to this MILEnv, triggering a failure if there is already a primitive
   * with the same name.
   */
  void addNewPrim(Prim p) throws Failure {
    if (addPrim(p) != null) {
      // TODO: it would be good to give better position information for this
      // primitive (and for the other definition too).
      multipleDefns(BuiltinPosition.pos, "primitive", p.getId());
    }
  }
}
