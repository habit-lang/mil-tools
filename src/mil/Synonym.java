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
import java.io.PrintWriter;
import obdd.Pat;

/** Represents a nullary type synonym. */
public class Synonym extends Tycon {

  private Kind kind;

  private Type expansion;

  /** Default constructor. */
  public Synonym(Position pos, String id, Kind kind, Type expansion) {
    super(pos, id);
    this.kind = kind;
    this.expansion = expansion;
  }

  public Type getExpansion() {
    return expansion;
  }

  public void setExpansion(Type expansion) {
    this.expansion = expansion;
  }

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return kind;
  }

  /** Return the arity of this type constructor. */
  public int getArity() {
    // TODO: The arity values returned by this method are used only to provide (hopefully)
    // friendlier error messages when a tycon is applied to the wrong number of arguments ...
    // For a synonym, we choose a large value to avoid imposing a limit.  Perhaps we can
    // find a better abstraction than getArity() that better matches the actual use.
    return Integer.MAX_VALUE;
  }

  public Synonym isSynonym() {
    return this;
  }

  public void fixKinds() {
    kind = kind.fixKind();
  }

  public int level = 0;

  public int getLevel() {
    return level;
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

  boolean sameTLit(TLit t) {
    return expansion.sameTLit(null, t);
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
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return expansion.tupleArity(null, n);
  }

  /**
   * Print a definition for this type constructor using source level syntax. TODO: Find a more
   * appropriate place for this code ...
   */
  void dumpTypeDefinition(PrintWriter out) {
    out.print("type ");
    out.print(id);
    out.print(" = ");
    out.println(expansion.toString());
    out.println();
  }

  /**
   * Find the canonical version of this type in the given set, using the specified environment to
   * interpret TGens, and assuming that we have already pushed a certain number of args for this
   * type on the stack.
   */
  Type canonType(Type[] env, TypeSet set, int args) {
    return expansion.canonType(null, set, args);
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    return this;
  }

  Type canonArgs(Type[] tenv, TypeSet set, int args) {
    return expansion.canonArgs(null, set, args);
  }

  /** Return the representation vector for values of this type. */
  Type[] repCalc() {
    return expansion.repCalc();
  }

  /**
   * Return the representation vector for types formed by applying this type to the argument a. This
   * allows us to provide special representations for types of the form Bit a, Ix a, Ref a, etc. If
   * none of these apply, we just return null. TODO: are there other types we should be including
   * here?
   */
  Type[] repCalc(Type a) {
    return expansion.repCalc(a);
  }

  /**
   * Generate a call to a new primitive, wrapped in an appropriate chain of closure definitions, if
   * this type can be derived from pt in the following grammar: pt ::= [d1,...,dn] ->> rt ; rt ::=
   * [pt] | [r1,...,rm] .
   */
  Tail generatePrim(Position pos, String id) {
    return expansion.generatePrim(pos, id);
  }

  /**
   * Test to see whether the receiver matches the grammar for pt, but with the additional
   * information that it appears in the context of an enclosing type of the form [d1,...,dn] ->>
   * [this].
   */
  Call generatePrimNested(Position pos, String id, Type[] ds) {
    return expansion.generatePrimNested(pos, id, ds);
  }

  /**
   * Test to determine if this skeleton is an application of (->>) to a tuple of types, returning
   * either the tuple components in an array or null if there is no match.
   */
  Type[] funcFromTuple1() {
    return expansion.funcFromTuple1();
  }

  /** Test to determine if this type is the MIL function arrow, ->>, without any arguments. */
  boolean isMILArrow() {
    return expansion.isMILArrow();
  }

  /**
   * Test to determine if this type is a tuple of the form [t1,...,tn], returning either the
   * components of the tuple in an array, or null if there is no match. The argument is the number
   * of potential tuple components that have already been seen; the initial call should use 0 for
   * this argument.
   */
  Type[] tupleComponents(int n) {
    return expansion.tupleComponents(n);
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
    return expansion.liftToBlock0(pos, id, f);
  }

  /**
   * Helper function for liftToCode, used in the case where the receiver is the only component (in
   * position 0, explaining the name of this method) in a tuple type that is known to be the range
   * of a ->> function.
   */
  Code liftToCode0(Block b, Temp[] us, Atom f, Temp[] vs) {
    return expansion.liftToCode0(b, us, f, vs);
  }

  /**
   * Worker method for calculating the BitSize for a type of the form (this a) (i.e., this, applied
   * to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  Type bitSize(Type[] tenv, Type a) {
    return expansion.bitSize(tenv, a);
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
    return expansion.bitPat(null, a.with(tenv));
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
    return expansion.byteSize(null, a.with(tenv));
  }

  /**
   * Worker method for calculating the ByteSize for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  Type byteSize(Type[] tenv, Type a, Type b) {
    return expansion.byteSize(null, a.with(tenv), b.with(tenv));
  }

  /** Determine if this is a type of the form (Ref a) or (Ptr a) for some area type a. */
  boolean referenceType(Type[] tenv) {
    return expansion.referenceType(null);
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return expansion.referenceType(null, a.with(tenv));
  }

  /** Return the alignment associated with this type constructor. */
  public long alignment() {
    return expansion.alignment(null);
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a) (i.e., this,
   * applied to the argument a). The specified type environment, tenv, is used for both this and a.
   */
  long alignment(Type[] tenv, Type a) {
    return expansion.alignment(null, a.with(tenv));
  }

  /**
   * Worker method for calculating the alignment for a type of the form (this a b) (i.e., this,
   * applied to two arguments, a and b). The specified type environment, tenv, is used for this, a,
   * and b.
   */
  long alignment(Type[] tenv, Type a, Type b) {
    return expansion.alignment(null, a.with(tenv), b.with(tenv));
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return expansion.nonUnit(null);
  }
}
