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

/** Branch to the start of another basic block, ending the current block. */
public class Goto extends Code {

  /** The the label of the block that we should branch to. */
  private String b;

  /** Default constructor. */
  public Goto(String b) {
    this.b = b;
  }

  /** Print out this code sequence to the specified PrintWriter. */
  public void print(PrintWriter out) {
    out.println("  br label %" + b);
  }
}
