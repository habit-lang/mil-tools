/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import core.*;
import java.io.PrintWriter;

/**
 * Describes a strategy for providing the implementation of an external. The base class provides a
 * default that maps externals either to primitive functions or to global variables, as determined
 * by the type of the external.
 */
public class ExtImp {

  /** Find the list of Defns that this Defn depends on. */
  Defns dependencies() {
    return null;
  }

  /**
   * Output a description of this implementation strategy, using the provided TypeWriter to format
   * any embedded types.
   */
  void dump(PrintWriter out, StringTypeWriter tw) {
    /* nothing to do */
  }

  /** Type check the implementation for an external. */
  void checkImp(Handler handler, Position pos) {
    /* nothing to do */
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    /* nothing to do */
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    /* nothing to do */
  }

  /** Generate a specialized version of this external implementation strategy. */
  ExtImp specialize(MILSpec spec, Type[] tenv) {
    return this;
  }

  /**
   * Attempt to generate an implementation, post representation transformation, for an external
   * primitive.
   */
  TopDefn repImplement(Handler handler, External ext, Type[] reps, RepTypeSet set) throws Failure {
    return ext.generatePrim(reps);
  }

  /**
   * Make a new top level definition for a given external with associated representation vector and
   * tail.
   */
  protected static TopDefn repImplement(Position pos, External ext, Type[] reps, Tail t) {
    TopLhs[] lhs; // Generate a suitable lhs
    String id = ext.getId();
    if (reps == null) { // No change in representation
      lhs = new TopLhs[] {new TopLhs(id)}; // ==> single left hand side
      lhs[0].setDeclared(ext.getDeclared());
    } else { // Change in representation
      lhs = new TopLhs[reps.length]; // ==> may require multiple lhss
      for (int i = 0; i < reps.length; i++) {
        lhs[i] = new TopLhs(Defn.mkid(id, i));
        lhs[i].setDeclared(reps[i]);
      }
    }
    // Make new top level to use as the replacement for ext
    TopDefn impl = new TopLevel(pos, lhs, t);
    impl.setIsEntrypoint(ext.isEntrypoint());
    debug.Log.println("Generated new top level definition for " + impl);
    return impl;
  }
}
