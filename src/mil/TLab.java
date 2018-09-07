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

public class TLab extends TLit {

  private String str;

  /** Default constructor. */
  public TLab(String str) {
    this.str = str;
  }

  /** Return the string associated with this type if it is a label type, or else return null. */
  public String getLabel() {
    return str;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal, possibly with some correspondence between the TGen objects in
   * the two types. We use the names left and right to keep track of which types were on the left
   * and the right in the original alphaEquiv() call so that we can build the TGenCorresp in a
   * consistent manner.
   */
  boolean alphaType(Type left, TGenCorresp corresp) {
    return left.alphaTLab(this);
  }

  /** Test to determine whether this type is equal to a given TLab. */
  boolean alphaTLab(TLab right) {
    return this.str.equals(right.str);
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    tw.write("\"");
    tw.write(str); // TODO: handle escape codes
    tw.write("\"");
  }

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    return t.alphaTLab(this);
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return KAtom.LAB;
  }

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    if (!t.alphaTLab(this)) {
      throw new TypeMismatchException(t, null, this, thisenv);
    }
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return set.canonLit(str, this, args);
  }
}
