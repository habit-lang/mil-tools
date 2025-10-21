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

public class TyconEnv {

  private TyconEnv enclosing;

  /** Default constructor. */
  public TyconEnv(TyconEnv enclosing) {
    this.enclosing = enclosing;
  }

  private Tycons tycons = null;

  private Tycons tyconsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(Tycon elem) {
    Tycons ns = new Tycons(elem, null);
    tyconsLast = (tyconsLast == null) ? (tycons = ns) : (tyconsLast.next = ns);
  }

  /** Display a TyconEnv (for debugging purposes). TODO: should we keep this? */
  public static void dump(String msg, TyconEnv tenv) {
    System.out.print(msg);
    System.out.print(": { ");
    if (tenv != null) {
      do {
        for (Tycons ts = tenv.tycons; ts != null; ts = ts.next) {
          System.out.print(ts.head.getId());
          System.out.print(" ");
        }
        if ((tenv = tenv.enclosing) == null) {
          break;
        } else {
          System.out.print("| ");
        }
      } while (tenv != null);
    }
    System.out.println("}");
  }

  /** Search an environment for a particular name (if it exists). */
  public static Tycon findTycon(String id, TyconEnv env) {
    return findTycon(id, env, null);
  }

  static Tycon findTycon(String id, TyconEnv env, TyconEnv enclosing) {
    Tycon name = null;
    while (env != enclosing && (name = env.findTyconInThis(id)) == null) {
      env = env.enclosing;
    }
    return name;
  }

  public Tycon findTyconInThis(String id) {
    return Tycons.find(id, tycons);
  }

  public void checkForMultipleTycons(Handler handler) {
    Tycons.checkForMultiple(handler, tycons);

    for (Tycons ts = tycons; ts != null; ts = ts.next) {
      Tycon t = findTycon(ts.head.getId(), enclosing);
      if (t != null) {
        handler.report(new PrevTyconDefnFailure(ts.head.getPos(), ts.head, t));
      }
    }
  }

  /** A placeholder for defining an environment of built-in type constructors and constants. */
  public static final TyconEnv builtin = new TyconEnv(null);
}
