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

public class TyvarEnv {

  /** Points to the first item in the list (null if the list is empty). */
  private Tyvars tyvars = null;

  /** Points to the last item in the list (null if the list is empty). */
  private Tyvars tyvarsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(Tyvar elem) {
    Tyvars ns = new Tyvars(elem, null);
    tyvarsLast = (tyvarsLast == null) ? (tyvars = ns) : (tyvarsLast.next = ns);
    len++;
  }

  /** Records the number of entries in this TyvarEnv. */
  private int len = 0;

  /** Return the number of entries in this TyvarEnv. */
  public int getLen() {
    return len;
  }

  public Tyvar find(String id) {
    return Tyvars.find(id, tyvars);
  }

  public void checkForMultipleTyvars(Handler handler) {
    Tyvars.checkForMultiple(handler, tyvars);
  }

  /** Calculate a signature for the type variables in this environment. */
  public Tyvar[] toSig() {
    Tyvar[] sig = new Tyvar[len];
    Tyvars vs = tyvars;
    for (int i = 0; vs != null; vs = vs.next) {
      sig[i++] = vs.head;
    }
    return sig;
  }

  /**
   * Calculate a Prefix corresponding to the type variables that are defined in this environment.
   */
  public Prefix toPrefix() {
    defaultKinds();
    return new Prefix(toSig());
  }

  /** Fix the kinds for all of the type variables in this environment. */
  public void defaultKinds() {
    for (Tyvars vs = tyvars; vs != null; vs = vs.next) {
      vs.head.defaultKinds();
    }
  }

  /**
   * Return (a copy of) the list of type variables that have been added to this environment since
   * the snapshot of its length was captured using getLen(). The use of copying, as well as the
   * final reverse, are likely unnecessary in practice but avoids leaking the internal structure of
   * the Tyvars list in this environment.
   */
  public Tyvars beyond(int len) {
    Tyvars vs = null;
    // Have entries been added since the count len was taken?
    if (this.len > len) {
      // If so, skip the old type variable entries ...
      Tyvars ts = tyvars;
      for (int i = 0; i < len; i++) {
        ts = ts.next;
      }
      // ... and add any additional variables to the output list
      for (; ts != null; ts = ts.next) {
        vs = new Tyvars(ts.head, vs);
      }
    }
    // reverse the result to preserve the original order
    return Tyvars.reverse(vs);
  }

  /**
   * Construct a DataType object with parameters corresponding to this environment. We
   * assume/require that this method is called at the point where params contains only entries for
   * the parameters, with no duplicates and no added variables that are functionaly determined by
   * the parameters. In particular, this means that we can use the length, len, to determine the
   * correct arity.
   */
  public DataType makeDataType(Position pos, String id) {
    return new DataType(pos, id, toKind(this.tyvars), len);
  }

  /** Recursive worker function for makeDataType(). */
  private static Kind toKind(Tyvars tyvars) {
    return (tyvars == null) ? KAtom.STAR : new KFun(tyvars.head.getKind(), toKind(tyvars.next));
  }
}
