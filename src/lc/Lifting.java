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

class Lifting {

  private TopLevel topLevel;

  private DefVar[] xvs;

  /** Default constructor. */
  Lifting(TopLevel topLevel, DefVar[] xvs) {
    this.topLevel = topLevel;
    this.xvs = xvs;
  }

  void dump() {
    System.out.print(topLevel.toString());
    for (int i = 0; i < xvs.length; i++) {
      System.out.print(" " + xvs[i]);
    }
  }

  /**
   * Construct abstract syntax for an application of the top variable in this Lifting to the list of
   * extra variables, replacing an occurrence of the associated identifier at pos with the specified
   * type.
   */
  Expr replacement(Position pos, Type type) {
    return replacement(pos, xvs.length, type);
  }

  /**
   * A recursive worker function for replacement, building an application of the top variable in
   * this lifting to the extra variables in xvs[0..i). The recursive structure is used here to allow
   * the calculation of types for each intermediate AST node (which naturally proceeds from the last
   * element of xvs to the first) at the same time as the calculation of the application (which
   * naturally proceeds from the first to the last element of xvs).
   */
  Expr replacement(Position pos, int i, Type type) {
    if (i > 0) {
      Type argtype = xvs[i - 1].instantiate();
      Type funtype = Type.fun(argtype, type);
      return new EAp(replacement(pos, i - 1, funtype), new EId(pos, xvs[i - 1], argtype), type);
    } else {
      return new EId(pos, new DecVar(new TopDef(topLevel, 0)), type);
    }
  }

  /** Add the free variables from this lifting to the given list of extra variables. */
  DefVars addLiftedArgs(DefVars xvs) {
    return DefVars.add(this.xvs, xvs);
  }

  TopLevel getTopLevel() {
    return topLevel;
  }
}
