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
package llvm;


/**
 * Represents a phi function. TODO: This abstract syntax does not reflect the fact that all Phi
 * functions must appear at the start of a block before any other code.
 */
public class Phi extends Rhs {

  /** List of predecessor blocks. */
  private String[] blocks;

  /**
   * List of values passed in from predecessor blocks. (Length and order must match blocks array.)
   */
  private Value[] values;

  /** Default constructor. */
  public Phi(String[] blocks, Value[] values) {
    this.blocks = blocks;
    this.values = values;
  }

  /** Append a printable string for this instruction to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append("phi ");
    values[0].getType().append(buf);
    buf.append(" ");
    for (int i = 0; i < blocks.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append("[");
      values[i].appendName(buf);
      buf.append(", %");
      buf.append(blocks[i]);
      buf.append("]");
    }
  }
}
