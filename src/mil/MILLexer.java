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
package mil;

import compiler.*;
import compiler.Handler;
import core.*;

/** A lexical analyzer for the MIL language. */
public class MILLexer extends LayoutLexer implements MILTokens {

  /** Construct a lexical analyzer for the MIL language. */
  public MILLexer(Handler handler, boolean optional, Source source) {
    super(handler, optional, source);
    reserved.put("primitive", new Integer(PRIMITIVE));
    reserved.put("return", new Integer(RETURN));
    reserved.put("assert", new Integer(ASSERT));
    reserved.put("@", new Integer(APPLY));
    reserved.put("_", new Integer(UNDER));
    reserved.put(">>=", new Integer(TBIND));
    reserved.put(Tycon.milArrowId, new Integer(MILTO));
    nextToken(); // force reading of first token
  }

  /** Return a printable representation of a token. */
  public String describeToken(int token, String lexeme) {
    switch (token) {
      case PRIMITIVE:
        return "\"primitive\" keyword";
      case RETURN:
        return "\"return\" keyword";
      case ASSERT:
        return "\"assert\" keyword";
      case APPLY:
        return "@";
      case UNDER:
        return "_ (underscore)";
      case TBIND:
        return ">>= (block type arrow)";
      case MILTO:
        return "\"" + Tycon.milArrowId + "\" symbol";
      default:
        return super.describeToken(token, lexeme);
    }
  }
}
