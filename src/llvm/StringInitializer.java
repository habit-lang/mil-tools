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


/** Represents a string initializer for an array of characters. */
public class StringInitializer extends Value {

  /** A string, to be stored with a null terminator. */
  private String str;

  /** Default constructor. */
  public StringInitializer(String str) {
    this.str = str;
    this.arrayType = new ArrayType(1 + str.length(), Type.i8);
  }

  private ArrayType arrayType;

  /** Return the LLVM type of this value. */
  public Type getType() {
    return arrayType;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append("c\""); // Begin string literal
    for (int i = 0; i < str.length(); i++) {
      int c = str.charAt(i);
      if (c >= 32 && c <= 126 && c != '\\') { // printable chars: [32..126]
        buf.append((char) c);
      } else {
        buf.append('\\');
        buf.append(hexDigit(c >> 4));
        buf.append(hexDigit(c));
      }
    }
    buf.append("\\00\""); // Add null terminator and end string
  }

  private static char hexDigit(int c) {
    return (char) (((c &= 0xf) < 10) ? ('0' + c) : ('a' + (c - 10)));
  }
}
