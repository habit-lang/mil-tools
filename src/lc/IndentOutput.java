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
 * Represents an output phase for producing textual output of abstract syntax trees using
 * indentation.
 */
class IndentOutput {

  private java.io.PrintStream out;

  /** Default constructor. */
  IndentOutput(java.io.PrintStream out) {
    this.out = out;
  }

  /** Output an indented description of the abstract syntax tree for the given program. */
  void indent(LCProgram prog) {
    prog.indent(this, 0);
  }

  /**
   * Print a given String message indented some number of spaces (currently two times the given
   * nesting level, n).
   */
  void indent(int n, String msg) {
    for (int i = 0; i < n; i++) {
      out.print("  ");
    }
    out.println(msg);
  }

  void indent(int n, BindingSCCs sccs, Bindings bindings, LCDefns defns) {
    if (sccs != null) {
      for (BindingSCCs bs = sccs; bs != null; bs = bs.next) {
        bs.head.indent(this, n);
      }
    } else if (bindings != null) {
      Bindings.indent(this, n, bindings);
    } else {
      LCDefns.indent(this, n, defns);
    }
  }
}
