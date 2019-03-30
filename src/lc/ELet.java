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
package lc;

import compiler.*;
import core.*;
import mil.*;

class ELet extends PosExpr {

  private LCDefns defns;

  private Expr e;

  /** Default constructor. */
  ELet(Position pos, LCDefns defns, Expr e) {
    super(pos);
    this.defns = defns;
    this.e = e;
  }

  private Bindings bindings;

  private BindingSCCs sccs;

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "ELet");
    out.indent(n, sccs, bindings, defns);
    e.indent(out, n + 1);
  }

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { //  let bindings in e
    // Extract the bindings from the definitions in this program:
    bindings = LCDefns.toBindings(handler, milenv.getTyconEnv(), defns);
    defns = null; // not needed beyond this point

    // Extend the environment with entries for everything in bindings:
    env = new BindingsEnv(env, bindings);

    // Find free variables in e, removing any vars defined in bindings.
    DefVars fvs = e.inScopeOf(handler, milenv, env);
    for (Bindings bs = bindings; bs != null; bs = bs.next) {
      fvs = bs.head.remove(fvs);
    }

    // Compute the list of free variables in each binding:
    for (Bindings bs = bindings; bs != null; bs = bs.next) {
      fvs = DefVars.add(bs.head.inScopeOf(handler, milenv, bindings, env), fvs);
    }

    // Compute the strongly connected components:
    sccs = Bindings.scc(bindings);
    BindingSCC.checkSafeRecursion(handler, sccs);

    return fvs;
  }

  /**
   * Search for and report failures for "ambiguous" type variables in the types of identifiers that
   * are part of this AST node. A type variable is considered ambiguous if there is no way to
   * determine how it should be instantiated in any given use. The input argument specifies the list
   * of all generic type variables that appear in the type of the enclosing binding (whose
   * instantiation will be determined by the context in which the bound variable is used) as a
   * prefix of all of the "fixed" type variables (i.e., those that appear free in the environment,
   * and hence cannot be freely instantiated). Any type variables that do not appear in this list
   * may be considered ambiguous. Ambiguity arises in examples like "length Nil" where there is no
   * way to determine the type of the "Nil" list. Occurrences of ambiguous type variables must be
   * fixed by rewriting the code (for this example, "length Nil" is an unnecessarily complicated way
   * of writing "0") or by adding type information.
   */
  void findAmbigTVars(TVars gens) throws Failure { // let bindings in e
    // TODO: do we need to do something with the bindings?
    e.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // let sccs(bindings) in e
    return type = e.inferType(BindingSCCs.inferTypes(null, tis, sccs));
  }

  /** Check that this expression will produce a value of the specified type. */
  void checkType(TVarsInScope tis, Type t) throws Failure { // let sccs(bindings) in e
    e.checkType(BindingSCCs.inferTypes(null, tis, sccs), type = t);
  }

  Expr lift(LiftEnv lenv) { // let bindings in e
    BindingSCCs prev = null;
    for (BindingSCCs bsccs = sccs; bsccs != null; bsccs = bsccs.next) {
      BindingSCC scc = bsccs.head;
      Bindings bs = scc.getBindings();
      if (scc.isRecursive() || Binding.anyQuantified(bs)) {
        // Compute extra variables needed to lift bindings in this scc: Even if there are no extra
        // variables
        // to add (i.e., if xvs is empty), we still need to add a lifting so that the original
        // DefVar for each
        // variable binding is replaced with an appropriate Top value.
        DefVar[] xvs = scc.extraVars(lenv);

        // Rename all of the bindings in this SCC
        Binding.rename(bs);

        // Lift them to the top level
        lenv.addTopBindings(lenv.liftBindings(bs, xvs));

        if (prev == null) { // Unlink the SCC from this ELet
          sccs = bsccs.next;
        } else {
          prev.next = bsccs.next;
        }
      } else { // Keep the local definition in this (non-recursive, monomorphic,
        for (; bs != null; bs = bs.next) { // singleton) binding group, but still lift the rhs
          bs.head.liftBinding(lenv);
        }
        prev = bsccs;
      }
    }
    e = e.lift(lenv);
    return (sccs == null) ? e : this;
  }

  /**
   * Compile an expression into a Tail. The continuation kt maps tails (of the same type as this
   * expression) to code sequences (that return a value of the type specified by kty).
   */
  Code compTail(
      final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // let bindings in e
    return e.compLet(env, sccs, abort, kty, kt);
  }

  /**
   * Compile a monadic expression into a Tail. If this is an expression of type Proc T, then the
   * continuation kt maps tails (that produce values of type T) to code sequences (that return a
   * value of the type specified by kty).
   */
  Code compTailM(
      final CGEnv env, final Block abort, final Type kty, final TailCont kt) { //  let bindings in e
    return e.compLetM(env, sccs, abort, kty, kt);
  }
}
