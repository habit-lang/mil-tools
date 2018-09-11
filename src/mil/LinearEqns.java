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
import compiler.Failure;
import core.*;

/**
 * A null-terminated linked list of items that can be accessed using (public) head and next fields.
 */
public class LinearEqns {

  public LinearEqn head;

  public LinearEqns next;

  /** Default constructor. */
  public LinearEqns(LinearEqn head, LinearEqns next) {
    this.head = head;
    this.next = next;
  }

  public static void display(LinearEqns eqns) {
    for (; eqns != null; eqns = eqns.next) {
      System.out.println("   " + eqns.head);
    }
  }

  public static void solve(LinearEqns eqns) throws Failure {
    LinearEqns deferred = null; // The list of equations that we were not able to solve immediately
    while (eqns != null) {
      LinearEqn eqn = eqns.head;
      if (eqn.solved()) { // If we can solve this equation immediately, then skip it
        eqns = eqns.next;
      } else { // Otherwise, defer this equation after eliminating its first variable
        LinearEqns es = eqns.next; // from all of the remaining equations
        eqns.next = deferred;
        deferred = eqns;
        for (eqns = es; es != null; es = es.next) {
          es.head.elimVar(eqn);
        }
      }
    }
    for (; deferred != null; deferred = deferred.next) {
      if (!deferred.head.solved()) { // Still not solved?  Then we have multiple solutions:
        throw new MultipleSolutionsFailure(deferred.head);
      }
    }
  }
}
