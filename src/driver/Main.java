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
package driver;

import compiler.*;
import java.io.*;
import lc.*;
import mil.*;

class Main {

  /** A command line entry point. */
  public static void main(String[] args) {
    new Main().run(args);
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
    System.err.println("         -e[filename]   list external generators");
    System.err.println("         -h[filename]   list primitives (but -help prints usage message)");
    System.err.println("         -l[filename]   LLVM code (requires s)");
    System.err.println("         -f[filename]   LLVM interface (requires s)");
    System.err.println("         -G[filename]   CFGs GraphViz output (requires s)");
    System.err.println("         -b[filename]   bytecode text");
    System.err.println("         -x[filename]   execute bytecode");
    System.err.println("         --mil-main=N   Set name of main function in MIL input");
    System.err.println("         --llvm-main=N  Set name of main function in LLVM output");
    System.err.println("         --standalone   Equivalent to --mil-main=main --llvm-main=main");
    System.err.println("         --32 / --64    Set wordsize to 32 / 64 bits");
    System.err.println("         --target=T     Set LLVM target triple to T");
    System.err.println("         --help         Display this message");
  }

  /** Flag to indicate if we should generate messages at each stage. */
  private boolean trace = false;

  private void message(String msg) {
    if (trace) {
      System.out.println(msg);
    }
  }

  /** Represents a stream of command line arguments. */
  abstract static class ArgStream {

    /** Advance to the next argument in this argument stream, returning null at the end. */
    abstract String nextArg();
  }

  /** An argument stream from a list of strings. */
  static class StringArgStream extends ArgStream {

    private String[] args;

    /** Default constructor. */
    StringArgStream(String[] args) {
      this.args = args;
    }

    private int i = 0;

    /** Advance to the next argument in this argument stream, returning null at the end. */
    String nextArg() {
      return (args == null || i >= args.length) ? null : args[i++];
    }
  }

  /** An argument stream that reads lines from a specified source. */
  static class SourceArgStream extends ArgStream {

    private Source source;

    /** Default constructor. */
    SourceArgStream(Source source) {
      this.source = source;
    }

    /** Advance to the next argument in this argument stream, returning null at the end. */
    String nextArg() {
      if (source == null) {
        return null;
      } else {
        String arg = source.readLine();
        if (arg == null) {
          source.close();
          source = null;
        }
        return arg;
      }
    }
  }

  /** Store the list of passes to run (null ==> run with a sensible default for other options) */
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

  private FilenameOption generatorsOutput = new FilenameOption("list external generators");

  private FilenameOption primitivesOutput = new FilenameOption("list primitives");

  private FilenameOption llvmOutput = new FilenameOption("llvm output");

  private FilenameOption llvmInterfaceOutput = new FilenameOption("llvm interface");

  private FilenameOption cfgsGraphvizOutput = new FilenameOption("CFGs GraphViz output");

  private FilenameOption bytecodeOutput = new FilenameOption("bytecode output");

  private FilenameOption execOutput = new FilenameOption("execution output");

  /** MIL main name option string. */
  private String milMain = "";

  /** Track the number of source files specified on the command line. */
  private int numSourceFiles = 0;

  /** Track the number of output actions executed. */
  private int numActions = 0;

  /** Set the maximum allowed nesting for .milc files. */
  private static final int MAX_NESTING = 5;

  /** Simple command line argument processing. */
  private void processArgs(Handler handler, ArgStream args, LCLoader loader, int nesting)
      throws Failure {
    String str;
    while ((str = args.nextArg()) != null) {
      if (str.startsWith("-")) {
        processOptions(str, args, loader);
      } else if (str.endsWith(".milc")) {
        if (nesting >= MAX_NESTING) {
          throw new Failure(
              "Cannot read options from nested configuration file \""
                  + str
                  + "\" (a maximum of "
                  + MAX_NESTING
                  + " levels is allowed)");
        } else if (!optionsFromFile(handler, str, loader, ++nesting)) {
          throw new Failure("Error reading options from \"" + str + "\"; file not accessible");
        }
      } else { // Treat as a source file name
        if (!loader.loadMIL(str)) { // Try first to load as mil ...
          loader.require(str); // ... but otherwise load as lc
        }
        numSourceFiles++; // Either way, it counts as a source file
      }
    }
  }

  /** Process command line options after an initial "-" character. */
  private void processOptions(String str, ArgStream args, LCLoader loader) throws Failure {
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
    } else if (optMatches("--help", str) || optMatches("-help", str)) {
      usage();
      numActions++;
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
          if (i + 1 < str.length()) {
            loader.extendSearchPath(str, i + 1);
          } else if ((str = args.nextArg()) != null) {
            loader.extendSearchPath(str, 0);
          } else {
            throw new Failure("Missing argument for -i option");
          }
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
        case 'e':
          generatorsOutput.setName(str, i);
          return;
        case 'h':
          primitivesOutput.setName(str, i);
          return;
        case 'l':
          llvmOutput.setName(str, i);
          return;
        case 'f':
          llvmInterfaceOutput.setName(str, i);
          return;
        case 'G':
          cfgsGraphvizOutput.setName(str, i);
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

  private boolean optionsFromFile(Handler handler, String name, LCLoader loader, int nesting)
      throws Failure {
    message("Reading options from " + name + " ...");
    try {
      ArgStream args = new SourceArgStream(new OptionSource(handler, new FileReader(name), name));
      processArgs(handler, args, loader, nesting);
      return true;
    } catch (FileNotFoundException e) {
      return false;
    }
  }

  /**
   * Process command line arguments and load the requested source files, compiling as necessary to
   * produce a single MIL program.
   */
  public void run(String[] args) {
    Handler handler = new SimpleHandler();
    try {
      // TODO: initial messages will not appear so long as trace is initialized to false :-)
      LCLoader loader = new LCLoader();
      if (optionsFromFile(handler, ".milc", loader, 1)) {
        message("Read options from .milc ..."); // Process options in .milc file, if present
      }
      ;
      message("Reading command line arguments ..."); // Process command line arguments
      processArgs(handler, new StringArgStream(args), loader, 0);
      handler.abortOnFailures();

      if (Word.size() == 0) { // Ensure that a word size is set, defaulting to 32 bits
        Word.setSize(32); // TODO: figure out how to set this automatically ...
      }

      if (numSourceFiles > 0) {
        message("Loading source files ..."); // Load and compile everything
        MILProgram mil = loader.load(handler, milMain);

        message("Running type checker ..."); // Sanity check/dependency analysis
        mil.typeChecking(handler);

        process(handler, mil);
      }

      generatorsOutput.run(
          new Action() {
            void run(PrintWriter out) {
              GenImp.dumpGenerators(out);
            }
          });

      primitivesOutput.run(
          new Action() {
            void run(PrintWriter out) {
              Prim.dumpPrimitives(out);
            }
          });

      if (numSourceFiles == 0 & numActions == 0) {
        usage();
      }
    } catch (Failure f) {
      handler.report(f);
      System.exit(-1);
    }
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
          (llvmOutput.isSet() || llvmInterfaceOutput.isSet() || cfgsGraphvizOutput.isSet())
              ? "csosrsos"
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
            GenImp.setBitdataRepresentations();
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
          mil.mergeRewrite();
          mil.shake();
          optimized = false;
          break;

        case 'm': // Merging of DataTypes
          // TODO: This is currently a "secret" option (because it is not documented in the usage
          // message).  Merging is used
          // as part of the representation transformation process above.  Including it as a separate
          // option here allows us to
          // experiment with the feature more generally, but it is not clear how useful this will be
          // in isolation.  If it turns
          // out to be useful, then it should be documented.  If not, then it should be deleted ...
          message("Merging datatypes ...");
          mil.mergeRewrite();
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

    if (llvmOutput.isSet() || llvmInterfaceOutput.isSet() || cfgsGraphvizOutput.isSet()) {
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
      cfgsGraphvizOutput.run(
          new Action() {
            void run(PrintWriter out) throws Failure {
              mil.cfgsToDot(out);
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
        numActions++;
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
