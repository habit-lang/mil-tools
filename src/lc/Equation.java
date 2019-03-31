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

public class Equation extends LCDefn {

  private String id;

  private Expr e;

  /** Default constructor. */
  public Equation(Position pos, String id, Expr e) {
    super(pos);
    this.id = id;
    this.e = e;
  }

  /** Add bindings corresponding to this definition to the given list of bindings. */
  public Bindings addBindings(Handler handler, Bindings bs) {
    // TODO: combine multiple equations into a single binding?
    // TODO: add new binding to end of list?
    return new Bindings(new Binding(pos, id, e), bs);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, id + " = ");
    e.indent(out, n + 1);
  }
}
