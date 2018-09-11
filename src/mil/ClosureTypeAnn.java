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

class ClosureTypeAnn extends TypeAnn {

  private TypeExp[] stored;

  private TypeExp type;

  /** Default constructor. */
  ClosureTypeAnn(Position pos, String[] ids, TypeExp[] stored, TypeExp type) {
    super(pos, ids);
    this.stored = stored;
    this.type = type;
  }

  private AllocType allocType;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure { // ids :: {stored} type
    allocType = AllocType.validate(milenv.getTyconEnv(), stored, type);
  }

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  void inScopeOf(Handler handler, MILEnv milenv) throws Failure {
    for (int i = 0; i < ids.length; i++) {
      ClosureDefn k = milenv.mustFindClosureDefn(handler, pos, ids[i]);
      if (k != null) {
        if (k.getDeclared() != null) {
          handler.report(
              new Failure(pos, "Multiple type annotations for closure \"" + k.getId() + "\""));
        } else {
          k.setDeclared(allocType);
        }
      }
    }
  }
}
