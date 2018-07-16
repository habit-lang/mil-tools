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
import compiler.Failure;
import compiler.Handler;
import core.*;

public class Asm {

  /** A command line entry point. */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("usage: java mil.Asm [options] inputFile ...`");
      System.err.println("options include: -d  debug");
      System.err.println("                 -I  show initial code");
      System.err.println("                 -O  show optimized code");
    } else {
      Handler handler = new SimpleHandler();
      MILLoader loader = new MILLoader();
      try {
        for (int i = 0; i < args.length; i++) {
          if (args[i].startsWith("-")) {
            options(args[i]);
          } else {
            loader.require(args[i]);
          }
        }
        MILProgram mil = new MILProgram();
        MILEnv milenv = loader.load(handler, mil);
        // TODO: what are we going to do with milenv?

        mil.shake(); // Initial dependency analysis

        if (showInitial) {
          System.out.println("Initial version: ========================");
          mil.dump(); // Output result
          mil.toDot("initial.dot");
        }

        if (showOptimized) {
          System.out.println("Running optimizer: ======================");
          mil.cfunRewrite();
          mil.optimize();
          mil.shake(); // Initial dependency analysis
          mil.dump(); // Output result
          mil.toDot("final.dot");
        }

        if (showInitial || showOptimized) {
          System.out.println("=========================================");
        }
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  private static boolean showInitial = false;

  private static boolean showOptimized = false;

  /** Simple command line option processing. */
  private static void options(String str) throws Failure {
    for (int i = 1; i < str.length(); i++) {
      switch (str.charAt(i)) {
        case 'd':
          debug.Log.on();
          break;
        case 'I':
          showInitial = true;
          break;
        case 'O':
          showOptimized = true;
          break;
        default:
          throw new Failure("Unrecognized option character '" + str.charAt(i) + "'");
      }
    }
  }
}
