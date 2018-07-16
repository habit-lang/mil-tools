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
import compiler.Failure;
import compiler.Position;
import core.*;
import obdd.Pat;

public class Synonym extends Tycon {

  private Type expansion;

  /** Default constructor. */
  public Synonym(Position pos, String id, Kind kind, Type expansion) {
    super(pos, id, kind);
    this.expansion = expansion;
  }

  public Type getExpansion() {
    return expansion;
  }

  public void setExpansion(Type expansion) {
    this.expansion = expansion;
  }

  public int level = 0;

  public int getLevel() {
    return level;
  }

  public Synonym isSynonym() {
    return this;
  }

  public int findLevel() throws Failure {
    if (level < 0) {
      throw new Failure(pos, "Recursion in synonym for \"" + id + "\"");
    } else if (level == 0 && expansion != null) {
      level = (-1); // mark as visiting
      level = 1 + expansion.findLevel();
    }
    return level;
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    return expansion.simplifyNatType(null);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return expansion.bitSize(tenv, a);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type bitSize(Type[] tenv, Type a, Type b) {
    return expansion.bitSize(tenv, a, b);
  }

  /** Return the nat that specifies the bit size of the type produced by this type constructor. */
  public Type bitSize() {
    return expansion.bitSize(null);
  }

  /** Return the bit pattern for the values of this type. */
  public Pat bitPat() {
    return expansion.bitPat(null);
  }

  Pat bitPat(Type[] tenv, Type a) {
    return expansion.bitPat(tenv, a);
  }

  Pat bitPat(Type[] tenv, Type a, Type b) {
    return expansion.bitPat(tenv, a, b);
  }

  /** Return the nat that specifies the byte size of the type produced by this type constructor. */
  public Type byteSize() {
    return expansion.byteSize(null);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return expansion.byteSize(tenv, a);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return expansion.byteSize(tenv, a, b);
  }

  Type byteSizeStoredRef(Type[] tenv) {
    return expansion.byteSizeStoredRef(null);
  }

  Type byteSizeStoredRef(Type[] tenv, Type a) {
    return expansion.byteSizeStoredRef(tenv, a);
  }

  Type byteSizeStoredRef(Type[] tenv, Type a, Type b) {
    return expansion.byteSizeStoredRef(tenv, a, b);
  }

  Type[] repCalc() {
    return expansion.repCalc();
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return expansion.bitdataTyconRep(a);
  }

  /**
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return expansion.bitdataTyconRep2(a, b);
  }
}
