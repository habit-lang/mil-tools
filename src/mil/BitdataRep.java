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
import compiler.Position;
import core.*;

class BitdataRep extends BitdataType {

  /** Default constructor. */
  BitdataRep(Position pos, String id) {
    super(pos, id);
  }

  BitdataRep isBitdataRep() {
    return this;
  }

  private Block[] consBlocks = null;

  /**
   * Return the block of code that can be used to construct a value using the numth constructor in
   * this BitdataRep.
   */
  Block bitdataConsBlock(int num) {
    if (consBlocks == null) { // First time for this BitdataRep?  Make a new array ...
      consBlocks = new Block[cfuns.length];
    }
    if (consBlocks[num] == null) { // First time for this value of num?  Make a new block ...
      return consBlocks[num] = layouts[num].makeConsBlock(cfuns[num]);
    }
    return consBlocks[num]; // Return the previously constructed block
  }

  private Block[][] selBlocks = null;

  /**
   * Return the block of code that can be used to construct a value using the numth constructor in
   * this BitdataRep.
   */
  Block bitdataSelBlock(int num, int n) {
    if (selBlocks == null) { // First time for this BitdataRep?  Make a new array ...
      selBlocks = new Block[cfuns.length][];
    }
    if (selBlocks[num] == null) { // First time for this value of num?  Make a new (sub)array ...
      selBlocks[num] = new Block[layouts[num].getArity()];
    }
    if (selBlocks[num][n] == null) { // First time for this n?  Make a new block ...
      return selBlocks[num][n] = layouts[num].makeSelBlock(cfuns[num], n);
    }
    return selBlocks[num][n]; // Return the previously constructed block
  }

  /**
   * Return the ith main constructor function for the associated bitdata type. (In general, this is
   * a constructor function with a type of the form T.C -> T.)
   */
  Cfun bitdataCfun(int i) {
    return cfuns[i];
  }
}
