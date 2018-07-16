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

/**
 * A lexical analyser that uses an array of Token objects to provide its input. Used to illustrate
 * alternative implementations of Lexer.
 */
public class TokenArrayLexer extends Lexer {

  private Token[] tokens;

  private int count = 0;

  /** Construct a lexical analysis phase with a specified diagnostic handler. */
  public TokenArrayLexer(Handler handler, Token[] tokens) {
    super(handler);
    this.tokens = tokens;
  }

  /** Return the next token from the array. */
  public int nextToken() {
    if (tokens == null || count >= tokens.length) {
      lexemeText = null;
      return token = 0;
    } else {
      token = tokens[count].getCode();
      lexemeText = tokens[count].getText();
      count++;
      return token;
    }
  }

  /** Return the position of the current token in the array. */
  public Position getPos() {
    return new TokenArrayPosition(count);
  }

  /** Close the token input stream by nulling out the token array. */
  public void close() {
    tokens = null;
  }
}
