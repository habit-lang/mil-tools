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

/**
 * An abstract base class for derived blocks. TODO: use a list of DerivedBlocks for the derived
 * field of Block, instead of a list of Blocks?
 */
abstract class DerivedBlock extends Block {

  /** Default constructor. */
  DerivedBlock(Position pos, Temp[] params, Code code) {
    super(pos, params, code);
  }

  /**
   * Count the number of unused arguments for this definition. A zero count indicates that all
   * arguments are used.
   */
  int countUnusedArgs(Temp[] dst) {
    return 0;
  }

  /**
   * Find the list of variables that are used in a call to this definition, taking account of the
   * usedArgs setting so that we only include variables appearing in argument positions that are
   * known to be used.
   */
  Temps usedVars(Atom[] args, Temps vs) {
    return useAllArgs(args, vs);
  }

  /**
   * Use information about which and how many argument positions are used to trim down an array of
   * destinations (specifically, the formal parameters of a Block or a ClosureDefn).
   */
  Temp[] removeUnusedTemps(Temp[] dsts) {
    return dsts;
  }

  /** Remove unused arguments from block calls and closure definitions. */
  void removeUnusedArgs() {
    /* Nothing to do here */
  }
}
