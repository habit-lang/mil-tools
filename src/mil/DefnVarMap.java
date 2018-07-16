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

/**
 * DefnVarMap is a variant of VarMap that is intended to be used when generating code for function
 * and closure definitions. References to a global variables (Top) within the body of a function
 * that do not have a known static value are replaced with new local variables and the mapping from
 * Top items to locals is recorded in a list. The list is then used to insert an appropriate set of
 * load instructions at the start of the corresponding LLVM function.
 */
class DefnVarMap extends VarMap {

  private GlobalLoadList globalLoads = null;

  /**
   * Find an LLVM value corresponding to the given Top. Use static values if possible, but generate
   * a new LLVM local if necessary and add it to the globalLoads list so that it will properly
   * initialized at the start of the LLVM function.
   */
  llvm.Value lookupGlobal(TypeMap tm, Top t) {
    llvm.Value sv = t.staticValue();
    if (sv != null) {
      return sv;
    }
    for (GlobalLoadList gs = globalLoads; gs != null; gs = gs.next) {
      if (t.sameTop(gs.t)) {
        return gs.v;
      }
    }
    llvm.Type gt = tm.toLLVM(t.getType());
    llvm.Global g = new llvm.Global(gt.ptr(), t.toString());
    llvm.Local v = reg(gt);
    globalLoads = new GlobalLoadList(t, v, g, globalLoads);
    return v;
  }

  llvm.Code loadGlobals(llvm.Code code) {
    for (; globalLoads != null; globalLoads = globalLoads.next) {
      code = new llvm.Op(globalLoads.v, new llvm.Load(globalLoads.g), code);
    }
    return code;
  }
}
