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


/** Binary operators. */
public abstract class BinOp extends Rhs {

  /** The type of the result. */
  protected Type ty;

  /** The left operand. */
  private Value l;

  /** The right operand. */
  private Value r;

  /** Default constructor. */
  public BinOp(Type ty, Value l, Value r) {
    this.ty = ty;
    this.l = l;
    this.r = r;
  }

  /**
   * Return the result type of this operation, which by default is the same as the type of the
   * arguments.
   */
  public Type resultType() {
    return ty;
  }

  /** Append a printable string for this instruction to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append(binOpString());
    buf.append(" ");
    ty.append(buf);
    buf.append(" ");
    l.appendName(buf);
    buf.append(", ");
    r.appendName(buf);
  }

  /** Return the LLVM opcode for this binary operation. */
  public abstract String binOpString();
}
