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
package driver;

import compiler.*;
import java.io.IOException;
import java.io.PrintWriter;
import lc.*;
import mil.*;

class Main {

  /** A command line entry point. */
  public static void main(String[] args) {
    if (args.length < 1) {
      usage();
    } else {
      new Main().run(args);
    }
  }

  public static void usage() {
    System.err.println("usage: java driver.Main [options] inputFile ...");
    System.err.println("options: -d             debugging on");
    System.err.println("         -v             verbose on");
    System.err.println("         -p{c,o,b,s,r}* passes");
    System.err.println("                        c = cfun rewrite");
    System.err.println("                        o = optimizer");
    System.err.println("                        s = specialization (eliminate polymorphism)");
    System.err.println("                        b = bitdata generation (immediately after s)");
    System.err.println(
        "                        r = representation transformation (requires earlier s)");
    System.err.println("         -m[filename]   mil code");
    System.err.println("         -g[filename]   GraphViz file for mil structure");
    System.err.println("         -c[filename]   type set");
    System.err.println("         -s[filename]   specialization type set (requires s)");
    System.err.println("         -r[filename]   representation type set (requires r)");
    System.err.println("         -l[filename]   LLVM code (requires s)");
    System.err.println("         -b[filename]   bytecode text");
    System.err.println("         -x[filename]   execute bytecode");
    System.err.println("         --mil-main=N   Set name of main function in MIL input");
    System.err.println("         --llvm-main=N  Set name of main function in LLVM output");
  }

  private boolean trace = false;

  private void message(String msg) {
    if (trace) {
      System.out.println(msg);
    }
  }

  private String passes = null;

  private void addPasses(String ps) {
    passes = (passes == null) ? ps : (passes + ps);
  }

  private FilenameOption milOutput = new FilenameOption("MIL output file");

  private FilenameOption graphvizOutput = new FilenameOption("MIL code GraphViz output");

  private FilenameOption typesetOutput = new FilenameOption("type set output");

  private FilenameOption specTypesetOutput = new FilenameOption("specialization type set output");

  private FilenameOption repTypesetOutput = new FilenameOption("representation type set output");

  private FilenameOption llvmOutput = new FilenameOption("llvm output");

  private FilenameOption bytecodeOutput = new FilenameOption("bytecode output");

  private FilenameOption execOutput = new FilenameOption("execution output");

  private String milMain = "";

  /** Simple command line option processing. */
  private void options(String str) throws Failure {
    String special;
    if ((special = nonemptyOptString("--llvm-main=", str)) != null) {
      llvm.FuncDefn.mainFunctionName = special;
      return;
    } else if ((special = nonemptyOptString("--mil-main=", str)) != null) {
      milMain = special;
      return;
    }
    for (int i = 1; i < str.length(); i++) {
      switch (str.charAt(i)) {
        case 'd':
          debug.Log.on();
          break;
        case 'v':
          trace = true;
          break;
        case 'p':
          addPasses(str.substring(i + 1));
          return;
        case 'm':
          milOutput.setName(str, i);
          return;
        case 'g':
          graphvizOutput.setName(str, i);
          return;
        case 'c':
          typesetOutput.setName(str, i);
          return;
        case 's':
          specTypesetOutput.setName(str, i);
          return;
        case 'r':
          repTypesetOutput.setName(str, i);
          return;
        case 'l':
          llvmOutput.setName(str, i);
          return;
        case 'b':
          bytecodeOutput.setName(str, i);
          return;
        case 'x':
          execOutput.setName(str, i);
          return;
        default:
          throw new Failure("Unrecognized option character '" + str.charAt(i) + "'");
      }
    }
  }

  private static String optString(String prefix, String str) {
    return (str.startsWith(prefix)) ? str.substring(prefix.length()) : null;
  }

  private static String nonemptyOptString(String prefix, String str) throws Failure {
    String s = optString(prefix, str);
    if (s != null && s.length() == 0) {
      throw new Failure("Missing value for option " + prefix);
    }
    return s;
  }

  public void run(String[] args) {
    Handler handler = new SimpleHandler();
    try {
      process(handler, load(handler, args));
    } catch (Failure f) {
      handler.report(f);
      System.exit(-1);
    }
  }

  /**
   * Process command line arguments and load the requested source files, compiling as necessary to
   * produce a single MIL program.
   */
  private MILProgram load(Handler handler, String[] args) throws Failure {
    // TODO: initial message will not appear so long as trace is initialized to false :-)
    message("Process arguments ..."); // Process command line arguments
    LCLoader loader = new LCLoader();
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        options(args[i]);
      } else if (!loader.loadMIL(args[i])) { // Try to load as mil
        loader.require(args[i]); // But otherwise load as lc
      }
    }
    message("Loading source files ..."); // Load and compile everything
    MILProgram mil = loader.load(handler, milMain);

    message("Running type checker ..."); // Sanity check/dependency analysis
    mil.typeChecking(handler);

    return mil;
  }

  private void process(Handler handler, MILProgram mil) throws Failure {
    MILSpec spec = null;
    RepTypeSet rep = null;
    boolean optimized = false; // Keep track whether the optimizer has been run

    if (passes == null) {
      // If no passes are specified, try to set some sensible defaults to satisfy
      // requirements implied by other command line arguments.  This does not prevent
      // errors that can occur with user-specified pass strings (e.g., use of 'r' without
      // a prior 's', or an attempt to generate LLVM code without specialization).

      passes =
          llvmOutput.isSet()
              ? "cosboro"
              : repTypesetOutput.isSet() ? "cosor" : specTypesetOutput.isSet() ? "cos" : "co";
      message("Defaulting to passes \"" + passes + "\":");
    }

    for (int i = 0; i < passes.length(); i++) {
      switch (passes.charAt(i)) {
        case 'c': // Constructor function rewrite
          message("Performing constructor function rewrite ...");
          mil.cfunRewrite();
          optimized = false;
          break;

        case 'o': // MIL optimizer
          message("Running optimizer ...");
          mil.optimize();
          optimized = true;
          break;

        case 's': // Specialization
          message("Running specializer ...");
          spec = mil.specialize(handler);
          handler.abortOnFailures();
          mil = spec.getProg();
          optimized = false;
          break;

        case 'b': // Bitdata generation
          message("Running bitdata generation ...");
          if (i == 0 || passes.charAt(i - 1) != 's') {
            throw new Failure(
                "Bitdata generation can only be used immediately after a specialization pass");
          }
          mil.bitdataRewrite(spec.bitdataCandidates());
          optimized = false;
          External.setBitdataRepresentations();
          break;

        case 'r': // Representation transformation
          message("Running representation transformation ...");
          if (spec == null) {
            throw new Failure(
                "Representation transformation only valid after an earlier specialization pass");
          }
          rep = mil.repTransform(handler);
          handler.abortOnFailures();
          mil.shake();
          optimized = false;
          break;

        default:
          throw new Failure("Unrecognized pass option \"" + passes.charAt(i) + "\"");
      }
      message("Running type checker ...");
      mil.typeChecking(handler);
      handler.abortOnFailures();
    }
    output(handler, mil, spec, rep, optimized);
  }

  /**
   * Generate any outputs that have been requested in this program, passing in the final resuts of
   * the compilation passes as arguments.
   */
  private void output(
      final Handler handler,
      final MILProgram mil,
      final MILSpec spec,
      final RepTypeSet rep,
      final boolean optimized)
      throws Failure {

    typesetOutput.run(
        new Action() {
          void run(PrintWriter out) {
            TypeSet set = new TypeSet();
            mil.collect(set);
            set.dump(out);
          }
        });

    specTypesetOutput.run(
        new Action() {
          void run(PrintWriter out) throws Failure {
            if (spec == null) {
              throw new Failure(
                  "A specialization pass is required for specialization type set output");
            } else {
              spec.dump(out);
            }
          }
        });

    repTypesetOutput.run(
        new Action() {
          void run(PrintWriter out) throws Failure {
            if (rep == null) {
              throw new Failure(
                  "A representation pass is required for representation type set output");
            } else {
              rep.dump(out);
            }
          }
        });

    milOutput.run(
        new Action() {
          void run(PrintWriter out) {
            TypeSet set = new TypeSet(); // TODO: can we reuse the set from typesetOutput?
            mil.collect(set);
            set.dumpTypeDefinitions(out);
            mil.dump(out);
          }
        });

    graphvizOutput.run(
        new Action() {
          void run(PrintWriter out) {
            mil.toDot(out);
          }
        });

    llvmOutput.run(
        new Action() {
          void run(PrintWriter out) throws Failure {
            if (!optimized) {
              throw new Failure("An optimization pass is required for LLVM output");
            } else if (spec == null) {
              throw new Failure("A specialization pass is required for LLVM output");
            } else {
              mil.toLLVM().dump(out);
            }
          }
        });

    if (bytecodeOutput.isSet() || execOutput.isSet()) {
      final MachineBuilder builder = mil.generateMachineBuilder(handler);
      final Machine machine = builder.getMachine();
      handler.abortOnFailures();

      bytecodeOutput.run(
          new Action() {
            void run(PrintWriter out) throws Failure {
              HashAddrMap addrMap = builder.makeAddrMap(handler);
              handler.abortOnFailures();
              machine.dump(out, addrMap);
            }
          });

      execOutput.run(
          new Action() {
            void run(PrintWriter out) {
              machine.exec(out, 0);
              out.println(machine.getInstrCount() + " instructions executed");
              out.println("Maximum call depth " + machine.getMaxCallDepth());
            }
          });
    }

    handler.abortOnFailures(); // Just to be sure ...
    message("Success!");
  }

  /**
   * Encapsulates a filename option that can be set to a non-null value to request output either to
   * standard output (if the filename is empty) or else to a named file.
   */
  class FilenameOption {

    private String description;

    /** Default constructor. */
    FilenameOption(String description) {
      this.description = description;
    }

    /**
     * The filename is one of: - null, indicating no action is required - empty, indicating output
     * to stdout is required - nonempty, indicating output to file is required
     */
    private String filename = null;

    /** Test to determine if this option has been set. */
    public boolean isSet() {
      return filename != null;
    }

    /** Set the filename for this output option. */
    public void setName(String filename) throws Failure {
      if (this.filename != null) {
        throw new Failure("Multiple settings for " + description);
      }
      this.filename = filename;
    }

    public void setName(String str, int i) throws Failure {
      setName(str.substring(i + 1));
    }

    /**
     * Run the specified action for this option in an appropriate way, skipping if the filename has
     * not been set.
     */
    void run(Action action) throws Failure {
      if (filename != null) {
        if (filename.equals("")) { // Output to System.out
          message("*** " + description + ":");
          PrintWriter out = new PrintWriter(System.out);
          action.run(out);
          out.flush();
        } else {
          message("Writing " + description + " to \"" + filename + "\" ...");
          try {
            PrintWriter out = new PrintWriter(filename);
            action.run(out);
            out.close();
          } catch (IOException e) {
            System.out.println(
                "Attempt to create " + description + " in \"" + filename + "\" failed");
          }
        }
      }
    }
  }
}
