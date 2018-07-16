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

class ExternalId extends Name {

  private String ref;

  private TypeExp[] spec;

  /** Default constructor. */
  ExternalId(Position pos, String id, String ref, TypeExp[] spec) {
    super(pos, id);
    this.ref = ref;
    this.spec = spec;
  }

  External toExternal(MILEnv milenv, Scheme declared) throws Failure {
    Type[] ts = Type.noTypes;
    if (ref != null && spec != null) {
      ts = new Type[spec.length];
      for (int i = 0; i < spec.length; i++) {
        spec[i].scopeType(null, milenv.getTyconEnv(), 0);
        spec[i].inferKind(); // TODO: is some form of fixKinds() required here?
        ts[i] = spec[i].toType(null);
      }
    }
    return new External(pos, id, declared, ref, ts);
  }
}
