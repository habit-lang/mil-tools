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

public class MILLoader extends core.Loader {

  /** Records the list of objects that have already been loaded. */
  private MILASTs loaded = null;

  /** Records the list of objects that are required but not yet loaded. */
  private MILASTs required = null;

  /**
   * Require that a specific file be loaded, returning a corresponding AST object that either
   * contains a previously loaded AST for that file, or else has been added to the list of required
   * files for loading at a later stage.
   */
  public MILAST require(String name) {
    MILAST ast;
    if ((ast = findIn(name, loaded)) == null
        && // if this file hasn't already been loaded and
        (ast = findIn(name, required)) == null) { //              hasn't already been required
      ast = new MILAST(name); // then add a new requirement
      required = new MILASTs(ast, required);
    }
    return ast;
  }

  /** Worker method for require, used to search a list for an AST with a specific name. */
  private MILAST findIn(String name, MILASTs list) {
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
  public MILASTs syntaxAnalysis(Handler handler) throws Failure {
    while (required != null) {
      // Prepare to load the next file on the required list:
      MILAST loading = required.head;
      required = required.next;
      loaded = new MILASTs(loading, loaded);

      // Load the abstract syntax from the associated source file:
      loading.syntaxAnalysis(handler, this);
    }
    return loaded;
  }

  /** Load the abstract syntax for all of the required files. */
  public MILEnv load(Handler handler, MILProgram program) throws Failure {
    MILASTSCCs sccs = MILASTs.scc(syntaxAnalysis(handler));

    // Run through strongly connected components to build up a MIL environment for the complete
    // program.
    MILEnv milenv = Builtin.obj;
    MILProgram full = new MILProgram();
    for (; sccs != null; sccs = sccs.next) {
      MILASTSCC scc = sccs.head;
      // TODO: At some point, we may be able to allow for mutual recursion between MIL files,
      // loading
      // all of them together as a group.  But that still wouldn't provide any good reason for a MIL
      // file to require itself, so perhaps some of this error checking code would still be useful
      // then ...
      if (scc.isRecursive()) {
        // TODO: add functionality to construct this list in a common class and reuse for mil & lc
        StringBuilder buf = new StringBuilder("Recursive requirements for ");
        for (MILASTs asts = scc.getBindings(); asts != null; asts = asts.next) {
          buf.append(asts.head.getName());
          if (asts.next != null) {
            buf.append(", ");
          }
        }
        handler.report(new Failure(buf.toString()));
      } else {
        // There must be exactly one (non-recursive) AST in this SCC:
        MILAST ast = scc.getBindings().head;
        debug.Log.println("Checking " + ast.getName() + " ...");

        // Validate the type definitions in the program, recording the results in a corresponding
        // type environment:
        TyconEnv tenv = ast.typeEnv(handler, milenv.getTyconEnv());

        // Run scope analysis:
        milenv = ast.scopeAnalysis(handler, tenv, milenv, program);
        ast.addDefnsTo(full);
      }
    }

    // Perform type checking on full program:
    debug.Log.println("Type checking ...");
    full.typeChecking(handler);

    return milenv;
  }
}
