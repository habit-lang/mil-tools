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
 * Integer binary operations return an integer result by combining two arguments of the same
 * (integer) type.
 */
public class IOp extends BinOp {

  private String opcode;

  /** Default constructor. */
  public IOp(Type ty, Value l, Value r, String opcode) {
    super(ty, l, r);
    this.opcode = opcode;
  }

  /** Return the LLVM opcode for this binary operation. */
  public String binOpString() {
    return opcode;
  }
}
