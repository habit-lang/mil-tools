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
import compiler.Failure;
import core.*;
import java.io.PrintWriter;

/** A base class for primitive unary integer operators. */
public abstract class PrimUnOp extends Prim {

  /** Default constructor. */
  public PrimUnOp(String id, int purity, BlockType blockType) {
    super(id, purity, blockType);
  }

  abstract long op(long n);

  void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
    stack[fp] = new WordValue(op(stack[fp].getInt()));
  }

  Code fold(long n) {
    MILProgram.report("constant folding for " + getId());
    return PrimCall.done(op(n));
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is not expected to produce any results, but execution is expected to continue with the given
   * code.
   */
  llvm.Code toLLVMPrimVoid(
      LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
    debug.Internal.error(id + " is not a void primitive");
    return c;
  }
}
