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
 * Provides a representation for atom environments that associate identifier names (represented by
 * String values) in the abstract syntax for MIL programs with corresponding MIL Temp values.
 */
class TempEnv {

  private String[] ids;

  private Temp[] ds;

  private TempEnv enclosing;

  /** Default constructor. */
  TempEnv(String[] ids, Temp[] ds, TempEnv enclosing) {
    this.ids = ids;
    this.ds = ds;
    this.enclosing = enclosing;
  }

  /**
   * Find the Temp value corresponding to the specified identifier in the given environment,
   * returning null if there is no appropriate binding.
   */
  static Temp find(String id, TempEnv tenv) {
    for (; tenv != null; tenv = tenv.enclosing) {
      String[] ids = tenv.ids;
      for (int i = 0; i < ids.length; i++) {
        if (ids[i].equals(id)) {
          return tenv.ds[i];
        }
      }
    }
    return null;
  }
}
