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
import mil.*;

/** Represents a let-bound variable, with an associated binding, id = e. */
class Binding extends DefVar {

  private Position pos;

  private String id;

  private Expr e;

  /** Default constructor. */
  Binding(Position pos, String id, Expr e) {
    this.pos = pos;
    this.id = id;
    this.e = e;
  }

  public Position getPos() {
    return pos;
  }

  /**
   * Records the declared type signature for this binding. A null value indicates that there was no
   * declared type signature.
   */
  private Scheme declared;

  /** Lists the generic type variables for this binding. */
  protected TVar[] generics;

  /** Find an identifier associated with this variable. */
  public String getId() {
    return id;
  }

  /** Determine whether this variable can be referenced by the specified identifier. */
  public boolean answersTo(String id) {
    return id.equals(this.id);
  }

  /** Return the most general type available for this variable. */
  public Scheme getScheme() {
    return (declared != null) ? declared : type;
  }

  /** Records the successors/callees of this node. */
  private Bindings callees = null;

  /** Records the predecessors/callers of this node. */
  private Bindings callers = null;

  /** Update callees/callers information with dependencies. */
  public void calls(Bindings xs) {
    for (callees = xs; xs != null; xs = xs.next) {
      xs.head.callers = new Bindings(this, xs.head.callers);
    }
  }

  /**
   * Flag to indicate that this node has been visited during the depth-first search of the forward
   * dependency graph.
   */
  private boolean visited = false;

  /** Visit this X during a depth-first search of the forward dependency graph. */
  Bindings forwardVisit(Bindings result) {
    if (!this.visited) {
      this.visited = true;
      return new Bindings(this, Bindings.searchForward(this.callees, result));
    }
    return result;
  }

  /**
   * Records the binding scc in which this binding has been placed. This field is initialized to
   * null but is set to the appropriate binding scc during dependency analysis.
   */
  private BindingSCC scc = null;

  /** Return the binding scc that contains this binding. */
  public BindingSCC getScc() {
    return scc;
  }

  /**
   * Visit this binding during a depth-first search of the reverse dependency graph. The scc
   * parameter is the binding scc in which all unvisited bindings that we find should be placed.
   */
  void reverseVisit(BindingSCC scc) {
    if (this.scc == null) {
      // If we arrive at a binding that hasn't been allocated to any SCC,
      // then we should put it in this SCC.
      this.scc = scc;
      scc.add(this);
      for (Bindings callers = this.callers; callers != null; callers = callers.next) {
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
      BindingSCC.addDependency(this.scc, scc);
    }
  }

  /**
   * Set the declared type for this binding, or report an error if there are multiple type
   * declarations.
   */
  void attachDeclaredType(Handler handler, Position pos, Scheme declared) {
    // TODO: could allow multiple type annotations if the type schemes are equal/equivalent
    // (although
    // determining the latter could be difficult).
    if (this.declared != null) {
      handler.report(new MultipleSignaturesFailure(pos, id));
    } else {
      this.declared = declared;
    }
  }

  void display(Screen s) {
    if (declared != null) {
      int ind = s.getIndent();
      s.print(id);
      s.print(" :: ");
      s.print(declared.toString());
      s.println();
      s.indent(ind);
    }
    s.print(id);
    s.print(" = ");
    e.display(s);
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    if (type == null) {
      out.indent(n, id + " = ");
    } else {
      StringBuilder buf = new StringBuilder();
      buf.append("(");
      buf.append(id);
      buf.append(" :: ");
      buf.append(type.skeleton().toString());
      buf.append(") =");
      if (generics != null && generics.length > 0) {
        buf.append(" /\\");
        for (int i = 0; i < generics.length; i++) {
          buf.append(" ");
          buf.append(generics[i].skeleton().toString());
        }
        buf.append(" ->");
      }
      out.indent(n, buf.toString());
    }
    e.indent(out, n + 1);
  }

  /**
   * Find the Binding corresponding to a particular Var in a given list of Bindings, or return null
   * if no such Binding can be found.
   */
  Binding find(Bindings bs) {
    return Bindings.isIn(this, bs) ? this : null;
  }

  /**
   * Records the extra variables (i.e., variables defined within the same set of bindings) that are
   * referenced by this binding.
   */
  private DefVars xvs;

  /**
   * Perform scope analysis on a binding, keeping track of call dependencies for items in the same
   * list of bindings. The returned list of free vars should only include variables that are defined
   * in an enclosing scope.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Bindings bindings, Env env) {
    DefVars fvs = e.inScopeOf(handler, milenv, env); // Find the free variables in the expression
    DefVars xvs = null; // List of vars required in body
    Bindings cs = null; // Find the callees that are listed in bindings
    DefVars prev = null; // Previous entry
    for (DefVars us = fvs; us != null; us = us.next) {
      Binding b = us.head.find(bindings);
      if (b == null) { // Variable not defined in bindings
        prev = us; // - skip, keeping it in fvs
      } else { // Variable defined in bindings
        cs = new Bindings(b, cs); // - add it to the callees
        if (prev == null) { // - remove it from fvs
          fvs = us.next;
        } else {
          prev.next = us.next;
        }
      }
      xvs = us.head.add(xvs);
    }
    this.calls(cs); // Register dependencies on this binding
    this.xvs = xvs; // Compute free variables
    return fvs;
  }

  /** Test to determine whether this binding can be used in a recursive binding group. */
  public DefVars checkSafeToRecurse(Handler handler, DefVars vs) { // id = e
    if (!e.isSafeToRecurse()) {
      handler.report(new InvalidRecursiveDefinition(pos, id));
    }
    return DefVars.add(xvs, vs);
  }

  /**
   * Extend the given list of unbound type variables, tvs, from the enclosing scope with any
   * additional unbound type variables that appear in this specific object.
   */
  public TVars tvarsInScope(TVars tvs) {
    return getScheme().tvars(tvs);
  }

  /**
   * Set this variable's type to be a fresh type variable. Used to initialize the type field of a
   * Var.
   */
  public Type freshType(Tyvar tyvar) {
    debug.Internal.error("should not call freshType for a Binding");
    return null;
  }

  /** Return a type for an instantiated version of this variable. */
  Type instantiate() {
    return (declared != null) ? declared.instantiate() : type;
  }

  /** Set the initial type for this binding. */
  void setInitialType() {
    // use an instance of declared type, if provided, otherwise, use a fresh type variable
    type = (declared != null) ? declared.instantiate() : new TVar(Tyvar.star);
  }

  /**
   * Type check this binding. Note that we allow for the possibility that the handler is null,
   * indicating that any errors that are detected should be thrown rather than reported.
   */
  void checkType(Handler handler, TVarsInScope tis) throws Failure {
    try {
      // Ensure that the type of the right hand expression matches the initial type assumption
      // for this variable.
      e.checkType(tis, type);
    } catch (Failure f) {
      // If there is a declared type and a non null handler, then we can recover from a type error
      // that is detected while checking this binding by using the declared type scheme (and
      // reporting the error to the handler).  But otherwise, we throw a failure.
      if (declared != null && handler != null) {
        handler.report(f);
        type = null; // set to null to signal that a type error occurred
      } else {
        throw f;
      }
    }
  }

  /**
   * Calculate a generalized type for this binding, adding a universal quantifier for any unbound
   * type variable in the inferred type that is not included in the list of fixed (i.e., non
   * generic) type variables.
   */
  void generalizeType(TVars fixed) throws Failure {
    // If an error was detected while checking this binding, then the type of this binding was set
    // to
    // null and there is nothing more to do.  But otherwise, proceed to calculate the most general
    // type.
    if (type != null) {
      // Calculate the generic variables for this binding:
      TVars gens = type.tvars(fixed);
      generics = TVar.generics(gens, fixed);

      // Calculate the inferred type for this binding:
      Scheme inferred = type.generalize(generics);
      debug.Log.println("Inferred " + id + " :: " + inferred);

      // Compare inferred and declared types with one another:
      if (declared == null) {
        declared = inferred;
      } else if (!declared.alphaEquiv(inferred)) {
        throw new Failure(
            pos,
            "Declared type \""
                + declared
                + "\" for \""
                + id
                + "\" is more general than inferred type \""
                + inferred
                + "\"");
      }

      // Search for ambiguous type variables:
      e.findAmbigTVars(gens);
    }
  }

  /**
   * Find a lifting for this variable (which will definitely be null if this variable is not a
   * Binding).
   */
  Lifting findLifting(LiftEnv lenv) {
    return lenv.findLifting(this);
  }

  TopBinding liftToTop(DefVar[] xvs, LiftEnv lenv) {
    // Create a new top-level MIL definition for this binding; the tail will be filled in by code
    // generation.
    TopLevel topLevel = new TopLevel(pos, id, null); // Create new top-level definition
    topLevel.setDeclared(0, declared);

    // Add an entry for this binding to the lifting environment and return a new top binding.
    lenv.addLifting(this, topLevel, xvs); // Add lifting v -> topLevel xvs
    return new TopBinding(topLevel, e, type, generics); // Create new top level binding
  }

  void liftBinding(LiftEnv lenv) {
    e = e.lift(lenv);
  }

  /** Test to see if any of the bindings in this list have polymorphic types. */
  static boolean anyQuantified(Bindings bs) {
    for (; bs != null; bs = bs.next) {
      if (bs.head.declared.isMonomorphic() == null) {
        return true;
      }
    }
    return false;
  }

  private static int count = 0;

  /** Pick new names for each of the bindings in this list (for debugging purposes only). */
  static void rename(Bindings bs) {
    for (; bs != null; bs = bs.next) {
      bs.head.id = bs.head.id + count++;
    }
  }

  /**
   * Generate code for a local binding that evaluates the associated expression, assigns the result
   * to the specified variable, and then continues with the remaining code. We can assume here that
   * the binding is not recursive because all recursive definitions should have been lifted out
   * earlier by the lambda lifter.
   */
  Code compBinding(final CGEnv env, final Temp t1, final Code code) {
    return e.compTail(
        env,
        MILProgram.abort,
        new TailCont() {
          Code with(Tail t) {
            return new Bind(t1, t, code);
          }
        });
  }
}
