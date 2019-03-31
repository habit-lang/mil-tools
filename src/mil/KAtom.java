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
import java.util.HashMap;

public class KAtom extends Kind {

  private String label;

  /** Default constructor. */
  private KAtom(String label) {
    this.label = label;

    if (table == null) {
      // We run in to subtle bugs if we attempt to initialize table as part
      // of the static declarations above, so we defer to runtime instead:
      table = new HashMap<String, Kind>();
    }
    table.put(label, this);
  }

  public static final Kind STAR = new KAtom("*");

  public static final Kind NAT = new KAtom("nat");

  public static final Kind LAB = new KAtom("lab");

  public static final Kind AREA = new KAtom("area");

  public static final Kind TUPLE = new KAtom("tuple");

  static HashMap<String, Kind> table;

  static {
    table.put("type", STAR);
  }

  public static Kind byName(String label) {
    return table.get(label);
  }

  /**
   * Construct a printable representation of a kind, adding parentheses around a function kind if
   * the boolean argument is true.
   */
  public String toString(boolean needParens) {
    return label;
  }

  /** Test to see if this kind includes any occurrences of the given (unbound) kind variable. */
  boolean contains(KVar v) {
    return false;
  }

  /** Copy a kind, replacing any unbound kind variables with star. */
  Kind fixKind() {
    return this;
  }

  /**
   * Attempt to unify two kinds, returning a boolean to indicate if the operation was successful.
   */
  public boolean unify(Kind that) {
    return that.unifyKAtom(this);
  }

  /** Attempt to unify this kind with a known atomic kind ka. */
  boolean unifyKAtom(KAtom ka) {
    return this == ka;
  }

  /** Attempt to unify this kind with a known function kind, kf. */
  boolean unifyKFun(KFun kf) {
    return false;
  }
}
