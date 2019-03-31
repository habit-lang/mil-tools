/*
    Copyright 2018-19 Mark P Jones, Portland State University

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

class InitStructFieldExp extends core.StructFieldExp {

  private Expr init;

  /** Default constructor. */
  InitStructFieldExp(Position pos, String id, Expr init) {
    super(pos, id);
    this.init = init;
  }

  private TopBinding tb;

  public StructField makeField(Type type, int offset, int width) {
    StructField sf = new StructField(pos, id, type, offset, width);
    TopLevel topLevel = new TopLevel(pos, "init_" + id, null);
    topLevel.setDeclared(0, Type.init(type));
    tb = new TopBinding(topLevel, init, null);
    sf.setDefaultInit(topLevel);
    return sf;
  }

  public TopBindings coreBindings(TopBindings tbs) {
    return new TopBindings(tb, tbs);
  }
}
