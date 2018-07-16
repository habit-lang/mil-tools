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
package compiler;

/** Provides a representation for the Tokens that are used in a TokenArrayLexer. */
public class Token {

  private int code;

  private String text;

  public Token(int code, String text) {
    this.code = code;
    this.text = text;
  }

  public Token(int code) {
    this(code, null);
  }

  public int getCode() {
    return code;
  }

  public String getText() {
    return text;
  }
}
