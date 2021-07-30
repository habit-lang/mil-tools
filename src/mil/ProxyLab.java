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

public class ProxyLab extends Proxy {

  private String lab;

  /** Default constructor. */
  public ProxyLab(String lab) {
    this.lab = lab;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return "!" + lab.toString();
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  boolean sameAtom(Atom that) {
    return that.sameProxyLab(this);
  }

  /** Test to determine whether this Atom refers to the given ProxyLab constant. */
  boolean sameProxyLab(ProxyLab c) {
    return this.lab.equals(c.lab);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Type.lab(new TLab(lab));
  }
}
