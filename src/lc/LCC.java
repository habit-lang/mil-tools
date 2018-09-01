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
package lc;

import compiler.*;
import core.*;
import mil.*;

class LCC {

  /** A command line entry point. */
  public static void main(String[] args) {
    if (args.length < 1) {
      usage();
    } else {
      Handler handler = new SimpleHandler();

      llvm.FuncDefn.mainFunctionName = "init";

      // Load primitive definitions:
      // TODO: find a better time/place to do this
      //    MILProgram.loadPrims(handler, "prims.mil");  // TODO: avoid hardwiring name

      // Process command line arguments and load specified files:
      LCLoader loader = new LCLoader();
      try {
        for (int i = 0; i < args.length; i++) {
          if (args[i].startsWith("-")) {
            options(args[i]);
          } else if (!loader.loadMIL(args[i])) { // Try to load as mil
            loader.require(args[i]); // But otherwise load as lc
          }
        }

        // Load the source code:
        MILProgram mil = loader.load(handler, "");

        // Run the MIL type checker as an initial dependency analysis and sanity check:
        mil.typeChecking(handler);

        if (showInitial) {
          System.out.println("Initial version: ========================");
          mil.dump(); // Output result
        }
        if (graphvizInitial) {
          mil.toDot("initial.dot");
        }
        if (fileInitial) {
          mil.dump("initial.mil");
        }
        if (specialize) {
          MILSpec spec = mil.specialize(handler);
          spec.dump(); // TODO: drop this
          mil = spec.getProg();
          System.out.println("Specialized version: ====================");
          mil.typeChecking(handler);
          mil.dump(); // Output result
        }

        if (showFinal || graphvizFinal || fileFinal) {
          mil.cfunRewrite();
          if (specialize) { // TODO: find a better way to integrate this
            mil.repTransform(handler);
            handler.abortOnFailures();
            mil.shake();
            mil.dump();
          }
          mil.optimize();
          // Rerun type checker for dependency analysis and sanity check on optimized code
          mil.typeChecking(handler);
          if (showFinal) {
            System.out.println("Optimized version: ======================");
            mil.dump(); // Output result
            mil.toLLVM().dump("output.ll");
          }
          if (graphvizFinal) {
            mil.toDot("final.dot");
          }
          if (fileFinal) {
            mil.dump("final.mil");
          }
        }

        if (computeTypeSet) {
          System.out.println("TypeSet: ================================");
          TypeSet set = new TypeSet();
          mil.collect(set);
          set.dump();
        }

        if (bytecode || execute) {
          MachineBuilder builder = mil.generateMachineBuilder(handler);
          Machine machine = builder.getMachine();
          handler.abortOnFailures();
          if (bytecode) {
            System.out.println("Bytecode: ===============================");
            HashAddrMap addrMap = builder.makeAddrMap(handler);
            handler.abortOnFailures();
            machine.dump(addrMap);
          }
          if (execute) {
            System.out.println("Execute: ================================");
            machine.exec(0);
            System.out.println(machine.getInstrCount() + " instructions executed");
            System.out.println("Maximum call depth " + machine.getMaxCallDepth());
          }
        }

        if (showInitial || showFinal || computeTypeSet || execute) {
          System.out.println("=========================================");
        }

        System.out.println("Success!");
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  private static boolean showInitial = false;

  private static boolean showFinal = false;

  private static boolean graphvizInitial = false;

  private static boolean graphvizFinal = false;

  private static boolean fileInitial = false;

  private static boolean fileFinal = false;

  private static boolean computeTypeSet = false;

  private static boolean specialize = false;

  private static boolean bytecode = false;

  private static boolean execute = false;

  /** Simple command line option processing. */
  private static void options(String str) throws Failure {
    for (int i = 1; i < str.length(); i++) {
      switch (str.charAt(i)) {
        case 'd':
          debug.Log.on();
          break;

        case 'S':
          showInitial = true;
          break;
        case 's':
          showFinal = true;
          break;

        case 'G':
          graphvizInitial = true;
          break;
        case 'g':
          graphvizFinal = true;
          break;

        case 'F':
          fileInitial = true;
          break;
        case 'f':
          fileFinal = true;
          break;

        case 'c':
          computeTypeSet = true;
          break;
        case 'z':
          specialize = true;
          break;

        case 'b':
          bytecode = true;
          break;
        case 'x':
          execute = true;
          break;

        default:
          throw new Failure("Unrecognized option character '" + str.charAt(i) + "'");
      }
    }
  }

  /** Display usage message. */
  private static void usage() {
    System.err.println("usage:   java lc.LCC [options] inputFile ...");
    System.err.println("options: -d       debugging on");
    System.err.println("         -S, -s   show MIL code (initial and/or final)");
    System.err.println("         -F, -f   save MIL code to file (initial.mil and/or final.mil)");
    System.err.println("         -G, -g   generate Graphviz file (initial.dot and/or final.dot)");
    System.err.println("         -c       compute and display type set");
    System.err.println("         -z       specialize polymorphism");
    System.err.println("         -b       generate and display bytecode");
    System.err.println("         -x       execute bytecode");
  }
}
