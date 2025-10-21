/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

public class TyvarEnv {

  private Tyvars tyvars = null;

  private Tyvars tyvarsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(Tyvar elem) {
    Tyvars ns = new Tyvars(elem, null);
    tyvarsLast = (tyvarsLast == null) ? (tyvars = ns) : (tyvarsLast.next = ns);
  }

  public Tyvar find(String id) {
    return Tyvars.find(id, tyvars);
  }

  public void checkForMultipleTyvars(Handler handler) {
    Tyvars.checkForMultiple(handler, tyvars);
  }

  /**
   * Calculate a Prefix corresponding to the type variables that are defined in this environment.
   */
  public Prefix toPrefix() {
    Prefix prefix = new Prefix();
    for (Tyvars vs = tyvars; vs != null; vs = vs.next) {
      vs.head.fixKinds();
      prefix.add(vs.head);
    }
    return prefix;
  }

  /** Fix the kinds for all of the type variables in this environment. */
  public void fixKinds() {
    for (Tyvars vs = tyvars; vs != null; vs = vs.next) {
      vs.head.fixKinds();
    }
  }

  /**
   * Generate a kind for a type constructor that uses the type variables in this environment as
   * parameters.
   */
  public Kind toKind() {
    return toKind(tyvars);
  }

  /** Recursive worker function for toKind(). */
  private static Kind toKind(Tyvars tyvars) {
    return (tyvars == null) ? KAtom.STAR : new KFun(tyvars.head.getKind(), toKind(tyvars.next));
  }
}
