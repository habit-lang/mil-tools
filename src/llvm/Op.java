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

import java.io.PrintWriter;

/**
 * A basic operation: perform the specified right hand operation, capture the result in the
 * specified location, and then continue with the rest of the code in the block.
 */
public class Op extends Code {

  /** The left hand side where the result will be saved. */
  private Local lhs;

  /** The right hand side operation. */
  private Rhs rhs;

  /** Points to the rest of the code in this basic block. */
  private Code next;

  /** Default constructor. */
  public Op(Local lhs, Rhs rhs, Code next) {
    this.lhs = lhs;
    this.rhs = rhs;
    this.next = next;
  }

  public Op(Local lhs, Expr expr, Code next) {
    this(lhs, new Eval(expr), next);
  }

  /** Print out this code sequence to the specified PrintWriter. */
  public void print(PrintWriter out) {
    out.println("  " + lhs.getName() + " = " + rhs);
    next.print(out);
  }

  /**
   * Utility function for reverseOnto(): handle type specific manipulations for adjusting the
   * pointers in this Code object when it is added to the front of the specified code sequence.
   */
  Code rotateOnto(Code rest) {
    Code next = this.next;
    this.next = rest;
    return next;
  }
}
