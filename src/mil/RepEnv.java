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

class RepEnv {

  private Temp v;

  private Temp[] vs;

  private RepEnv next;

  /** Default constructor. */
  RepEnv(Temp v, Temp[] vs, RepEnv next) {
    this.v = v;
    this.vs = vs;
    this.next = next;
  }

  static Temp[] find(Temp v, RepEnv env) {
    for (; env != null; env = env.next) {
      if (v == env.v) {
        return env.vs;
      }
    }
    debug.Internal.error("RepEnv lookup failed for " + v);
    return null; // not reached
  }

  static void display(RepEnv env) { // For debugging ...
    System.out.print("Env{");
    for (; env != null; env = env.next) {
      System.out.print(" " + env.v + " --> " + Atom.toString(env.vs));
    }
    System.out.println("}");
  }
}
