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
 * Integer comparison operations return a boolean as the result of comparing two integral values of
 * the same type.
 */
public class ICmp extends BinOp {

  private String cond;

  /** Default constructor. */
  public ICmp(Type ty, Value l, Value r, String cond) {
    super(ty, l, r);
    this.cond = cond;
  }

  /**
   * Return the result type of this operation, which by default is the same as the type of the
   * arguments.
   */
  public Type resultType() {
    return Type.i1;
  }

  /** Return the LLVM opcode for this binary operation. */
  public String binOpString() {
    return "icmp " + cond;
  }
}
