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
package mil;

import compiler.*;
import core.*;
import java.io.PrintWriter;

/**
 * Requests that the implementation of this external, post representation analysis, be provided by a
 * specified atom.
 */
public class AtomImp extends ExtImp {

  private Atom a;

  /** Default constructor. */
  public AtomImp(Atom a) {
    this.a = a;
  }

  /** Find the list of Defns that this Defn depends on. */
  Defns dependencies() {
    return a.dependencies(null);
  }

  void dump(PrintWriter out, StringTypeWriter tw) {
    out.print(" = " + a);
  }

  void checkImp(Handler handler, Position pos) {
    a.checkImp(handler, pos);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    a.collect(set);
  }

  /** Generate a specialized version of this external implementation strategy. */
  ExtImp specialize(MILSpec spec, Type[] tenv) {
    return new AtomImp(a.specializeAtom(spec, null, null));
  }

  /**
   * Attempt to generate an implementation, post representation transformation, for an external
   * primitive.
   */
  TopDefn repImplement(Handler handler, External ext, Type[] reps, RepTypeSet set) throws Failure {
    return repImplement(ext.getPos(), ext, reps, new Return(a));
  }
}
