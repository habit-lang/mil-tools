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
import debug.Screen;
import mil.*;

class EqField extends EField {

  /** Default constructor. */
  EqField(Position pos, String id, Expr e) {
    super(pos, id, e);
  }

  void display(Screen s) {
    s.print(id);
    s.print("=");
    e.display(s);
  }

  int checkTypeStructInit(TVarsInScope tis, StructType st, StructField[] sfields) throws Failure {
    throw new Failure(pos, "Error in structure initializer syntax for field \"" + id + "\"");
  }
}
