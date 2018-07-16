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

class BlockCFG extends CFG {

  private Block b;

  Temp[] params;

  /** Default constructor. */
  BlockCFG(Block b, Temp[] params) {
    this.b = b;
    this.params = params;
  }

  /** Return a string label that can be used to identify this node. */
  String label() {
    return b.label();
  }

  /** Return a string with the options (e.g., fillcolor) for displaying this CFG node. */
  String dotAttrs() {
    return b.dotAttrs();
  }

  void initCFG() {
    includedBlocks = b.identifyBlocks();
    succs = new Label[] {this.edge(this, b, params)};
    findSuccs();
  }

  llvm.FuncDefn toLLVMFuncDefn(TypeMap tm, DefnVarMap dvm) {
    llvm.Local[] formals = new llvm.Local[params.length];
    for (int i = 0; i < params.length; i++) {
      formals[i] = dvm.lookup(tm, params[i]);
    }
    return toLLVM(b.retType(tm), formals, dvm.loadGlobals(new llvm.Goto(succs[0].label())));
  }
}
