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

public class NewtypeTypeSet extends TypeSet {

  /**
   * Override the TypeSet method for calculating canonical versions of a type with a Tycon at its
   * head. By overriding this method, we are able to check for situations where the head is a
   * newtype constructor and, in those cases, replace the type with its expansion instead.
   */
  protected Type canon(Tycon h, int args) {
    Type t = h.translate(this, args); // Look for a translation of this type
    return (t == null) ? super.canon(h, args) : t; // Or fall back to the canonical representation
  }
}
