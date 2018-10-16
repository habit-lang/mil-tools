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

  private TypeExp typeExp;

  private AtomExp init;

  private TypeExp alignExp;

  /** Default constructor. */
  AreaDefnExp(Position pos, String id, TypeExp typeExp, AtomExp init, TypeExp alignExp) {
    super(pos);
    this.id = id;
    this.typeExp = typeExp;
    this.init = init;
    this.alignExp = alignExp;
  }

  private MemArea a;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure {
    typeExp.scopeType(null, milenv.getTyconEnv(), 0);
    typeExp.checkKind(KAtom.AREA);
    Type areaType = typeExp.toType(null).skeleton();
    Type size = areaType.calcAreaSize(pos);
    long alignment = areaType.calcAreaAlignment(pos, milenv, alignExp);
    a = new MemArea(pos, id, alignment, areaType, size);
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
