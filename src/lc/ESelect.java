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

class ESelect extends PosExpr {

  Expr e;

  String lab;

  /** Default constructor. */
  ESelect(Position pos, Expr e, String lab) {
    super(pos);
    this.e = e;
    this.lab = lab;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) { // e . lab
    indent(out, n, "ESelect: " + lab);
    e.indent(out, n + 1);
  }

  private Cfun cf;

  private Cfun lcf;

  private int index;

  private StructField sf;

  /**
   * Perform scope analysis on this expression, creating a Temp for each variable binding, checking
   * that all of the identifiers it references correspond to bound variables, and returning the set
   * of free variables in the term.
   */
  DefVars inScopeOf(Handler handler, MILEnv milenv, Env env) { // e . lab
    return e.inScopeOf(handler, milenv, env);
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
  void findAmbigTVars(TVars gens) throws Failure { // e . lab
    e.findAmbigTVars(gens);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // e . lab
    Type et = e.inferType(tis).skeleton(); // take skeleton to shortcut TInds, etc.
    BitdataType bt = et.bitdataType();
    BitdataLayout layout;
    if (bt == null) {
      layout = et.bitdataLayout();
      if (layout == null) {
        TVar tv = new TVar(Tyvar.area); // Is e a reference to a structure?
        if (Type.ref(tv).match(null, et, null)) {
          StructType st = tv.skeleton().structType();
          if (st != null) {
            StructField[] fields = st.getFields();
            index = Name.index(lab, fields);
            if (index >= 0) {
              sf = fields[index];
              return type = Type.ref(sf.getType());
            }
          }
        }
        throw new Failure(
            pos, "Invalid selector for field \"" + lab + "\" from value of type " + et);
      }
      cf = null; // et is a layout type, so no outer constructor is involved
    } else {
      BitdataLayout[] layouts = bt.getLayouts();
      if (layouts.length != 1) {
        throw new Failure(pos, "Invalid selector: " + et + " has multiple constructors");
      }
      layout = layouts[0];
      cf = bt.getCfuns()[0]; // record outer constructor
    }
    BitdataField[] lfields = layout.getFields();
    index = Name.index(lab, lfields);
    if (index < 0) {
      throw new Failure(pos, "There is no \"" + lab + "\" field for type " + et);
    }
    lcf = layout.getCfun();
    return type = lfields[index].getType();
  }

  Expr lift(LiftEnv lenv) { // e . lab
    e = e.lift(lenv);
    return this;
  }

  /**
   * Compile an expression into a Tail. The continuation kt maps tails (of the same type as this
   * expression) to code sequences (that return a value of the type specified by kty).
   */
  Code compTail(final CGEnv env, final Block abort, final Type kty, final TailCont kt) { // e . lab
    return e.compAtom(
        env,
        kty,
        new AtomCont() {
          Code with(final Atom a) {
            if (sf != null) { // structure field selection
              return kt.with(sf.getSelectPrim().withArgs(a));
            } else if (cf == null) { // operating directly on layout
              return kt.with(new Sel(lcf, index, a));
            } else {
              Temp t = new Temp(lcf.getDataName().asType());
              return new Bind(t, new Sel(cf, 0, a), kt.with(new Sel(lcf, index, t)));
            }
          }
        });
  }

  /**
   * Compile a monadic expression into a Tail. If this is an expression of type Proc T, then the
   * continuation kt maps tails (that produce values of type T) to code sequences (that return a
   * value of the type specified by kty).
   */
  Code compTailM(
      final CGEnv env,
      final Block abort,
      final Type kty,
      final TailCont kt) { // id [ fields ], e [ fields ],  e . lab
    debug.Internal.error("Constructs of this form do not produce values of monadic type");
    return null;
  }
}
