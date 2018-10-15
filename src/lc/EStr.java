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

class EStr extends ELit {

  private String str;

  /** Default constructor. */
  EStr(Position pos, String str) {
    super(pos);
    this.str = str;
  }

  void display(Screen s) {
    s.print("\"");
    s.print(str); // TODO: display special characters using escapes
    s.print("\"");
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EStr: \"" + str + "\""); // TODO: use escapes for non printing chars
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // Ref String
    return type = StringArea.refString;
  }

  /** Compile an expression into an Atom. */
  Code compAtom(final CGEnv env, final AtomCont ka) {
    return ka.with(new TopArea(new StringArea(pos, str)));
  }
}
