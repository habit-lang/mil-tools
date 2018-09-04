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

/**
 * A base class for definitions that can be used at the top-level and locally (i.e., within a let)
 * in LC programs.
 */
public abstract class LCDefn {

  protected Position pos;

  /** Default constructor. */
  public LCDefn(Position pos) {
    this.pos = pos;
  }

  /** Add bindings corresponding to this definition to the given list of bindings. */
  public Bindings addBindings(Handler handler, Bindings bs) {
    return bs;
  }

  /**
   * Annotate bindings in the given list using information (such as a type signature or fixity) that
   * is provided by this definition.
   */
  public void annotateBindings(Handler handler, TyconEnv tenv, Bindings bs) {
    /* do nothing */
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  abstract void indent(IndentOutput out, int n);
}
