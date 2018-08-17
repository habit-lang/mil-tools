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

class CodeLabel extends Label {

  private Block b;

  /** Default constructor. */
  CodeLabel(Block b) {
    this.b = b;
  }

  /** Return a string label that can be used to identify this node. */
  String label() {
    return b.getId();
  }

  /** Return a string with the options (e.g., fillcolor) for displaying this CFG node. */
  String dotAttrs() {
    return b.dotAttrs();
  }

  /** Determine whether this Label contains the code for the specified Block. */
  boolean hasCodeFor(Block block) {
    return block == b;
  }

  /** Find the CFG successors for this Label. */
  void findSuccs(CFG cfg) {
    succs = b.findSuccs(cfg, this);
  }

  TempSubst visit(TempSubst s) {
    // Add renamings if this block only has a single predecessor:
    return (preds.next == null) ? b.mapParams(preds.args, s) : s;
  }

  /** Generate code for this Label within an enclosing LLVM function definition. */
  llvm.Code toLLVMLabel(LLVMMap lm, VarMap vm, TempSubst s) {
    llvm.Code code = b.toLLVMBlock(lm, vm, s, succs);
    if (preds.next != null) { // multiple predecessors: merge parameters using phi functions
      Temp[] params = Temp.nonUnits(b.getParams());
      if (params.length > 0) {
        int numpreds = 2; // count the number of predecessor nodes
        for (PredNodes pns = preds.next.next; pns != null; pns = pns.next) {
          numpreds++;
        }
        for (int i = params.length; --i >= 0; ) {
          String[] blocks = new String[numpreds];
          llvm.Value[] values = new llvm.Value[numpreds];
          int j = numpreds;
          for (PredNodes ps = preds; ps != null; ps = ps.next) {
            blocks[--j] = ps.head.label();
            values[j] = ps.args[i].toLLVMAtom(lm, vm, s);
          }
          code = new llvm.Op(vm.lookup(lm, params[i]), new llvm.Phi(blocks, values), code);
        }
      }
    }
    return code;
  }
}
