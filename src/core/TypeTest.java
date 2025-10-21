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

public class TypeTest {

  public static void main(String[] args) {
    Handler handler = new SimpleHandler();
    Source source = new StdinSource(handler);
    CoreLexer lexer = new CoreLexer(handler, source);
    CoreParser parser = new CoreParser(handler, lexer);
    lexer.nextToken(); // read a token to prime the lexer
    TypeExp te = parser.sneakTypeExp();
    parser.checkForEnd();
    System.out.println("parsing complete");
    try {
      Scheme s = te.toScheme(TyconEnv.builtin);
      System.out.println("Type scheme is: " + s);
    } catch (Failure f) {
      handler.report(f);
    }
    System.out.println("done!");
  }
}
