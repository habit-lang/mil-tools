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

public class KindMismatchException extends UnifyException {

  Kind k;

  Type t;

  Type[] tenv;

  /** Default constructor. */
  public KindMismatchException(Kind k, Type t, Type[] tenv) {
    this.k = k;
    this.t = t;
    this.tenv = tenv;
  }

  public String describe() {
    return "Type \""
        + t.skeleton(tenv)
        + "\" has kind \""
        + t.calcKind(tenv)
        + "\" where a type of kind \""
        + k
        + "\" is required";
  }
}
