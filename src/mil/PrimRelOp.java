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

/** A base class for primitive relational operators on integers. */
public abstract class PrimRelOp extends Prim {

  /** Default constructor. */
  public PrimRelOp(String id, int arity, int outity, int purity, BlockType blockType) {
    super(id, arity, outity, purity, blockType);
  }

  abstract boolean op(int n, int m);

  void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
    stack[fp] = BoolValue.make(op(stack[fp].getInt(), stack[fp + 1].getInt()));
  }

  Code foldRel(int n, int m) {
    MILProgram.report("constant folding for " + getId());
    return new Done(new Return(FlagConst.fromBool(op(n, m))));
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is not expected to produce any results, but execution is expected to continue with the given
   * code.
   */
  llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
    debug.Internal.error(id + " is not a void primitive");
    return c;
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is expected to return a result (that should be captured in the specified lhs), and then
   * execution is expected to continue on to the specified code, c.
   */
  llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
    return new llvm.Op(
        lhs, this.op(llvm.Type.i32, args[0].toLLVM(tm, vm, s), args[1].toLLVM(tm, vm, s)), c);
  }

  /**
   * Generate an LLVM right hand side for this binary MIL primitive with the given values as input.
   */
  abstract llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r);
}
