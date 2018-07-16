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
import core.*;

public class TVarSubst {

  private TVar[] tvs;

  private Type[] type;

  /** Default constructor. */
  public TVarSubst(TVar[] tvs, Type[] type) {
    this.tvs = tvs;
    this.type = type;
  }

  public Type find(TVar tv) {
    for (int i = 0; i < tvs.length; i++) {
      if (tvs[i] == tv) {
        return type[i];
      }
    }
    debug.Internal.error("Unable to find TVarSubst entry for " + tv.skeleton());
    return null;
  }

  public String toString() {
    StringBuilder buf = new StringBuilder("[");
    for (int i = 0; i < tvs.length; i++) {
      if (0 < i) {
        buf.append(", ");
      }
      buf.append(tvs[i].toString());
      buf.append(" --> ");
      buf.append(type[i].skeleton().toString());
    }
    buf.append("]");
    return buf.toString();
  }

  static TVarSubst make(TVar[] generics, Type[] tenv) {
    for (int i = 0; i < tenv.length; i++) {
      tenv[i] = tenv[i].removeTVar(); // TODO: This is a big hack ... :-)
    }
    return new TVarSubst(generics, tenv);
  }
}
