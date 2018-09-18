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
import java.io.*;
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
    System.err.println("usage: milc [inputs|options]");
    System.err.println("inputs:  filename.milc  Load options from specified file");
    System.err.println(
        "         filename.mil   Load MIL source from specified file (.lmil for literate)");
    System.err.println(
        "         filename.lc    Load LC source from specified file (.llc for literate)");
    System.err.println("options: -v             verbose on");
    System.err.println("         -d             display debug messages");
    System.err.println("         -ipathlist     append items to input search path");
    System.err.println("         -p{c,o,b,s,r}* passes");
    System.err.println("                        c = cfun rewrite");
    System.err.println("                        o = optimizer");
    System.err.println("                        s = specialization (eliminate polymorphism)");
    System.err.println("                        b = bitdata generation");
    System.err.println(
        "                        r = representation transformation (requires earlier s)");
    System.err.println("         -m[filename]   mil code");
    System.err.println("         -t[filename]   type definitions");
    System.err.println("         -g[filename]   GraphViz file for mil structure");
    System.err.println("         -c[filename]   type set");
    System.err.println("         -s[filename]   specialization type set (requires s)");
    System.err.println("         -r[filename]   representation type set (requires r)");
    System.err.println("         -l[filename]   LLVM code (requires s)");
    System.err.println("         -f[filename]   LLVM interface (requires s)");
    System.err.println("         -b[filename]   bytecode text");
    System.err.println("         -x[filename]   execute bytecode");
    System.err.println("         --mil-main=N   Set name of main function in MIL input");
    System.err.println("         --llvm-main=N  Set name of main function in LLVM output");
    System.err.println("         --standalone   Equivalent to --mil-main=main --llvm-main=main");
    System.err.println("         --32 / --64    Set wordsize to 32 / 64 bits");
    System.err.println("         --target=T     Set LLVM target triple to T");
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

  private FilenameOption typeDefnsOutput = new FilenameOption("Type definitions");

  private FilenameOption graphvizOutput = new FilenameOption("MIL code GraphViz output");

  private FilenameOption typeSetOutput = new FilenameOption("type set output");

  private FilenameOption specTypeSetOutput = new FilenameOption("specialization type set output");

  private FilenameOption repTypeSetOutput = new FilenameOption("representation type set output");

  private FilenameOption llvmOutput = new FilenameOption("llvm output");

  private FilenameOption llvmInterfaceOutput = new FilenameOption("llvm interface");

  private FilenameOption bytecodeOutput = new FilenameOption("bytecode output");

  private FilenameOption execOutput = new FilenameOption("execution output");

  private String milMain = "";

  /** Simple command line argument processing. */
  private void options(Handler handler, String str, LCLoader loader, boolean nested)
      throws Failure {
    if (str.startsWith("-")) {
      String special;
      if ((special = nonemptyOptString("--llvm-main=", str)) != null) {
        llvm.FuncDefn.mainFunctionName = special;
        return;
      } else if ((special = nonemptyOptString("--mil-main=", str)) != null) {
        milMain = special;
        return;
      } else if (optMatches("--standalone", str)) {
        milMain = llvm.FuncDefn.mainFunctionName = "main";
        return;
      } else if ((special = nonemptyOptString("--target=", str)) != null) {
        llvm.Program.targetTriple = special;
        return;
      } else if (optMatches("--32", str)) {
        Word.setSize(32);
        return;
      } else if (optMatches("--64", str)) {
        Word.setSize(64);
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
          case 'i':
            loader.extendSearchPath(str, i + 1);
            return;
          case 'p':
            addPasses(str.substring(i + 1));
            return;
          case 'm':
            milOutput.setName(str, i);
            return;
          case 't':
            typeDefnsOutput.setName(str, i);
            return;
          case 'g':
            graphvizOutput.setName(str, i);
            return;
          case 'c':
            typeSetOutput.setName(str, i);
            return;
          case 's':
            specTypeSetOutput.setName(str, i);
            return;
          case 'r':
            repTypeSetOutput.setName(str, i);
            return;
          case 'l':
            llvmOutput.setName(str, i);
            return;
          case 'f':
            llvmInterfaceOutput.setName(str, i);
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
    } else if (str.endsWith(".milc")) {
      if (nested) {
        throw new Failure("Cannot read options from nested configuration file \"" + str + "\"");
      } else if (!optionsFromFile(handler, str, loader, true)) {
        throw new Failure("Error reading options from \"" + str + "\"; file not accessible");
      }
    } else if (!loader.loadMIL(str)) { // Try to load as mil
      loader.require(str); // But otherwise load as lc
    }
  }

  private static boolean optMatches(String opt, String str) throws Failure {
    if (!str.startsWith(opt)) {
      return false;
    } else if (!str.equals(opt)) {
      throw new Failure("Extra characters on command line option \"" + str + "\"");
    }
    return true;
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

  private boolean optionsFromFile(Handler handler, String name, LCLoader loader, boolean nested)
      throws Failure {
    try {
      message("Reading options from " + name + " ...");
      Reader reader = new FileReader(name);
      Source source = new OptionSource(handler, reader, name);
      String line;
      while ((line = source.readLine()) != null) {
        options(handler, line, loader, true);
      }
      source.close();
      return true;
    } catch (FileNotFoundException e) {
      return false;
    }
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
    // TODO: initial messages will not appear so long as trace is initialized to false :-)
    LCLoader loader = new LCLoader();
    if (optionsFromFile(handler, ".milc", loader, false)) {
      message("Read options from .milc ..."); // Process options in .milc file, if present
    }
    ;
    message("Reading command line arguments ..."); // Process command line arguments
    for (int i = 0; i < args.length; i++) {
      options(handler, args[i], loader, false);
    }
    handler.abortOnFailures();

    message("Loading source files ..."); // Load and compile everything
    MILProgram mil = loader.load(handler, milMain);

    message("Running type checker ..."); // Sanity check/dependency analysis
    mil.typeChecking(handler);

    return mil;
  }

  private void process(Handler handler, MILProgram mil) throws Failure {
    MILSpec spec = null;
    RepTypeSet rep = null;
    boolean optimized = false; // Keep track of whether the optimizer has been run
    boolean cfunRewrite = false; // Keep track of whether the cfun rewrite has been run

    if (passes == null) {
      // If no passes are specified, try to set some sensible defaults to satisfy
      // requirements implied by other command line arguments.  This does not prevent
      // errors that can occur with user-specified pass strings (e.g., use of 'r' without
      // a prior 's', or an attempt to generate LLVM code without specialization).

      passes =
          (llvmOutput.isSet() || llvmInterfaceOutput.isSet())
              ? "cosboro"
              : execOutput.isSet()
                  ? "cosboro"
                  : repTypeSetOutput.isSet() ? "cosor" : specTypeSetOutput.isSet() ? "cos" : "co";
      message("Defaulting to passes \"" + passes + "\":");
    }

    for (int i = 0; i < passes.length(); i++) {
      switch (passes.charAt(i)) {
        case 'c': // Constructor function rewrite
          message("Running constructor function rewrite ...");
          mil.cfunRewrite();
          cfunRewrite = true;
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
          mil.bitdataRewrite();
          optimized = false;
          if (spec != null) {
            External.setBitdataRepresentations();
          }
          break;

        case 'r': // Representation transformation
          message("Running representation transformation ...");
          if (!cfunRewrite) {
            throw new Failure(
                "Representation transformation requires an earlier constructor function rewrite");
          } else if (spec == null) {
            throw new Failure(
                "Representation transformation requires an earlier specialization pass");
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

  /** Encapsulates an action to be performed involving writing to a specified PrintWriter. */
  abstract static class Action {

    abstract void run(PrintWriter out) throws Failure;
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

    specTypeSetOutput.run(
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

    repTypeSetOutput.run(
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

    if (typeSetOutput.isSet() || typeDefnsOutput.isSet() || milOutput.isSet()) {
      final TypeSet set = new TypeSet();
      mil.collect(set);
      typeSetOutput.run(
          new Action() {
            void run(PrintWriter out) {
              set.dump(out);
            }
          });
      typeDefnsOutput.run(
          new Action() {
            void run(PrintWriter out) {
              set.dumpTypeDefinitions(out);
            }
          });
      milOutput.run(
          new Action() {
            void run(PrintWriter out) {
              set.dumpTypeDefinitions(out);
              mil.dump(out);
            }
          });
    }

    graphvizOutput.run(
        new Action() {
          void run(PrintWriter out) {
            mil.toDot(out);
          }
        });

    if (llvmOutput.isSet() || llvmInterfaceOutput.isSet()) {
      if (spec == null) {
        throw new Failure("A specialization pass is required for LLVM output");
      } else if (rep == null) {
        throw new Failure("A representation pass is required for LLVM output");
      }
      final llvm.Program llvmProg = mil.toLLVM();
      llvmOutput.run(
          new Action() {
            void run(PrintWriter out) throws Failure {
              llvmProg.dump(out);
            }
          });
      llvmInterfaceOutput.run(
          new Action() {
            void run(PrintWriter out) throws Failure {
              llvmProg.dumpInterface(out);
            }
          });
    }

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
