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
package mil;

import compiler.*;
import compiler.Failure;
import compiler.Handler;
import compiler.Position;
import core.*;

class AreaDefnExp extends DefnExp {

  private String id;

  private long alignment;

  private TypeExp areaType;

  private AtomExp init;

  /** Default constructor. */
  AreaDefnExp(Position pos, String id, long alignment, TypeExp areaType, AtomExp init) {
    super(pos);
    this.id = id;
    this.alignment = alignment;
    this.areaType = areaType;
    this.init = init;
  }

  private Area a;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure {
    areaType.scopeType(null, milenv.getTyconEnv(), 0);
    areaType.checkKind(KAtom.AREA);
    Type at = areaType.toType(null);
    if (at == null) {
      throw new Failure(pos, "Definition for area " + id + " specifies a polymorphic type");
    }
    a = new Area(pos, id, alignment, at);
    if (milenv.addTop(id, new TopArea(a)) != null) {
      MILEnv.multipleDefns(pos, "top level/area symbol", id);
    }
  }

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  void inScopeOf(Handler handler, MILEnv milenv) throws Failure {
    a.inScopeOf(handler, milenv, init);
  }
}
