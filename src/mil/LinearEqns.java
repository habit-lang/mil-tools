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

/**
 * A null-terminated linked list of LinearEqn objects that can be accessed using (public) head and
 * next fields.
 */
public class LinearEqns {

  public LinearEqn head;

  public LinearEqns next;

  /** Default constructor. */
  public LinearEqns(LinearEqn head, LinearEqns next) {
    this.head = head;
    this.next = next;
  }

  /**
   * Solve a set of linear equations, or throw an exception if there are multiple (or no) solutions.
   */
  public static void solve(LinearEqns eqns) throws Failure {
    // The list of equations that we were not able to solve immediately
    LinearEqns deferred = null;
    while (eqns != null) {
      LinearEqn eqn = eqns.head;
      if (eqn.solved()) {
        // If we can solve this equation immediately, then skip it
        eqns = eqns.next;
        for (LinearEqns es = eqns; es != null; es = es.next) {
          // Ensure invariant on es.head still holds following solve of eqn
          es.head.solved();
        }
      } else {
        // Otherwise, defer this equation after eliminating its first
        // variable from all of the remaining equations
        LinearEqns es = eqns.next;
        eqns.next = deferred;
        deferred = eqns;
        for (eqns = es; es != null; es = es.next) {
          es.head.elimVar(eqn);
        }
      }
    }
    for (; deferred != null; deferred = deferred.next) {
      if (!deferred.head.solved()) {
        // Still not solved?  Then we have multiple solutions:
        throw new MultipleSolutionsFailure(deferred.head);
      }
    }
  }
}
