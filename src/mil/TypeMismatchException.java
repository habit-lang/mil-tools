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

public class TypeMismatchException extends UnifyException {

  Type t1;

  Type[] tenv1;

  Type t2;

  Type[] tenv2;

  /** Default constructor. */
  public TypeMismatchException(Type t1, Type[] tenv1, Type t2, Type[] tenv2) {
    this.t1 = t1;
    this.tenv1 = tenv1;
    this.t2 = t2;
    this.tenv2 = tenv2;
  }

  public String describe() {
    return "Type mismatch: cannot unify \""
        + t1.skeleton(tenv1)
        + "\" with \""
        + t2.skeleton(tenv2)
        + "\"";
  }
}
