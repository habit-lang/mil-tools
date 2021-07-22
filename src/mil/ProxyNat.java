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
import java.math.BigInteger;

public class ProxyNat extends Proxy {

  private BigInteger nat;

  /** Default constructor. */
  public ProxyNat(BigInteger nat) {
    this.nat = nat;
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return "!" + nat;
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameProxyNat(this);
  }

  /** Test to determine whether this Atom refers to the given ProxyNat constant. */
  public boolean sameProxyNat(ProxyNat c) {
    return this.nat.equals(c.nat);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return Type.nat(new TNat(nat));
  }
}
