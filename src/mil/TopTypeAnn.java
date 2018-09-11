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

class TopTypeAnn extends TypeAnn {

  private TypeExp type;

  /** Default constructor. */
  TopTypeAnn(Position pos, String[] ids, TypeExp type) {
    super(pos, ids);
    this.type = type;
  }

  private Scheme scheme;

  /**
   * Worker function for addTo(handler, milenv) that throws an exception if an error is detected.
   */
  void addTo(MILEnv milenv) throws Failure { // ids :: type
    scheme = type.toScheme(milenv.getTyconEnv());
  }

  /**
   * Perform scope analysis on this definition to ensure that all referenced identifiers are
   * appropriately bound.
   */
  void inScopeOf(Handler handler, MILEnv milenv) throws Failure {
    for (int i = 0; i < ids.length; i++) {
      Top t = milenv.mustFindTop(handler, pos, ids[i]);
      if (t != null) {
        t.setDeclared(handler, pos, scheme);
      }
    }
  }
}
