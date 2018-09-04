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
import compiler.Position;
import core.*;
import java.io.PrintWriter;

public class KFun extends Kind {

  private Kind dom;

  private Kind rng;

  /** Default constructor. */
  public KFun(Kind dom, Kind rng) {
    this.dom = dom;
    this.rng = rng;
  }

  /**
   * Construct a printable representation of a kind, adding parentheses around a function kind if
   * the boolean argument is true.
   */
  public String toString(boolean needParens) {
    String s = dom.toString(true) + " -> " + rng.toString(false);
    return needParens ? ("(" + s + ")") : s;
  }

  public boolean same(Kind that) {
    if (that instanceof KFun) {
      KFun kf = (KFun) that;
      if (dom.same(kf.dom)) {
        // TODO: the following code rewrites the graph to reduce duplication of kind
        // data structures; is it worth the trouble?
        dom = kf.dom;
        if (rng.same(kf.rng)) {
          rng = kf.rng; //
          return true;
        }
      }
    }
    return false;
  }

  /** Test to see if this kind includes any occurrences of the given (unbound) kind variable. */
  boolean contains(KVar v) {
    return dom.contains(v) || rng.contains(v);
  }

  /** Copy a kind, replacing any unbound kind variables with star. */
  Kind fixKind() {
    this.dom = dom.fixKind();
    this.rng = rng.fixKind();
    return this;
  }

  /**
   * Attempt to unify two kinds, returning a boolean to indicate if the operation was successful.
   */
  public boolean unify(Kind that) {
    return that.unifyKFun(this);
  }

  /** Attempt to unify this kind with a known atomic kind ka. */
  boolean unifyKAtom(KAtom ka) {
    return false;
  }

  /** Attempt to unify this kind with a known function kind, kf. */
  boolean unifyKFun(KFun kf) {
    return kf.dom.unify(dom) && kf.rng.unify(rng);
  }

  Kind getRng() {
    return rng;
  }

  Type makeHead(Position pos, PrintWriter out, int i, Type h) {
    int suffix = (i / 26);
    String id = ((char) ('a' + (i % 26))) + ((suffix > 0) ? Integer.toString(suffix) : "");
    out.print(" (");
    out.print(id);
    out.print(" :: ");
    out.print(dom.toString());
    out.print(")");
    return rng.makeHead(pos, out, i + 1, new TAp(h, new TTycon(new PrimTycon(pos, id, dom, 0))));
  }
}
