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


/** Represents a getelementptr instruction. */
public class Getelementptr extends Rhs {

  /** Base pointer. */
  private Value ptr;

  /** The index from the pointer. */
  private Value[] offsets;

  /** Default constructor. */
  public Getelementptr(Value ptr, Value[] offsets) {
    this.ptr = ptr;
    this.offsets = offsets;
  }

  public Getelementptr(Value ptr, Value o1) {
    this(ptr, new Value[] {o1});
  }

  public Getelementptr(Value ptr, Value o1, Value o2) {
    this(ptr, new Value[] {o1, o2});
  }

  public Getelementptr(Value ptr, Value o1, Value o2, Value o3) {
    this(ptr, new Value[] {o1, o2, o3});
  }

  /** Append a printable string for this instruction to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append("getelementptr inbounds ");
    ptr.getType().ptsTo().append(buf);
    buf.append(", ");
    ptr.append(buf);
    for (int i = 0; i < offsets.length; i++) {
      buf.append(", ");
      offsets[i].append(buf);
    }
  }
}
