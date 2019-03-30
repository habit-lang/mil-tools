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
package lc;

import compiler.*;
import core.*;
import mil.*;

/**
 * Represents a continuation that takes a Tail that will produce the final result of a previous
 * calculation and returns a code sequence that uses the Tail to complete a computation.
 */
abstract class TailCont {

  /** Invoke this TailCont with the given Tail to be embedded in a complete code sequence. */
  abstract Code with(final Tail t);

  /** A TailCont that wraps the incoming Tail in a simple Done. */
  public static final TailCont done =
      new TailCont() {
        Code with(final Tail t) {
          return new Done(t);
        }
      };
}
