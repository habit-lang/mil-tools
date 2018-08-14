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

  Temp[] fparams;

  /** Default constructor. */
  BlockCFG(Block b, Temp[] fparams) {
    this.b = b;
    this.fparams = fparams;
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
    succs = new Label[] {this.edge(this, b, fparams)};
    findSuccs();
  }

  /** Calculate an array of formal parameters for the associated LLVM function definition. */
  llvm.Local[] formals(LLVMMap lm, DefnVarMap dvm) {
    llvm.Local[] formals = new llvm.Local[fparams.length];
    for (int i = 0; i < fparams.length; i++) {
      formals[i] = dvm.lookup(lm, fparams[i]);
    }
    return formals;
  }

  /**
   * Helper function for constructing a function definition with the fiven formal parameters and
   * code by connecting the the associated MIL Defn for additional details.
   */
  llvm.FuncDefn toLLVMFuncDefn(
      LLVMMap lm, DefnVarMap dvm, TempSubst s, llvm.Local[] formals, String[] ss, llvm.Code[] cs) {
    return b.toLLVMFuncDefn(lm, dvm, s, formals, ss, cs, succs);
  }
}
