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
import mil.*;

public class LCClosureDefn extends ClosureDefn {

  private Type result;

  public LCClosureDefn(Position pos, Type result, Temp[] args, Tail tail) {
    super(pos, (Temp[]) null, args, tail);
    this.result = result;
  }

  public Temp[] addArgs() throws Failure {
    if (params == null) { // compute stored params on first visit
      params = Temps.toArray(Temps.remove(args, tail.addArgs(null)));
      AllocType at = new AllocType(Temp.types(params), result);
      declared = at.generalize();
    }
    return params;
  }
}
