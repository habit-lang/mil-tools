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
import compiler.BuiltinPosition;
import core.*;

public class MILArrow extends PrimTycon {

  private MILArrow() {
    super(
        BuiltinPosition.position,
        "->>",
        new KFun(KAtom.TUPLE, new KFun(KAtom.TUPLE, KAtom.STAR)),
        2);
  }

  public static final Tycon milArrow = new MILArrow();

  /** Test to determine if this type is the MILArrow, ->>, without any arguments. */
  boolean isMILArrow() {
    return true;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    if (args != 2) {
      debug.Internal.error("MILArrow toLLVM arity mismatch");
    }
    return lm.closurePtrTypeCalc(c);
  }
}
