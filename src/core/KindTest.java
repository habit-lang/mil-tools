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
package core;

import compiler.*;
import mil.*;

public class KindTest {

  public static void main(String[] args) {
    Handler handler = new SimpleHandler();
    Source source = new StdinSource(handler);
    CoreLexer lexer = new CoreLexer(handler, source);
    CoreParser parser = new CoreParser(handler, lexer);
    lexer.nextToken(); // read a token to prime the lexer
    KindExp ke = parser.sneakKindExp();
    parser.checkForEnd();
    System.out.println("parsing complete");

    try {
      Kind k = ke.toKind();
      System.out.println("Kind: " + k);
    } catch (Failure f) {
      handler.report(f);
    }

    System.out.println("done!");
  }
}
