/*
    Copyright 2018-25 Mark P Jones, Portland State University

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


/** Base class for values (i.e., operands) in LLVM instructions. */
public abstract class Value {

  /** Return the LLVM type of this value. */
  public abstract Type getType();

  /**
   * Return the LLVM name for this value. We provide a default definition that relies on the
   * existence of an appendName() implementation, which has a default implementation in terms of
   * getName(). To avoid an infinite loop, at least one of these methods must be implemented for
   * every concrete instance of Value.
   */
  public String getName() {
    StringBuilder buf = new StringBuilder();
    appendName(buf);
    return buf.toString();
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append(getName());
  }

  /**
   * Return a string representation for this value that includes both the type and the name with a
   * single space between them.
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    append(buf);
    return buf.toString();
  }

  /** Append a string representation for this value to the specified buffer. */
  public void append(StringBuilder buf) {
    getType().append(buf);
    buf.append(" ");
    appendName(buf);
  }

  /**
   * Append a parameter list, represented by a list of values, to a buffer. This is intended for use
   * in displaying the values of the arguments in a Call or CallVoid.
   */
  static void append(StringBuilder buf, String open, Value[] vals, String close) {
    buf.append(open);
    for (int i = 0; i < vals.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      vals[i].append(buf);
    }
    buf.append(close);
  }

  /**
   * Return a string representation for a list of values, formatted as a comma-separated list
   * enclosed in parentheses.
   */
  public static String toString(Value[] args) {
    StringBuilder buf = new StringBuilder();
    Value.append(buf, "(", args, ")");
    return buf.toString();
  }
}
