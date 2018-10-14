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
 * A generalized conditional jump to one of several blocks, the choice being determined by matching
 * a specific value against a list of options, with a default to fall back on if necessary.
 */
public class Switch extends Code {

  /** The value to use in making the branch decisions. */
  private Value v;

  /** The list of numeric options to match against. */
  private Value[] nums;

  /** A list of block labels, one for each option in nums. */
  private String[] bs;

  /** The label of the default block if no other applies. */
  private String def;

  /** Default constructor. */
  public Switch(Value v, Value[] nums, String[] bs, String def) {
    this.v = v;
    this.nums = nums;
    this.bs = bs;
    this.def = def;
  }

  /** Print out this code sequence to the specified PrintWriter. */
  public void print(PrintWriter out) {
    out.print("  switch " + v + ", label %" + def + " [");
    for (int i = 0; i < nums.length; i++) {
      out.print("\n      " + nums[i] + ", label %" + bs[i]);
    }
    out.println(" ]");
  }
}
