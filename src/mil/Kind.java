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

public abstract class Kind {

  /** Construct a printable representation of a kind. */
  public String toString() {
    return toString(false);
  }

  /**
   * Construct a printable representation of a kind, adding parentheses around a function kind if
   * the boolean argument is true.
   */
  public abstract String toString(boolean needParens);

  public boolean same(Kind that) {
    return that == this;
  }

  private static Kind[] simpleCache;

  public static Kind simple(int n) {
    if (simpleCache == null) {
      simpleCache = new Kind[10];
    } else if (n >= simpleCache.length) {
      Kind[] newCache = new Kind[Math.max(n + 1, 2 * simpleCache.length)];
      for (int i = 0; i < simpleCache.length; i++) {
        newCache[i] = simpleCache[i];
      }
      simpleCache = newCache;
    } else if (simpleCache[n] != null) {
      return simpleCache[n];
    }
    // Code to update cache[arg] = ... will be appended here.

    return fillCache(simpleCache, n, KAtom.STAR);
  }

  private static Kind[] tupleCache;

  public static Kind tuple(int n) {
    if (tupleCache == null) {
      tupleCache = new Kind[10];
    } else if (n >= tupleCache.length) {
      Kind[] newCache = new Kind[Math.max(n + 1, 2 * tupleCache.length)];
      for (int i = 0; i < tupleCache.length; i++) {
        newCache[i] = tupleCache[i];
      }
      tupleCache = newCache;
    } else if (tupleCache[n] != null) {
      return tupleCache[n];
    }
    // Code to update cache[arg] = ... will be appended here.

    return fillCache(tupleCache, n, KAtom.TUPLE);
  }

  private static Kind fillCache(Kind[] cache, int n, Kind base) {
    int last = n; // Compute new entries
    while (last > 0 && cache[--last] == null) { // Find last cached kind
      /* do nothing */
    }
    if (last == 0) {
      cache[0] = base;
    }
    for (; last < n; ++last) { // Fill in blanks
      cache[last + 1] = new KFun(KAtom.STAR, cache[last]);
    }
    return cache[n]; // Return result
  }

  /** Test to see if this kind includes any occurrences of the given (unbound) kind variable. */
  abstract boolean contains(KVar v);

  /** Copy a kind, replacing any unbound kind variables with star. */
  abstract Kind fixKind();

  /**
   * Attempt to unify two kinds, returning a boolean to indicate if the operation was successful.
   */
  public abstract boolean unify(Kind that);

  /** Attempt to unify this kind with a known atomic kind ka. */
  abstract boolean unifyKAtom(KAtom ka);

  /** Attempt to unify this kind with a known function kind, kf. */
  abstract boolean unifyKFun(KFun kf);

  /**
   * Attempt to unify this kind with a known kind variable, kv. We assume that kv has bound==null.
   */
  boolean unifyKVar(KVar kv) {
    return kv.bindTo(this);
  }

  Kind getRng() {
    return null;
  }

  Type makeHead(Position pos, PrintWriter out, int i, Type h) {
    return h;
  }
}
