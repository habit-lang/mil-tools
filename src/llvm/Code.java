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

/** Represents a sequence of code in an LLVM basic block. */
public abstract class Code {

  /** Print out this code sequence to the specified PrintWriter. */
  public abstract void print(PrintWriter out);

  /**
   * Reverse the elements of the first code sequence (viewed as a null-terminated linked list of
   * CodeComment, Op, CallVoid, and Store instructions) onto the front of the second code sequence.
   */
  public static Code reverseOnto(Code edoc, Code rest) {
    while (edoc != null) {
      Code next = edoc.rotateOnto(rest);
      rest = edoc;
      edoc = next;
    }
    return rest;
  }

  /**
   * Utility function for reverseOnto(): handle type specific manipulations for adjusting the
   * pointers in this Code object when it is added to the front of the specified code sequence.
   */
  Code rotateOnto(Code rest) {
    debug.Internal.error("called rotateOnto for a terminator");
    return null;
  }
}
