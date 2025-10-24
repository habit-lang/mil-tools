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
package core;

import compiler.*;
import mil.*;

class TypeOps {

  private TypeOps typeOps;

  private TypeExp t;

  private TypeExp op;

  /** Default constructor. */
  TypeOps(TypeOps typeOps, TypeExp t, TypeExp op) {
    this.typeOps = typeOps;
    this.t = t;
    this.op = op;
  }

  /** Records the fixity associated with the operator field in this Ops value. */
  private Fixity fixity;

  /**
   * Use fixity information to rearrange the components of a sequence of operands separated by
   * operators into an expression. Uses a simple shift/reduce technique, and assumes that fixities
   * have already been assigned to each of the operators in the Ops list.
   */
  TypeExp tidyInfix(TypeExp t) throws Failure {
    TypeOps left = this;
    TypeOps right = null;
    do {
      // invariant: at least one of left, right is not null
      if (left != null && (right == null || left.before(right))) {
        TypeOps temp = left.typeOps; // Shift! (move left on to right stack)
        left.typeOps = right;
        right = left;
        left = temp;
      } else {
        TypeOps temp = right.typeOps; // Reduce! (apply right operator)
        if (temp == null) {
          t = right.apply(t);
        } else {
          temp.t = right.apply(temp.t);
        }
        right = temp;
      }
    } while (left != null || right != null);
    return t;
  }

  /** Helper method to determine whether this operation should be done before that. */
  private boolean before(TypeOps that) throws Failure {
    switch (this.fixity.which(that.fixity)) {
      case Fixity.LEFT:
        return true;
      case Fixity.NONASS:
        throw new TypeExpFixityParseFailure(this.op, that.op);
      default:
        return false;
    }
  }

  /** Find a position for this type expression. */
  public Position position() {
    return (typeOps == null) ? t.position() : typeOps.position();
  }

  /** Visit each subexpression in this list and attach fixity information to each operator. */
  void tidyInfix(TyconEnv env) throws Failure {
    for (TypeOps ts = this; ts != null; ts = ts.typeOps) {
      ts.t = ts.t.tidyInfix(env);
      ts.op = ts.op.tidyInfix(env);
      ts.fixity = ts.op.getFixity();
    }
  }

  /**
   * Helper method to construct the application of the operator and type in a typeOps object to a
   * given type.
   */
  private TypeExp apply(TypeExp t) {
    TypeExp r = new ApTypeExp(this.op, this.t);
    return (t == null) ? r : new ApTypeExp(r, t);
  }
}
