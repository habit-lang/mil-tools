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
import java.math.BigInteger;
import mil.*;

class EBit extends ELit {

  private BigInteger nat;

  private int width;

  /** Default constructor. */
  EBit(Position pos, BigInteger nat, int width) {
    super(pos);
    this.nat = nat;
    this.width = width;
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    indent(out, n, "EBit: " + nat + "/" + width);
  }

  /**
   * Infer a type for this expression, using information in the tis parameter to track the set of
   * type variables that appear in an assumption.
   */
  Type inferType(TVarsInScope tis) throws Failure { // nat (of width w)
    // TODO: check that width is within allowed range ...
    return type = Type.bit(width);
  }

  /**
   * Compile an expression into an Atom. The continuation ka expects an Atom (of the same type as
   * this expression) and produces a code sequence (that returns a value of the type kty).
   */
  Code compAtom(final CGEnv env, final Type kty, final AtomCont ka) {
    return ka.with(new Bits(nat, width));
  }
}
