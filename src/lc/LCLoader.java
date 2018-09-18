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

public class LCLoader extends core.Loader {

  /** Records the list of objects that have already been loaded. */
  private LCPrograms loaded = null;

  /** Records the list of objects that are required but not yet loaded. */
  private LCPrograms required = null;

  /**
   * Require that a specific file be loaded, returning a corresponding AST object that either
   * contains a previously loaded AST for that file, or else has been added to the list of required
   * files for loading at a later stage.
   */
  public LCProgram require(String name) {
    LCProgram ast;
    if ((ast = findIn(name, loaded)) == null
        && // if this file hasn't already been loaded and
        (ast = findIn(name, required)) == null) { //              hasn't already been required
      ast = new LCProgram(name); // then add a new requirement
      required = new LCPrograms(ast, required);
    }
    return ast;
  }

  /** Worker method for require, used to search a list for an AST with a specific name. */
  private LCProgram findIn(String name, LCPrograms list) {
    for (; list != null; list = list.next) {
      if (list.head.answersTo(name)) {
        return list.head;
      }
    }
    return null;
  }

  /**
   * Load all required files, recognizing that more files might become required in the process, and
   * returning a list of all loaded files.
   */
  public LCPrograms syntaxAnalysis(Handler handler) throws Failure {
    while (required != null) {
      // Prepare to load the next file on the required list:
      LCProgram loading = required.head;
      required = required.next;
      loaded = new LCPrograms(loading, loaded);

      // Load the abstract syntax from the associated source file:
      loading.syntaxAnalysis(handler, this);
    }
    return loaded;
  }

  /** Include a MILLoader for any .mil files that this program requires. */
  private MILLoader milLoader = new MILLoader();

  /** Require that a specific MIL file is loaded. */
  void requireMIL(String name) {
    milLoader.require(name);
  }

  /**
   * If the given name ends with ".mil" or ".lmil", then add it as a MIL requirement for this loader
   * and return true. Otherwise return false, indicating that further action is required to load it
   * as lc code.
   */
  public boolean loadMIL(String name) {
    if (name.endsWith(".mil") || name.endsWith(".lmil")) {
      requireMIL(name);
      return true;
    }
    return false;
  }

  /** Set the search path for this LCLoader as well as the underlying MILLoader. */
  public void setSearchPath(String[] searchPath) {
    super.setSearchPath(searchPath);
    milLoader.setSearchPath(searchPath);
  }

  /**
   * Load all of the files that have been requested from this loader, including any transitive
   * dependencies.
   */
  public MILProgram load(Handler handler, String mainName) throws Failure {
    // Load all of the required LCProgram objects:
    LCProgramSCCs sccs = LCPrograms.scc(syntaxAnalysis(handler));

    // Load all of the required MIL files:
    MILProgram mil = new MILProgram(); // Construct an empty MIL program
    MILEnv milenv = milLoader.load(handler, mil);

    // Load all of the specified LC files:
    for (; sccs != null; sccs = sccs.next) {
      LCProgramSCC scc = sccs.head;
      if (scc.isRecursive()) {
        // TODO: add functionality to construct this list in a common class and reuse for mil & lc
        StringBuilder buf = new StringBuilder("Recursive requirements for ");
        for (LCPrograms asts = scc.getBindings(); asts != null; asts = asts.next) {
          buf.append(asts.head.getName());
          if (asts.next != null) {
            buf.append(", ");
          }
        }
        handler.report(new Failure(buf.toString()));
      } else {
        // There must be exactly one (non-recursive) AST in this SCC:
        LCProgram ast = scc.getBindings().head;
        debug.Log.println("Compiling " + ast.getName() + " ...");
        milenv = ast.staticAnalysis(handler, milenv);
        ast.compile(mil, milenv);
      }
    }
    if (!mainName.equals("")) {
      Top main = milenv.findTop(mainName);
      if (main == null) {
        handler.report(new Failure("Program does not contain a definition for " + mainName));
      } else {
        mil.setMain(main.getDefn());
      }
    } else if (mil.isEmpty()) {
      handler.report(
          new Failure("No entrypoints or main function have been specified for this program"));
    }
    mil.addArgs(); // Add arguments to blocks and closures
    handler.abortOnFailures();
    return mil;
  }
}
