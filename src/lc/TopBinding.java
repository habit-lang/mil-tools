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

public class TopBinding {

  private TopLevel topLevel;

  private Expr e;

  private Type type;

  /** Default constructor. */
  public TopBinding(TopLevel topLevel, Expr e, Type type) {
    this.topLevel = topLevel;
    this.e = e;
    this.type = type;
  }

  /** Print an indented description of a TopBinding. */
  void indent(IndentOutput out, int n) {
    out.indent(n, "TopBinding: " + topLevel + ", type = " + type.skeleton());
    e.indent(out, n + 1);
  }

  void bindExtras(DefVar[] xvs) {
    // Abstract over extra arguments.  If there are any, add a lambda expression to the right hand
    // side of the binding and adjust the type to match:
    if (xvs.length > 0) {
      // Calculate type of lambda expression with extra arguments:
      for (int i = xvs.length; i > 0; i--) {
        type = Type.fun(xvs[i - 1].instantiate(), type);
      }
      // Abstract over the extra variables:
      e = new ELam(e.getPos(), xvs, e, type);

      // Calculate the most general type of the lifted function:
      topLevel.setDeclared(0, type.generalize());
    }
    debug.Log.println("Type of lifted " + topLevel + " :: " + topLevel.getDeclared(0));
  }

  void liftBinding(LiftEnv lenv) {
    e = e.lift(lenv);
  }

  /** Compile a top-level binding, updating the associated TopLevel value. */
  void compile() { // id = e
    topLevel.setTail(e.compTopLevel(topLevel.getPos()));
  }
}
