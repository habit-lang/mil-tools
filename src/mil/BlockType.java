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

/**
 * Represents the type of a MIL block or primitive with a tuple of inputs (specified by the domain
 * type) and a tuple of outputs (specified by the range type).
 */
public class BlockType {

  protected Type dom;

  protected Type rng;

  /** Default constructor. */
  public BlockType(Type dom, Type rng) {
    this.dom = dom;
    this.rng = rng;
  }

  /**
   * Construct a printable representation of this BlockType. This method is not intended for use
   * with TIndBlockTypes; that case is technically covered by the default definition for BlockType
   * but it will probably not produce the right output because it ignores the type environment.
   */
  public String toString() {
    return toString(new Prefix());
  }

  /** Construct a printable representation of this BlockType using the specified Prefix. */
  public String toString(Prefix prefix) {
    StringTypeWriter tw = new StringTypeWriter(prefix);
    tw.writeQuantifiers();
    dom.write(tw);
    tw.write(" >>= ");
    rng.write(tw);
    return tw.toString();
  }

  /**
   * Instantiate this BlockType, creating a monomorphic instance (either a BlockType or a
   * TIndBlockType) in which any universally quantified variables bound to fresh type variables.
   */
  public BlockType instantiate() {
    return this;
  }

  void domUnifiesWith(Position pos, Type type) throws Failure {
    dom.unify(pos, null, type, null);
  }

  Type apply(Position pos, Type[] inputs) throws Failure {
    domUnifiesWith(pos, Type.tuple(inputs));
    return rngType();
  }

  /** Return the domain type of this block type. */
  Type domType() {
    return dom;
  }

  /** Return the range type of this block type. */
  Type rngType() {
    return rng;
  }

  /** Return the arity (number of inputs) for this block type. */
  int getArity() {
    return dom.tupleArity(null, 0);
  }

  /** Return the outity (number of outputs) for this block type. */
  int getOutity() {
    return rng.tupleArity(null, 0);
  }

  /** Find the list of unbound type variables in this (monomorphic) BlockType. */
  TVars tvars() {
    return tvars(null);
  }

  TVars tvars(TVars tvs) {
    return rng.tvars(null, dom.tvars(null, tvs));
  }

  /**
   * Generalize this monomorphic BlockType to a polymorphic type using the specified list of generic
   * variables.
   */
  public BlockType generalize(TVar[] generics) {
    return generalize(generics, null);
  }

  /**
   * Create a generalized version of this BlockType using the specified list of generics and the
   * given type environment to interpret any TGen values in this BlockType.
   */
  protected BlockType generalize(TVar[] generics, Type[] tenv) {
    Type ndom = dom.skeleton(tenv, generics);
    Type nrng = rng.skeleton(tenv, generics);
    return (generics.length > 0)
        ? new PolyBlockType(ndom, nrng, new Prefix(generics))
        : new BlockType(ndom, nrng);
  }

  /**
   * Test to determine whether two block types are alpha equivalent. We assume that this method will
   * only be used with BlockType and PolyBlockType objects (i.e., not TIndBlockType) because these
   * are the only forms of BlockTypes that can be obtained as the result of generalization or an
   * explicitly declared type.
   */
  public boolean alphaEquiv(BlockType bt) {
    return bt.alphaBlockType(this, null);
  }

  /**
   * Test to determine whether this BlockType is alpha equivalent to the BlockType passed in as an
   * argument given the (optional) correspondence between TGens.
   */
  boolean alphaBlockType(BlockType left, TGenCorresp corresp) {
    return this.dom.alphaType(left.dom, corresp) && this.rng.alphaType(left.rng, corresp);
  }

  /** Test to see if this block type is monomorphic. */
  public BlockType isMonomorphic() {
    return this;
  }

  /** Determine if two block types can be matched. */
  public boolean match(Type[] thisenv, BlockType t, Type[] tenv) {
    return this.dom.match(thisenv, t.dom, tenv) && this.rng.match(thisenv, t.rng, tenv);
  }

  /**
   * Construct a modified version of this BlockType that (potentially) has fewer input arguments.
   * The usedArgs array is true in the positions where an argument should be retained, and
   * numUsedArgs is the total number of true values in usedArgs/the number of arguments in the
   * resulting block type's domain tuple. The original block type is instantiated and then, once the
   * appropriate argument type components have been removed, (re)generalized to account for any
   * changes in the number or order of quantified type variables.
   */
  BlockType removeArgs(int numUsedArgs, boolean[] usedArgs) {
    BlockType bt = freshInstantiate();
    bt.dom =
        (usedArgs == null) ? Type.empty : bt.dom.removeArgs(numUsedArgs, usedArgs, usedArgs.length);
    return bt.generalize(TVar.generics(bt.tvars(), null));
  }

  /**
   * Instantiate this BlockType, ensuring that the result is a newly allocated object (that can
   * therefore be modified by side effecting operations without modifying the original).
   */
  BlockType freshInstantiate() {
    return new BlockType(dom, rng);
  }

  /** Calculate a new version of this block type with canonical components. */
  BlockType canonBlockType(TypeSet set) {
    return new BlockType(dom.canonType(set), rng.canonType(set));
  }

  public BlockType apply(TVarSubst s) {
    return apply(null, s);
  }

  public BlockType apply(Type[] tenv, TVarSubst s) {
    return new BlockType(dom.apply(tenv, s), rng.apply(tenv, s));
  }

  /**
   * Extend a substitution by matching this (potentially polymorphic) BlockType against a
   * monomorphic instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, BlockType inst) {
    if (generics.length != 0 || !this.alphaEquiv(inst)) {
      debug.Internal.error("specializingSubst fails on BlockType");
    }
    return null;
  }

  /** Construct a BlockType from a pair of type expressions that describe its domain and range. */
  static BlockType validate(TyconEnv env, TypeExp dom, TypeExp rng) throws Failure {
    TyvarEnv params = new TyvarEnv(); // Validate domain and range types
    dom.scopeType(params, env, 0);
    rng.scopeType(params, env, 0);
    dom.checkKind(KAtom.TUPLE); // Check domain and range kinds
    rng.checkKind(KAtom.TUPLE);
    Prefix prefix = params.toPrefix(); // Build BlockType
    return prefix.forall(dom.toType(prefix), rng.toType(prefix));
  }

  /** Returns the LLVM type for value that is returned by a function. */
  llvm.Type retType(LLVMMap lm) {
    return lm.toLLVM(rng);
  }

  llvm.FunctionType toLLVM(LLVMMap lm) {
    llvm.Type rt = lm.toLLVM(rng);
    // TODO: eliminate duplicated calls to canonType in line below and in tupleToArray ...
    llvm.Type[] tys = dom.canonType(lm).tupleToArray(lm, 0, 0);
    return new llvm.FunctionType(rt, tys);
  }
}
