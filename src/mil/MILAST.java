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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

class MILAST extends CoreProgram {

  private String name;

  /** Default constructor. */
  MILAST(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  private DefnExps milDefns = null;

  private DefnExps milDefnsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(DefnExp elem) {
    DefnExps ns = new DefnExps(elem, null);
    milDefnsLast = (milDefnsLast == null) ? (milDefns = ns) : (milDefnsLast.next = ns);
  }

  /**
   * Load the abstract syntax for a list of MIL definitions from the source file for this MILAST
   * object.
   */
  public void syntaxAnalysis(Handler handler, MILLoader loader) throws Failure {
    debug.Log.println("Loading " + name + " ...");
    try {
      Reader reader = new FileReader(loader.findFile(handler, name));
      Source source = new JavaSource(handler, name, reader);
      if (name.endsWith(".lmil")) {
        source = new LiterateSource(handler, true, source);
      }
      source = new CacheSource(handler, source); // Add a caching layer
      MILLexer lexer = new MILLexer(handler, true, source);
      MILParser parser = new MILParser(handler, lexer, loader);
      parser.parse(this);
    } catch (FileNotFoundException e) {
      handler.report(new Failure("Cannot open input file \"" + name + "\""));
    }
    handler.abortOnFailures();
  }

  /** Use scope analysis to convert a sequence of MIL definitions to corresponding MIL code. */
  public MILEnv scopeAnalysis(Handler handler, TyconEnv tenv, MILEnv imports, MILProgram program)
      throws Failure {
    // Pass 1: Build internal environment that contains bindings for all symbols defined in this
    // MILAST:
    MILEnv external = newmil(handler, tenv, imports); // Build two external environment layers
    MILEnv internal = new MILEnvChain(tenv, external); // internal is used only inside this MILAST
    for (DefnExps ds = milDefns; ds != null; ds = ds.next) {
      ds.head.addTo(handler, internal);
    }
    handler.abortOnFailures();

    // Pass 2: Use the internal environment to match all identifier uses with corresponding
    // definitions:
    for (DefnExps ds = milDefns; ds != null; ds = ds.next) {
      ds.head.inScopeOf(handler, internal);
    }
    handler.abortOnFailures();

    // Pass 3: Add bindings for exported symbols in to the external environment, and return that as
    // final result:
    for (DefnExps ds = milDefns; ds != null; ds = ds.next) {
      ds.head.addExports(external, program);
    }
    handler.abortOnFailures();

    return external;
  }

  /** Compare the name of this object with a given string. */
  public boolean answersTo(String str) {
    return name.equals(str);
  }

  /** Register a dependency indicating that "this" requires "that". */
  public void requires(MILAST that) {
    this.callees = new MILASTs(that, this.callees);
    that.callers = new MILASTs(this, that.callers);
  }

  /** Records the successors/callees of this node. */
  private MILASTs callees = null;

  /** Records the predecessors/callers of this node. */
  private MILASTs callers = null;

  /** Update callees/callers information with dependencies. */
  public void calls(MILASTs xs) {
    for (callees = xs; xs != null; xs = xs.next) {
      xs.head.callers = new MILASTs(this, xs.head.callers);
    }
  }

  /**
   * Flag to indicate that this node has been visited during the depth-first search of the forward
   * dependency graph.
   */
  private boolean visited = false;

  /** Visit this X during a depth-first search of the forward dependency graph. */
  MILASTs forwardVisit(MILASTs result) {
    if (!this.visited) {
      this.visited = true;
      return new MILASTs(this, MILASTs.searchForward(this.callees, result));
    }
    return result;
  }

  /**
   * Records the binding scc in which this binding has been placed. This field is initialized to
   * null but is set to the appropriate binding scc during dependency analysis.
   */
  private MILASTSCC scc = null;

  /** Return the binding scc that contains this binding. */
  public MILASTSCC getScc() {
    return scc;
  }

  /**
   * Visit this binding during a depth-first search of the reverse dependency graph. The scc
   * parameter is the binding scc in which all unvisited bindings that we find should be placed.
   */
  void reverseVisit(MILASTSCC scc) {
    if (this.scc == null) {
      // If we arrive at a binding that hasn't been allocated to any SCC,
      // then we should put it in this SCC.
      this.scc = scc;
      scc.add(this);
      for (MILASTs callers = this.callers; callers != null; callers = callers.next) {
        callers.head.reverseVisit(scc);
      }
    } else if (this.scc == scc) {
      // If we arrive at a binding that has the same binding scc
      // as the one we're building, then we know that it is recursive.
      scc.setRecursive();
      return;
    } else {
      // The only remaining possibility is that we've strayed outside
      // the binding scc we're building to a scc that *depends on*
      // the one we're building.  In other words, we've found a binding
      // scc dependency from this.scc to scc.
      MILASTSCC.addDependency(this.scc, scc);
    }
  }

  /** Add all of the definitions in this MILAST as entry points in the given MIL program. */
  void addDefnsTo(MILProgram mil) {
    for (DefnExps ds = milDefns; ds != null; ds = ds.next) {
      ds.head.addAsEntryTo(mil);
    }
  }
}
