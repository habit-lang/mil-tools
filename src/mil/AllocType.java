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

/**
 * Represents the type of a MIL allocator, each of which describes the types of a tuple of values
 * that will be stored, and the type of the resulting allocated object.
 */
public class AllocType {

  protected Type[] stored;

  protected Type result;

  /** Default constructor. */
  public AllocType(Type[] stored, Type result) {
    this.stored = stored;
    this.result = result;
  }

  /** Convenience constructor for AllocType objects with no stored fields. */
  public AllocType(Type result) {
    this(Type.noTypes, result);
  }

  /** Return the arity (i.e., number of stored components) for this type. */
  public int getArity() {
    return stored.length;
  }

  /** Return a stored type component for this AllocType. */
  Type storedType(int i) {
    return stored[i];
  }

  /** Return the result type for this AllocType. */
  Type resultType() {
    return result;
  }

  /**
   * Construct a printable representation of a block type. This method is not intended for use with
   * TIndAllocTypes; that case is technically covered by the default definition for BlockType but
   * should not be expected to give useful results because it ignores the type environment.
   */
  public String toString() {
    return toString(new Prefix());
  }

  /** Construct a printable representation of this AllocType using the specified Prefix. */
  public String toString(Prefix prefix) {
    StringTypeWriter tw = new StringTypeWriter(prefix);
    tw.writeQuantifiers();
    tw.write("{");
    if (stored.length > 0) {
      stored[0].write(tw);
      for (int i = 1; i < stored.length; i++) {
        tw.write(", ");
        stored[i].write(tw);
      }
    }
    tw.write("} ");
    result.write(tw);
    return tw.toString();
  }

  /**
   * Calculate an lc type scheme for a curried constructor function corresponding to this AllocType.
   * This method is only intended to be used on AllocTypes resulting from explicit declarations or
   * generalization (i.e., no TIndAllocType objects).
   */
  public Scheme toScheme() {
    return toType();
  }

  protected Type toType() {
    Type type = result;
    for (int i = stored.length; --i >= 0; ) {
      type = Type.fun(stored[i], type);
    }
    return type;
  }

  /**
   * Instantiate this AllocType, creating a monomorphic instance (either an AllocType or a
   * TIndAllocType) in which any universally quantified variables bound to fresh type variables.
   */
  public AllocType instantiate() {
    return this;
  }

  Type[] tenv() {
    return null;
  }

  void storedUnifiesWith(Position pos, Type[] inputs) throws Failure {
    storedUnifiesWith(pos, tenv(), inputs);
  }

  void storedUnifiesWith(Position pos, Type[] tenv, Type[] inputs) throws Failure {
    if (inputs.length != stored.length) {
      throw new Failure(
          pos,
          "Mismatch between expected ("
              + stored.length
              + ") and actual ("
              + inputs.length
              + ") argument counts");
    }
    for (int i = 0; i < inputs.length; i++) {
      stored[i].unify(pos, tenv, inputs[i], null);
    }
  }

  void resultUnifiesWith(Position pos, Type type) throws Failure {
    result.unify(pos, tenv(), type, null);
  }

  Type alloc(Position pos, Type[] inputs) throws Failure {
    storedUnifiesWith(pos, inputs);
    return resultType();
  }

  /** Find the list of unbound type variables in this (monomorphic) AllocType. */
  TVars tvars() {
    return tvars(null);
  }

  TVars tvars(TVars tvs) {
    return tvars(tenv(), tvs);
  }

  protected TVars tvars(Type[] tenv, TVars tvs) {
    for (int i = 0; i < stored.length; i++) {
      tvs = stored[i].tvars(tenv, tvs);
    }
    return result.tvars(tenv, tvs);
  }

  /**
   * Generalize this monomorphic AllocType to a polymorphic type using the specified list of generic
   * variables.
   */
  public AllocType generalize(TVar[] generics) {
    return generalize(generics, tenv());
  }

  /**
   * Create a generalized version of this AllocType using the specified list of generics and the
   * given type environment to interpret any TGen values in this AllocType.
   */
  protected AllocType generalize(TVar[] generics, Type[] tenv) {
    Type[] nstored = new Type[stored.length];
    for (int i = 0; i < stored.length; i++) {
      nstored[i] = stored[i].skeleton(tenv, generics);
    }
    Type nresult = result.skeleton(tenv, generics);
    return (generics.length > 0)
        ? new PolyAllocType(nstored, nresult, new Prefix(generics))
        : new AllocType(nstored, nresult);
  }

  /**
   * Test to determine whether two allocator types are alpha equivalent. We assume that this method
   * will only be used with AllocType and PolyAllocType objects (i.e., not TIndAllocType) because
   * these are the only forms of AllocTypes that can be obtained as the result of generalization or
   * an explicity declared type.
   */
  public boolean alphaEquiv(AllocType at) {
    return at.alphaAllocType(this, null);
  }

  /**
   * Test to determine whether this AllocType is alpha equivalent to the AllocType passed in as an
   * argument (considering only the stored and result components of that argument).
   */
  boolean alphaAllocType(AllocType left, TGenCorresp corresp) {
    if (this.stored.length != left.stored.length) {
      return false;
    }
    for (int i = 0; i < stored.length; i++) {
      if (!this.stored[i].alphaType(left.stored[i], corresp)) {
        return false;
      }
    }
    return this.result.alphaType(left.result, corresp);
  }

  /** Test to see if this allocator type is monomorphic. */
  public AllocType isMonomorphic() {
    return this;
  }

  /** Determine if two allocator types can be matched. */
  public boolean match(Type[] thisenv, AllocType t, Type[] tenv) {
    if (this.stored.length != t.stored.length || !this.result.match(thisenv, t.result, tenv)) {
      return false;
    }
    for (int i = 0; i < this.stored.length; i++) {
      if (!this.stored[i].match(thisenv, t.stored[i], tenv)) {
        return false;
      }
    }
    return true;
  }

  void dump(PrintWriter out, Type head) {
    Type[] tenv = tenv();
    if (result.match(tenv, head, null)) {
      for (int i = 0; i < stored.length; i++) {
        out.print(" ");
        out.print(stored[i].skeleton(tenv).toString(TypeWriter.ALWAYS));
      }
    } else {
      debug.Internal.error("result type does not match " + head);
    }
  }

  /**
   * Construct a modified version of this AllocType that (potentially) has fewer stored components.
   * The usedArgs array is true in the positions where the original stored argument should be
   * retained, and numUsedArgs is the total number of true values in usedArgs (and hence the total
   * number of stored components in the final type). The modified type may not have the same set of
   * quantified variables as the original, so it is necessary to instantiate and then (re)generalize
   * the type once the appropriate stored components have been removed.
   */
  AllocType removeStored(int numUsedArgs, boolean[] usedArgs) {
    AllocType at = freshInstantiate();
    Type[] nstored = new Type[numUsedArgs];
    int j = 0;
    for (int i = 0; i < stored.length; i++) {
      if (usedArgs != null && usedArgs[i]) {
        nstored[j++] = at.stored[i];
      }
    }
    at.stored = nstored;
    return at.generalize(TVar.generics(at.tvars(), null));
  }

  /** Instantiate this AllocType, ensuring that the result is a newly allocated object. */
  AllocType freshInstantiate() {
    return new AllocType(stored, result);
  }

  /** Calculate a new version of this allocator type with canonical components. */
  AllocType canonAllocType(TypeSet set) {
    return new AllocType(set.canonTypes(stored, null), result.canonType(set));
  }

  public AllocType apply(TVarSubst s) {
    return apply(null, s);
  }

  public AllocType apply(Type[] tenv, TVarSubst s) {
    Type[] nsts = new Type[stored.length];
    for (int i = 0; i < stored.length; i++) {
      nsts[i] = stored[i].apply(tenv, s);
    }
    return new AllocType(nsts, result.apply(tenv, s));
  }

  /**
   * Extend a substitution by matching this (potentially polymorphic) AllocType against a
   * monomorphic instance.
   */
  public TVarSubst specializingSubst(TVar[] generics, AllocType inst) {
    if (generics.length != 0 || !this.alphaEquiv(inst)) {
      debug.Internal.error("specializingSubst fails on AllocType");
    }
    return null;
  }

  boolean resultMatches(Type inst) {
    return result.match(null, inst, null);
  }

  /**
   * Calculate the offset of the field that was originally stored at offset n. The offset may change
   * if there were changes in the number of words used for any of the preceding fields.
   */
  int repOffset(int n) {
    int offset = 0;
    for (int i = 0; i < n; i++) {
      Type[] r = stored[i].repCalc();
      offset += (r == null ? 1 : r.length);
    }
    return offset;
  }

  /** Return the bit pattern for the ith stored component of this AllocType. */
  Pat bitPat(int i) {
    return stored[i].bitPat(null);
  }

  /**
   * Construct an AllocType from a collection of type expressions that describe the types of the
   * stored components and the type of the constructed result.
   */
  static AllocType validate(TyconEnv env, TypeExp[] storedexp, TypeExp texp) throws Failure {
    TyvarEnv params = new TyvarEnv();
    for (int i = 0; i < storedexp.length; i++) {
      storedexp[i].scopeType(params, env, 0);
      storedexp[i].checkKind(KAtom.STAR);
    }
    texp.scopeType(params, env, 0);
    texp.checkKind(KAtom.STAR);

    Prefix prefix = params.toPrefix();
    Type[] stored = new Type[storedexp.length];
    for (int i = 0; i < storedexp.length; i++) {
      stored[i] = storedexp[i].toType(prefix);
      storedexp[i].checkKind(KAtom.STAR);
    }
    return prefix.forall(stored, texp.toType(prefix));
  }

  /**
   * Calculate a structure type describing the layout of a data value built with a specific
   * constructor.
   */
  llvm.Type cfunLayoutTypeCalc(LLVMMap lm) {
    return structLayoutCalc(lm, LLVMMap.tagType());
  }

  /**
   * Return the type of an allocated structure for this type that starts with a tag of the given
   * type.
   */
  private llvm.StructType structLayoutCalc(LLVMMap lm, llvm.Type tag) {
    Type[] nustored = Type.nonUnits(stored);
    llvm.Type[] tys = new llvm.Type[1 + nustored.length];
    tys[0] = tag;
    for (int i = 0; i < nustored.length; i++) {
      tys[i + 1] = lm.toLLVM(nustored[i]);
    }
    return new llvm.StructType(tys);
  }

  /**
   * Calculate the type of a structure describing the layout of a closure for a specific definition.
   */
  llvm.Type closureLayoutTypeCalc(LLVMMap lm) {
    return structLayoutCalc(lm, lm.codePtrType(result));
  }
}
