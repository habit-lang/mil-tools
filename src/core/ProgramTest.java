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
package core;

import compiler.*;
import mil.*;

public class ProgramTest {

  public static void main(String[] args) {
    debug.Log.on();
    Handler handler = new SimpleHandler();
    try {
      Source source = new StdinSource(handler);
      CoreProgram program = new SyntaxAnalysis(handler).analyze(source);
      TyconEnv env = new StaticAnalysis(handler).analyze(program);
      System.out.println("done!");
    } catch (Failure f) {
      handler.report(f);
    } catch (Exception e) {
      handler.report(new Failure("Exception: " + e));
      // e.printStackTrace();
    }
  }
}
