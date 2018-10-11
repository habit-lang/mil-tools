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

public abstract class TLit extends TConst {

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return t.sameTLit(tenv, this);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    return that.sameTLit(null, this);
  }

  /**
   * Matching of types: test to see if the type on the right can be obtained by instantiating type
   * variables in the type on the left. (The "receiver", or "this", in the following code.)
   *
   * <p>match :: Type -> Env -> Type -> Env -> IO ()
   *
   * <p>Note that it is possible for a partial match to occur, meaning that some of the variables in
   * the receiver might be bound during the matching process, even if match returns false.
   */
  public boolean match(Type[] thisenv, Type t, Type[] tenv) {
    return t.sameTLit(tenv, this);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    t.unifyTLit(tenv, this);
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    that.unifyTLit(null, this);
  }

  /**
   * Return the natural number type that specifies the BitSize of this type (required to be of kind
   * *) or null if this type has no BitSize (i.e., no bit-level representation). This method should
   * only be used with a limited collection of classes (we only expect to use it with top-level,
   * monomorphic types), but, just in case, we also provide implementations for classes that we do
   * not expect to see in practice, and allow for the possibility of a type environment, even though
   * we expect it will only ever be null.
   */
  public Type bitSize(Type[] tenv) {
    debug.Internal.error("No BitSize for type literals (kind error)");
    return null;
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    debug.Internal.error("No ByteSize for type literals (kind error)");
    return null;
  }

  /** Return the alignment of this type (or zero if there is no alignment). */
  public long alignment(Type[] tenv) {
    debug.Internal.error("No alignment for type literals (kind error)");
    return 0;
  }
}
