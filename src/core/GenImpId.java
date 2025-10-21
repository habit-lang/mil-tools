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

class GenImpId extends ExtImpId {

  private String ref;

  private TypeExp[] spec;

  /** Default constructor. */
  GenImpId(Position pos, String id, String ref, TypeExp[] spec) {
    super(pos, id);
    this.ref = ref;
    this.spec = spec;
  }

  /** Check that all of the type arguments used in this external id, if any, are well-scoped. */
  void scopeExtImpId(TyvarEnv params, TyconEnv env) throws Failure {
    for (int i = 0; i < spec.length; i++) {
      spec[i].scopeType(false, params, env, 0); // Scope analysis
      spec[i].inferKind(); // Check for a valid kind
    }
  }

  /** Calculate a MIL External object corresponding to this ExtImpId. */
  External toExternal(Prefix prefix, CoreProgram prog, Scheme declared) throws Failure {
    Type[] ts = new Type[spec.length];
    for (int i = 0; i < spec.length; i++) {
      ts[i] = spec[i].toType(prefix);
    }
    return new GenImp(ref, ts).validate(pos, id, declared); // TODO: forward ref to GenImp
  }
}
