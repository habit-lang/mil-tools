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
package core;

import compiler.*;
import mil.*;

class NotInScopeTyvarFailure extends Failure {

  NotInScopeTyvarFailure(Position pos, String id) {
    super(pos, "The type variable \"" + id + "\" is not in scope");
  }

  /**
   * Report an out of scope error for any type variable that was added to this environment after the
   * snapshot of its length was captured (by using a call to getLen()).
   */
  public static void beyond(Handler handler, TyvarEnv params, int len) {
    for (Tyvars ts = params.beyond(len); ts != null; ts = ts.next) {
      Tyvar tyvar = ts.head;
      handler.report(new core.NotInScopeTyvarFailure(tyvar.getPos(), tyvar.getId()));
    }
  }
}
