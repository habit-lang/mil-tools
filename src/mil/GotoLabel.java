/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
import core.*;

class GotoLabel extends Label {

  private Label lab;

  /** Default constructor. */
  GotoLabel(Label lab) {
    this.lab = lab;
  }

  /**
   * Return a string that can be used as a label for the generated code corresponding to this node.
   */
  String label() {
    return "g" + num;
  }

  /** Return a string with the options (e.g., fillcolor) for displaying this CFG node. */
  String dotAttrs() {
    return "style=filled, fillcolor=thistle1";
  }

  /** Find the CFG successors for this Label. */
  void findSuccs(CFG cfg) {
    succs = new Label[] {lab};
  }

  /** Generate code for this Label within an enclosing LLVM function definition. */
  llvm.Code toLLVMLabel(LLVMMap lm, VarMap vm, TempSubst s) {
    return new llvm.Goto(lab.label());
  }
}
