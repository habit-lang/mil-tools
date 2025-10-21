/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

/** TConst is used to represent a constant type such as a type constructor or a literal. */
public abstract class TConst extends Type {

  /**
   * Find the list of unbound type variables in this type, with a given environment, thisenv, for
   * interpreting TGen values, and accumulating the results in tvs.
   */
  TVars tvars(Type[] thisenv, TVars tvs) {
    return tvs;
  }

  /**
   * Calculate a type skeleton for this type, replacing occurrences of any of the TVar objects in
   * generics with a TGen value corresponding to its index. Any other unbound TVars are kept as is.
   * All TInd and bound TVar nodes are eliminated in the process.
   */
  Type skeleton(Type[] thisenv, TVar[] generics) {
    return this;
  }

  boolean contains(Type[] thisenv, TVar v) {
    return false;
  }

  Type apply(Type[] thisenv, TVarSubst s) {
    return this;
  }
}
