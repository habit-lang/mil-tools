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
import java.util.HashMap;

abstract class VarMap {

  private int count = 0;

  /** Generate a new LLVM local that has not been used previously with this VarMap. */
  llvm.Local reg(llvm.Type ty) {
    return new llvm.Local(ty, count++);
  }

  /**
   * Stores the mapping from MIL temporaries to llvm locals in a CFG. We assume that the MIL
   * temporaries used here have already been translated, if necessary, by the parameter elimination
   * substitution that identifies distinct Block parameters that use the same temporary.
   */
  private HashMap<Temp, llvm.Local> tempMap = new HashMap();

  /**
   * Find the LLVM Local corresponding to the given Temp in this CFG, creating a new entry in the
   * tempMap for the first occurrence of a given Temp value.
   */
  llvm.Local lookup(LLVMMap lm, Temp t) {
    llvm.Local lhs = tempMap.get(t);
    if (lhs == null) {
      lhs = reg(t.lookupType(lm));
      tempMap.put(t, lhs);
    }
    return lhs;
  }

  /** Return an LLVM value corresponding to the specified global. */
  abstract llvm.Value lookupGlobal(LLVMMap lm, Top t);
}
