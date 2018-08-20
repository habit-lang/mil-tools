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

/** A base class for primitives that convert a flag to a word. */
public abstract class PrimFtoW extends Prim {

  /** Default constructor. */
  public PrimFtoW(String id, int purity, BlockType blockType) {
    super(id, purity, blockType);
  }

  abstract long op(boolean b);

  void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
    stack[fp] = new WordValue(op(stack[fp].getBool()));
  }

  Code fold(boolean n) {
    MILProgram.report("constant folding for " + getId());
    return PrimCall.done(op(n));
  }
}
