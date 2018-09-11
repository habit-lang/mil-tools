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
package mil;

import compiler.*;
import core.*;

abstract class Label extends Node {

  /** The list of Nodes that are predecessors to this Label. */
  protected PredNodes preds;

  public static final Label[] noLabels = new Label[0];

  /** Determine whether this Label contains the code for the specified Block. */
  boolean hasCodeFor(Block block) {
    return false;
  }

  /**
   * Determine whether we will need to add an extra GotoLabel for an edge from the specified src to
   * this Node.
   */
  boolean needsGoto(Node src) {
    // TODO: we can return false immediately if the block for this node has an empty parameter list
    // (because no phi function will be needed in that case)
    for (PredNodes ps = preds; ps != null; ps = ps.next) {
      if (ps.head == src) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update the predecessor list for this Label to indicate that it is called with the specified
   * arguments from the specified source node, src:
   */
  void calledFrom(Node src, Atom[] args) {
    this.preds = new PredNodes(src, args, this.preds);
  }

  /** Find the CFG successors for this Label. */
  abstract void findSuccs(CFG cfg);

  protected boolean visited = false;

  TempSubst paramElim(TempSubst s) {
    if (!visited) {
      visited = true;
      s = visit(s);
      for (int i = 0; i < succs.length; i++) {
        s = succs[i].paramElim(s);
      }
    }
    return s;
  }

  TempSubst visit(TempSubst s) {
    return s;
  }

  /** Generate code for this Label within an enclosing LLVM function definition. */
  abstract llvm.Code toLLVMLabel(LLVMMap lm, VarMap vm, TempSubst s);
}
