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

public class TTycon extends TConst {

  private Tycon name;

  /** Default constructor. */
  public TTycon(Tycon name) {
    this.name = name;
  }

  /**
   * Test to determine whether this type is alpha equivalent to another type, by checking to see if
   * the two type skeletons are equal, possibly with some correspondence between the TGen objects in
   * the two types. We use the names left and right to keep track of which types were on the left
   * and the right in the original alphaEquiv() call so that we can build the TGenCorresp in a
   * consistent manner.
   */
  boolean alphaType(Type left, TGenCorresp corresp) {
    return left.alphaTTycon(this);
  }

  /** Test to determine whether this type is equal to a given TTycon. */
  boolean alphaTTycon(TTycon right) {
    return this.name == right.name;
  }

  /**
   * Write this type to the specified writer, in a context with the specified precedence and number
   * of arguments.
   */
  void write(TypeWriter tw, int prec, int args) {
    name.write(tw, prec, args);
  }

  public int findLevel() throws Failure {
    return name.findLevel();
  }

  /**
   * Test to determine whether two types are equal.
   *
   * <p>same :: Type -> Env -> Type -> Env -> Bool
   */
  public boolean same(Type[] thisenv, Type t, Type[] tenv) {
    return t.sameTTycon(tenv, this);
  }

  /** Test to determine whether this type is equal to a specified type application. */
  boolean sameTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    Synonym s = name.isSynonym();
    return (s != null) && s.getExpansion().sameTAp(null, tap, tapenv);
  }

  /** Test to determine whether this type is equal to a specified type constant. */
  boolean sameTTycon(Type[] thisenv, TTycon that) {
    if (this.name == that.name) {
      return true;
    }
    Synonym sthis = this.name.isSynonym();
    Synonym sthat = that.name.isSynonym();
    if (sthis == null) {
      if (sthat == null) {
        return false; // neither this or that is a synonym
      }
    } else {
      if (sthat != null) {
        // this and that are both synonyms
        int levelthis = sthis.getLevel();
        int levelthat = sthat.getLevel();
        if (levelthis > levelthat) {
          sthat = null; // don't expand that because it has a lower level
        } else if (levelthis < levelthat) {
          sthis = null; // don't expand this because it has a lower level
        } else {
          // levels are equal, so expand both sides and repeat equality test
          return sthat.getExpansion().same(null, sthis.getExpansion(), null);
        }
      }
    }

    // exactly one of this or that is a synonym:
    return (sthis != null)
        ? sthis.getExpansion().sameTTycon(null, that)
        : sthat.getExpansion().same(null, this, null);
  }

  /** Test to determine whether this type is equal to a specified type literal. */
  boolean sameTLit(Type[] thisenv, TLit t) {
    Synonym s = name.isSynonym();
    return (s != null) && s.getExpansion().sameTLit(null, t);
  }

  /**
   * Return the kind of this type. We assume here that the type is already known to be kind correct,
   * so the intent here is just to return the kind of the type as quickly as possible (i.e., with
   * minimal traversal of the type data structure), and not to (re)check that the type is kind
   * correct.
   */
  Kind calcKind(Type[] thisenv) {
    return name.getKind();
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
    return t.sameTTycon(tenv, this);
  }

  /**
   * Test to determine whether the specified type application will match this type. For this method,
   * we should only instantiate type variables that appear in the type application, tap.
   */
  boolean matchTAp(Type[] thisenv, TAp tap, Type[] tapenv) {
    Synonym s = name.isSynonym();
    return (s != null) && s.getExpansion().matchTAp(null, tap, tapenv);
  }

  /**
   * Unification of types.
   *
   * <p>unify :: Type -> Env -> Type -> Env -> IO ()
   */
  public void unify(Type[] thisenv, Type t, Type[] tenv) throws UnifyException {
    t.unifyTTycon(tenv, this);
  }

  void unifyTAp(Type[] thisenv, TAp tap, Type[] tapenv) throws UnifyException {
    Synonym s = name.isSynonym();
    if (s != null) {
      s.getExpansion().unifyTAp(null, tap, tapenv);
    } else {
      super.unifyTAp(thisenv, tap, tapenv); // trigger error
    }
  }

  void unifyTTycon(Type[] thisenv, TTycon that) throws UnifyException {
    if (this.name != that.name) {
      Synonym sthis = this.name.isSynonym();
      Synonym sthat = that.name.isSynonym();
      if (sthis == null) {
        if (sthat == null) { // Distinct, no expansion ==> error
          throw new TypeMismatchException(that, null, this, thisenv);
        }
      } else {
        if (sthat != null) { // this and that are both synonyms
          int levelthis = sthis.getLevel(); // find their levels
          int levelthat = sthat.getLevel();
          if (levelthis > levelthat) { // don't expand (lower level) that
            sthat = null;
          } else if (levelthis < levelthat) { // don't expand (lower level) this
            sthis = null;
          } else { // equal levels: expand both, try again
            // levels are equal, so expand both sides and repeat equality test
            sthat.getExpansion().unify(null, sthis.getExpansion(), null);
          }
        }
      }

      // exactly one of this or that is a synonym:
      if (sthis != null) { // expand this synonym and unify
        sthis.getExpansion().unifyTTycon(null, that);
      } else { // expand that synonym and unify
        sthat.getExpansion().unify(null, this, null);
      }
    }
  }

  void unifyTLit(Type[] thisenv, TLit t) throws UnifyException {
    Synonym s = name.isSynonym();
    if (s != null) {
      s.getExpansion().unifyTLit(null, t);
    } else {
      super.unifyTLit(thisenv, t);
    }
  }

  /**
   * Simplify this natural number type, using the specified type environment if needed, returning
   * either an unbound TVar, or else a TNat literal. TODO: This could be used more generally as a
   * way to eliminate all TGen, TInd, bound TVar, or Synonym nodes at the root of any type, not just
   * natural number types ... Suggest rewriting description and renaming method to reflect that ...
   * (and testing too ...)
   */
  public Type simplifyNatType(Type[] tenv) {
    Type t = name.simplifyNatType(null);
    return (t != null) ? t : this;
  }

  /** Find the name of the associated bitdata type, if any. */
  public BitdataName bitdataName() {
    return name.bitdataName();
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
    return name.bitSize();
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return name.bitSize(tenv, a);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type bitSize(Type[] tenv, Type a, Type b) {
    return name.bitSize(tenv, a, b);
  }

  public Pat bitPat(Type[] tenv) {
    return name.bitPat();
  }

  Pat bitPat(Type[] tenv, Type a) {
    return name.bitPat(tenv, a);
  }

  Pat bitPat(Type[] tenv, Type a, Type b) {
    return name.bitPat(tenv, a, b);
  }

  /**
   * Find the Bitdata Layout associated with values of this type, if there is one, or else return
   * null. TODO: perhaps this code should be colocated with bitdataName()?
   */
  public BitdataLayout bitdataLayout() {
    return name.bitdataLayout();
  }

  /** Find the name of the associated struct type, if any. */
  public StructName structName() {
    return name.structName();
  }

  /**
   * Return the natural number type that specifies the ByteSize of this type (required to be of kind
   * area) or null if this type has no ByteSize (i.e., no memory layout).
   */
  public Type byteSize(Type[] tenv) {
    return name.byteSize();
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type byteSize(Type[] tenv, Type a) {
    return name.byteSize(tenv, a);
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return name.byteSize(tenv, a, b);
  }

  Type byteSizeStoredRef(Type[] tenv) {
    return name.byteSizeStoredRef(null);
  }

  Type byteSizeStoredRef(Type[] tenv, Type a) {
    return name.byteSizeStoredRef(tenv, a);
  }

  Type byteSizeStoredRef(Type[] tenv, Type a, Type b) {
    return name.byteSizeStoredRef(tenv, a, b);
  }

  /**
   * A worker function that traverses a tuple type and removes the components that are not marked in
   * usedArgs. We assume a very simple structure for the input type: a left-leaning spine of TAps
   * with a tuple type constructor at the head, and no TGen, TVar, or TInd nodes on the spine.
   */
  Type removeArgs(int numUsedArgs, boolean[] usedArgs, int i) {
    return TupleCon.tuple(numUsedArgs).asType();
  }

  /**
   * Find a canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().canonType(null, set, args) : set.canon(name, args);
  }

  DataName isDataName() {
    return name.isDataName();
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return name.repCalc();
  }

  /**
   * Determine whether this type constructor is of the form Bit, Ix, or ARef l returning an
   * appropriate representation vector, or else null if none of these patterns applies. TODO: are
   * there other types we should be including here?
   */
  Type[] bitdataTyconRep(Type a) {
    return name.bitdataTyconRep(a);
  }

  /**
   * Determine whether this type constructor is an ARef, returning either an appropriate
   * representation vector, or else null.
   */
  Type[] bitdataTyconRep2(Type a, Type b) {
    return name.bitdataTyconRep2(a, b);
  }

  /**
   * Generate a call to a new primitive, wrapped in an appropriate chain of closure definitions, if
   * this type can be derived from pt in the following grammar: pt ::= [d1,...,dn] ->> rt ; rt ::=
   * [pt] | [r1,...,rm] .
   */
  Tail generatePrim(Position pos, String id) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().generatePrim(pos, id) : null;
  }

  /**
   * Test to see whether the receiver matches the grammar for pt, but with the additional
   * information that it appears in the context of an enclosing type of the form [d1,...,dn] ->>
   * [this].
   */
  Call generatePrimNested(Position pos, String id, Type[] ds) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().generatePrimNested(pos, id, ds) : null;
  }

  /**
   * Test to determine if this skeleton is an application of (->>) to a tuple of types, returning
   * either the tuple components in an array or null if there is no match.
   */
  Type[] funcFromTuple1() {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().funcFromTuple1() : null;
  }

  /** Test to determine if this type is the MILArrow, ->>, without any arguments. */
  boolean isMILArrow() {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().isMILArrow() : name.isMILArrow();
  }

  /**
   * Test to determine if this type is a tuple of the form [t1,...,tn], returning either the
   * components of the tuple in an array, or null if there is no match. The argument is the number
   * of potential tuple components that have already been seen; the initial call should use 0 for
   * this argument.
   */
  Type[] tupleComponents(int n) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().tupleComponents(n) : name.tupleComponents(n);
  }

  /**
   * Generate a block whose code implements an uncurried version of the TopLevel f, whose type is
   * the receiver. For this operation to succeed, the declared type must be a monomorphic type
   * matching the grammar: et ::= [d1,...dm] ->> [et] | [d1,...dm] ->> t where di, t are types and
   * we apply the first production as many times as possible. For example, if the declared type is
   * [[X,Y] ->> [[Z] ->> [R]]], then the generated block will have type [X,Y,Z] >>= [R] and body
   * b[x,y,z] = t <- f @ [x,y]; t @ [z].
   */
  Block liftToBlock0(Position pos, String id, TopLevel f) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().liftToBlock0(pos, id, f) : null;
  }

  /**
   * Helper function for liftToCode, used in the case where the receiver is the only component (in
   * position 0, explaining the name of this method) in a tuple type that is known to be the range
   * of a ->> function.
   */
  Code liftToCode0(Block b, Temp[] us, Atom f, Temp[] vs) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().liftToCode0(b, us, f, vs) : null;
  }

  boolean useBitdataLo(Type t, Type s) {
    return name != DataName.aref && name != DataName.aptr;
  }

  boolean nonUnit(Type[] tenv) {
    Synonym s = name.isSynonym();
    return (s != null) ? s.getExpansion().nonUnit(null) : name.nonUnit();
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    llvm.Type t = name.toLLVMCalc(c, lm, args);
    lm.drop(args);
    return t;
  }
}
