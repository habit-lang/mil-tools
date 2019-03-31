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

/** KVar is used to represent kind variables. */
public class KVar extends Kind {

  /** Records the value of this variable (initially null, indicating an unbound variable). */
  private Kind bound = null;

  /**
   * Construct a printable representation of this kind variable. For simplicity, all unbound kind
   * variables are displayed as an underscore, even if they are distinct. TODO: It would be less
   * confusing to use different printed representations for distinct kind variables.
   */
  public String toString(boolean needParens) {
    return bound == null ? "_" : bound.toString(needParens);
  }

  /** Bind this kind variable to a specific kind, performing an occurs check to ensure validity. */
  boolean bindTo(Kind k) {
    if (this == k) { // Trivially unify a variable with itself
      return true;
    } else if (k.contains(this)) { // Run occurs check
      return false;
    }
    bound = k;
    return true;
  }

  /** Test to see if this kind includes any occurrences of the given (unbound) kind variable. */
  boolean contains(KVar v) {
    return bound == null ? (this == v) : bound.contains(v);
  }

  /** Copy a kind, replacing any unbound kind variables with star. */
  Kind fixKind() {
    return bound == null ? KAtom.STAR : bound.fixKind();
  }

  /**
   * Attempt to unify two kinds, returning a boolean to indicate if the operation was successful.
   */
  public boolean unify(Kind that) {
    return bound == null ? that.unifyKVar(this) : bound.unify(that);
  }

  /** Attempt to unify this kind with a known atomic kind ka. */
  boolean unifyKAtom(KAtom ka) {
    return bound == null ? bindTo(ka) : bound.unifyKAtom(ka);
  }

  /** Attempt to unify this kind with a known function kind, kf. */
  boolean unifyKFun(KFun kf) {
    return bound == null ? bindTo(kf) : bound.unifyKFun(kf);
  }

  /**
   * Attempt to unify this kind with a known kind variable, kv. We assume that kv has bound==null.
   */
  boolean unifyKVar(KVar kv) {
    return bound == null ? bindTo(kv) : bound.unifyKVar(kv);
  }
}
