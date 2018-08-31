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
 * InitVarMap is a variant of VarMap that is intended to be used when generating code for the
 * initialization function that is run when the program is first executed.
 */
class InitVarMap extends VarMap {

  /**
   * Records the mapping from top level definitions to local variables holding those values in the
   * initialization function.
   */
  private static class GlobalInitList {

    Top top;

    llvm.Value v;

    GlobalInitList next;

    /** Default constructor. */
    private GlobalInitList(Top top, llvm.Value v, GlobalInitList next) {
      this.top = top;
      this.v = v;
      this.next = next;
    }
  }

  private GlobalInitList globalInits = null;

  /**
   * Find an LLVM value corresponding to the given Top. Use static values if possible, and draw
   * remaining mappings from a globalInits list that is built up by calls to mapGlobal as initial
   * values are calculated.
   */
  llvm.Value lookupGlobal(LLVMMap lm, Top top) {
    llvm.Value sv = top.staticValue();
    if (sv != null) {
      return sv;
    }
    for (GlobalInitList gs = globalInits; gs != null; gs = gs.next) {
      if (top.sameAtom(gs.top)) {
        return gs.v;
      }
    }
    debug.Internal.error("Failed to find initial value for \"" + top + "\"");
    return null;
  }

  /**
   * Find an LLVM value corresponding to the given External in the GlobalInitList, or return null,
   * indicating that it will be necessary to use a load to read the external.
   */
  llvm.Value lookupExternal(External external) {
    for (GlobalInitList gs = globalInits; gs != null; gs = gs.next) {
      if (gs.top.sameTopExt(external)) {
        return gs.v;
      }
    }
    return null;
  }

  /**
   * Add an item to the GlobalInitList, indicating that the specified Top has been initialized to
   * the specified value.
   */
  void mapGlobal(Top top, llvm.Value v) {
    globalInits = new GlobalInitList(top, v, globalInits);
  }
}
