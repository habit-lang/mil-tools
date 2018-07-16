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
 * A primitive with a low-level block implementation that will be used to replace the primitive
 * during representation transformation.
 */
public class BlockPrim extends Prim {

  private Block impl;

  /** Default constructor. */
  public BlockPrim(String id, int arity, int outity, int purity, BlockType blockType, Block impl) {
    super(id, arity, outity, purity, blockType);
    this.impl = impl;
  }

  Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
    return new BlockCall(impl, targs);
  }
}
