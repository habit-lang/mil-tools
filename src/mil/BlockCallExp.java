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

class BlockCallExp extends CallExp {

  /** Default constructor. */
  BlockCallExp(Position pos, String id, AtomExp[] as) {
    super(pos, id, as);
  }

  /**
   * Perform scope analysis on the callee in this CallExp, checking to ensure that it has been
   * defined elsewhere in the program. The resulting Call object can then be extended with the
   * appropriate list of arguments in the implementation of inScopeOf() for CallExp objects.
   */
  Call calleeInScopeOf(Handler handler, MILEnv milenv) {
    return new BlockCall(milenv.mustFindBlock(handler, pos, id));
  }

  /**
   * Perform scope analysis on a block call expression. This method is needed to handle the block
   * calls that appear in case and if constructs: it performs the same analysis as the more general
   * inScopeOf() method for Tails, but it works only on BlockCallExp inputs, and it is guaranteed to
   * return a BlockCall result.
   */
  BlockCall blockCallInScopeOf(Handler handler, MILEnv milenv, TempEnv tenv) {
    BlockCall bc = new BlockCall(milenv.mustFindBlock(handler, pos, id));
    bc.withArgs(AtomExp.inScopeOf(handler, milenv, tenv, as));
    return bc;
  }
}
