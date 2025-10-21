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

class BlockWithDuplicateArgs extends DerivedBlock {

  private int[] dups;

  /** Default constructor. */
  BlockWithDuplicateArgs(Position pos, Temp[] params, Code code, int[] dups) {
    super(pos, params, code);
    this.dups = dups;
  }

  /**
   * Check to see if this is a derived version of a block with duplicate arguments that matches the
   * given pattern.
   */
  boolean hasDuplicateArgs(int[] dups) {
    if (dups.length == this.dups.length) {
      for (int i = 0; i < dups.length; i++) {
        if (dups[i] != this.dups[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
