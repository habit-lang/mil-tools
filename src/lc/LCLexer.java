/*
    Copyright 2018-19 Mark P Jones, Portland State University

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
package lc;

import compiler.*;
import core.*;
import mil.*;

/** A lexical analyzer for the LC language. */
class LCLexer extends LayoutLexer implements LCTokens {

  LCLexer(Handler handler, boolean optional, Source source) {
    super(handler, optional, source);
    reserved.put("do", Integer.valueOf(DO));
    reserved.put("\\", Integer.valueOf(LAMBDA));
    reserved.put("&&", Integer.valueOf(AMPAMP));
    reserved.put("||", Integer.valueOf(BARBAR));
    reserved.put("where", Integer.valueOf(WHERE));
    nextToken(); // Start the token stream
  }

  /** Return a printable representation of a token. */
  public String describeToken(int token, String lexeme) {
    switch (token) {
      case DO:
        return "\"do\" keyword";
      case LAMBDA:
        return "\\ (lambda)";
      case AMPAMP:
        return "&& (logical and)";
      case BARBAR:
        return "|| (logical or)";
      case WHERE:
        return "\"where\" keyword";
      default:
        return super.describeToken(token, lexeme);
    }
  }
}
