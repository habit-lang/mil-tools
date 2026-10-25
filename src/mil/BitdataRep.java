/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

class BitdataRep extends BitdataType {

  /** Default constructor. */
  BitdataRep(Position pos, String id) {
    super(pos, id);
  }

  /** Test whether this Tycon is an BitdataRep. */
  public BitdataRep isBitdataRep() {
    return this;
  }

  /**
   * Return the ith main constructor function for the associated bitdata type. (In general, this is
   * a constructor function with a type of the form T.C -> T.)
   */
  Cfun bitdataCfun(int i) {
    return cfuns[i];
  }

  /**
   * Stores a cache of blocks for constructing values of the constructor functions for this Bitdata
   * Rep.
   */
  private Block[] consBlocks = null;

  /**
   * Return the block of code that can be used to construct a value using the (num)th constructor in
   * this BitdataRep.
   */
  Block bitdataConsBlock(int num) {
    if (consBlocks == null) {
      // First time for this BitdataRep?  Make a new array ...
      consBlocks = new Block[cfuns.length];
    }
    if (consBlocks[num] == null) {
      // First time for this value of num?  Make a new block ...
      return consBlocks[num] = layouts[num].makeConsBlock(cfuns[num]);
    }
    // Return the previously constructed block
    return consBlocks[num];
  }

  /**
   * Stores a cache of of blocks for selecting fields from this BitdataRep. Specifically,
   * selBlocks[num][n] will hold a block that selects the nth field of the numth constructor.
   */
  private Block[][] selBlocks = null;

  /**
   * Return a block that can be called to select the nth field of the (num)th constructor of this
   * BitdataRep. New blocks are generated on demand and added to the selBlocks cache so that they
   * can be reused for subsequent calls, if needed.
   */
  Block bitdataSelBlock(int num, int n) {
    if (selBlocks == null) {
      // First time for this BitdataRep?  Make a new array ...
      selBlocks = new Block[cfuns.length][];
    }
    if (selBlocks[num] == null) {
      // First time for this value of num?  Make a new (sub)array ...
      selBlocks[num] = new Block[layouts[num].getArity()];
    }
    if (selBlocks[num][n] == null) {
      // First time for this n?  Make a new block ...
      return selBlocks[num][n] = layouts[num].makeSelBlock(cfuns[num], n);
    }
    // Return the previously constructed block
    return selBlocks[num][n];
  }
}
