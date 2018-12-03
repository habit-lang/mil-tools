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

class InitCFG extends CFG {

  private InitVarMap ivm;

  private Block b;

  llvm.Code edoc;

  /** Default constructor. */
  InitCFG(InitVarMap ivm, Block b, llvm.Code edoc) {
    this.ivm = ivm;
    this.b = b;
    this.edoc = edoc;

    includedBlocks = b.identifyBlocks(new Blocks(b, null));
    succs = new Label[] {this.edge(this, b, Temp.noTemps)};
    findSuccs();
  }

  VarMap getVarMap() {
    return ivm;
  }

  /** Return a string that can be used as the name of this node in debugging output. */
  String nodeName() {
    return llvm.FuncDefn.mainFunctionName;
  }

  /** Return a string with the options (e.g., fillcolor) for displaying this CFG node. */
  String dotAttrs() {
    return b.dotAttrs();
  }

  /** Calculate an array of formal parameters for the associated LLVM function definition. */
  llvm.Local[] formals(LLVMMap lm, VarMap vm) {
    return new llvm.Local[0];
  }

  /**
   * Helper function for constructing a function definition with the given formal parameters and
   * code by dispatching on the associated MIL Defn to collect additional details.
   */
  llvm.FuncDefn toLLVMFuncDefn(LLVMMap lm, llvm.Local[] formals, String[] ss, llvm.Code[] cs) {
    cs[0] = llvm.Code.reverseOnto(edoc, new llvm.Goto(succs[0].label()));
    return new llvm.FuncDefn(
        llvm.Mods.NONE, b.retType(lm), llvm.FuncDefn.mainFunctionName, formals, ss, cs);
  }
}
