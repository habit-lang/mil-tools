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

/**
 * Represents environments mapping Temps in original programs to corresponding specialized versions.
 */
class SpecEnv {

  private Temp[] vs;

  private Temp[] svs;

  private SpecEnv next;

  /** Default constructor. */
  SpecEnv(Temp[] vs, Temp[] svs, SpecEnv next) {
    this.vs = vs;
    this.svs = svs;
    this.next = next;
  }

  /** Search for an occurrence of a given variable v in a SpecEnv. */
  static Temp find(Temp v, SpecEnv env) {
    for (; env != null; env = env.next) {
      for (int i = 0; i < env.vs.length; i++) {
        if (env.vs[i] == v) {
          return env.svs[i];
        }
      }
    }
    debug.Internal.error("mismatched Temp in SpecEnv.find");
    return null; // not reached
  }
}
