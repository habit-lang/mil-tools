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
import debug.Screen;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import mil.*;

public class LCProgram extends CoreProgram {

  private String name;

  /** Default constructor. */
  public LCProgram(String name) {
    this.name = name;
  }

  /** Return the name for this program. */
  public String getName() {
    return name;
  }

  private TopDefns topDefns = null;

  private TopDefns topDefnsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(TopDefn elem) {
    TopDefns ns = new TopDefns(elem, null);
    topDefnsLast = (topDefnsLast == null) ? (topDefns = ns) : (topDefnsLast.next = ns);
  }

  private LCDefns defns = null;

  private LCDefns defnsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(LCDefn elem) {
    LCDefns ns = new LCDefns(elem, null);
    defnsLast = (defnsLast == null) ? (defns = ns) : (defnsLast.next = ns);
  }

  /**
   * Read the source file for this program and store the corresponding abstract syntax in this
   * object.
   */
  public void syntaxAnalysis(Handler handler, LCLoader loader) throws Failure {
    debug.Log.println("Loading " + name + " ...");
    try {
      Reader reader = new FileReader(loader.findFile(handler, name));
      Source source = new JavaSource(handler, name, reader);
      if (name.endsWith(".llc")) {
        source = new LiterateSource(handler, true, source);
      }
      source = new CacheSource(handler, source);
      LCLexer lexer = new LCLexer(handler, true, source);
      LCParser parser = new LCParser(handler, lexer, loader);
      parser.parse(this);
    } catch (FileNotFoundException e) {
      throw new Failure("Cannot open input file \"" + name + "\"");
    }
    handler.abortOnFailures();
  }

  private Bindings bindings;

  private BindingSCCs sccs;

  void display(Screen s) {
    Bindings.display(s, bindings, sccs);
    // TODO display top level and core definitions too?
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    out.indent(n, sccs, bindings, defns);
  }

  /**
   * Run scope analysis on this program to ensure that every identifier name that is used has a
   * corresponding definition somewhere in the program.
   */
  public void scopeAnalysis(Handler handler, MILEnv milenv) throws Failure {
    // Extract the bindings from the definitions in this program:
    bindings = LCDefns.toBindings(handler, milenv.getTyconEnv(), defns);
    defns = null; // not needed beyond this point

    // Build an environment with entries for a list of top-level bindings:
    Env env = new BindingsEnv(null, bindings);

    // Add environment entries for symbols introduced in top-level definitions:
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.validateTopDefn(handler, milenv);
    }

    // Visit each binding in the list of top-level bindings:
    for (Bindings bs = bindings; bs != null; bs = bs.next) {
      // We can ignore the list of "free variables" that are returned by the following call, which
      // should only contain references to other top-level values defined in earlier binding groups.
      bs.head.inScopeOf(handler, milenv, bindings, env);
    }

    // Scope analysis on expressions in top-level definitions and check that exports and entrypoints
    // are in scope:
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.scopeTopDefn(handler, milenv, env);
    }

    // Check every expression that appears in a core definition:
    super.inScopeOf(handler, milenv, env);

    // Compute the strongly connected components:
    sccs = Bindings.scc(bindings);
    BindingSCC.checkSafeRecursion(handler, sccs);
  }

  public void typeAnalysis(Handler handler) throws Failure {
    BindingSCCs.inferTypes(handler, null, sccs);
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.inferTypes(handler);
    }
    super.inferTypes(handler);
  }

  TopBindings lambdaLift() {
    LiftEnv lenv = new LiftEnv();
    lenv.liftBindings(bindings, DefVar.noVars);
    liftCoreProgram(lenv);
    // TODO: is the following line necessary?
    // sccs = null; // Invalidate sccs now that new bindings have been added:
    liftTopDefns(lenv);
    return lenv.getLifted();
  }

  void liftTopDefns(LiftEnv lenv) {
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.liftTopDefn(lenv);
    }
  }

  /** Compare the name of this object with a given string. */
  public boolean answersTo(String str) {
    return name.equals(str);
  }

  /** Register a dependency indicating that "this" requires "that". */
  public void requires(LCProgram that) {
    this.callees = new LCPrograms(that, this.callees);
    that.callers = new LCPrograms(this, that.callers);
  }

  /** Records the successors/callees of this node. */
  private LCPrograms callees = null;

  /** Records the predecessors/callers of this node. */
  private LCPrograms callers = null;

  /** Update callees/callers information with dependencies. */
  public void calls(LCPrograms xs) {
    for (callees = xs; xs != null; xs = xs.next) {
      xs.head.callers = new LCPrograms(this, xs.head.callers);
    }
  }

  /**
   * Flag to indicate that this node has been visited during the depth-first search of the forward
   * dependency graph.
   */
  private boolean visited = false;

  /** Visit this X during a depth-first search of the forward dependency graph. */
  LCPrograms forwardVisit(LCPrograms result) {
    if (!this.visited) {
      this.visited = true;
      return new LCPrograms(this, LCPrograms.searchForward(this.callees, result));
    }
    return result;
  }

  /**
   * Records the binding scc in which this binding has been placed. This field is initialized to
   * null but is set to the appropriate binding scc during dependency analysis.
   */
  private LCProgramSCC scc = null;

  /** Return the binding scc that contains this binding. */
  public LCProgramSCC getScc() {
    return scc;
  }

  /**
   * Visit this binding during a depth-first search of the reverse dependency graph. The scc
   * parameter is the binding scc in which all unvisited bindings that we find should be placed.
   */
  void reverseVisit(LCProgramSCC scc) {
    if (this.scc == null) {
      // If we arrive at a binding that hasn't been allocated to any SCC,
      // then we should put it in this SCC.
      this.scc = scc;
      scc.add(this);
      for (LCPrograms callers = this.callers; callers != null; callers = callers.next) {
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
      LCProgramSCC.addDependency(this.scc, scc);
    }
  }

  /** Run static analysis on this LC program. */
  MILEnv staticAnalysis(Handler handler, MILEnv milenv) throws Failure {
    // Validate type definitions, recording the results in a corresponding type environment:
    TyconEnv tenv = this.typeEnv(handler, milenv.getTyconEnv());

    // Build a new MILEnv for this program:
    milenv = this.newmil(handler, tenv, milenv);

    // Run scope analysis on the LC program:
    this.scopeAnalysis(handler, milenv);
    handler.abortOnFailures();

    // Run type analysis on the LC program:
    this.typeAnalysis(handler);
    handler.abortOnFailures();

    return milenv;
  }

  /**
   * Compile an LC program into MIL. The resulting code will require a subsequent call to addArgs(),
   * but that can wait until all files have been loaded.
   */
  void compile(MILProgram mil, MILEnv milenv) {
    TopBindings tbs = lambdaLift(); // Lift out definitions of recursive functions
    addExports(mil, milenv); // Process exports and entry points
    for (; tbs != null; tbs = tbs.next) { // Generate MIL code from LC
      tbs.head.compile();
    }
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.compileTopDefn();
    }
  }

  void addExports(MILProgram mil, MILEnv milenv) {
    for (TopDefns tds = topDefns; tds != null; tds = tds.next) {
      tds.head.addExports(mil, milenv);
    }
  }
}
