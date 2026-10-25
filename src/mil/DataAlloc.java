/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import java.io.PrintWriter;

/**
 * Represents a Call of the form cf(as) that builds a data value with constructor function cf and
 * arguments as.
 */
public class DataAlloc extends Allocator {

  /** The constructor for the call. */
  private Cfun cf;

  /** Default constructor. */
  public DataAlloc(Cfun cf) {
    this.cf = cf;
  }

  /** Constructor for calls where arguments are already known. */
  public DataAlloc(Cfun cf, Atom[] args) {
    this(cf);
    this.args = args;
  }

  /** Constructor for calls with one argument that is already known. */
  public DataAlloc(Cfun cf, Atom a) {
    this(cf, new Atom[] {a});
  }

  /** Display a printable representation of this code sequence on the specified PrintWriter. */
  void dump(PrintWriter out, Temps ts) {
    dump(out, cf.toString(), "(", args, ")", ts);
  }

  /** Test if two Tail expressions are the same. */
  boolean sameTail(Tail that) {
    return that.sameDataAlloc(this);
  }

  /** Test whether this Tail is the same as the given DataAlloc. */
  boolean sameDataAlloc(DataAlloc that) {
    return this.cf == that.cf && this.sameArgs(that);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new DataAlloc(cf, args);
  }

  /** The AllocType instance for this call. */
  private AllocType ltype;

  /** Records the type tuple that describes the outputs of this Tail. */
  private Type outputs;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return ltype.tvars(tvs);
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  Type resultType() {
    return outputs;
  }

  /** Infer a type for this Call expression. */
  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    ltype = cf.instantiate();
    return outputs = Type.tuple(ltype.alloc(pos, inputs));
  }

  void invokeCall(MachineBuilder builder, int o) {
    builder.alloc(cf.getNum(), args.length, o);
    builder.store(o);
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.sameDataAllocForm(this);
  }

  boolean sameDataAllocForm(DataAlloc that) {
    return that.cf == this.cf && that.cfunNoArgs() == this.cfunNoArgs();
  }

  /**
   * Return the associated constructor function if this is a data allocator without any arguments.
   */
  Cfun cfunNoArgs() {
    return (args == null) ? cf : null;
  }

  boolean isRecursive() {
    return cf.getDataName().isRecursive();
  }

  /**
   * Figure out the BlockCall that will be used in place of the original after shorting out a Case.
   * Note that we require a DataAlloc fact for this to be possible (closures and monadic thunks
   * shouldn't show up here if the program is well-typed, but we'll check for this just in case).
   * Once we've established an appropriate DataAlloc, we can start testing each of the alternatives
   * to find a matching constructor, falling back on the default branch if no other option is
   * available.
   */
  BlockCall shortCase(TempSubst s, Alts alts) {
    BlockCall bc = alts.blockCallFor(cf);
    if (bc != null) {
      MILProgram.report("shorting out match on constructor " + cf);
      return bc.applyBlockCall(s);
    }
    return null;
  }

  /**
   * Test whether this Tail value corresponds to a data allocator, returning either a DataAlloc
   * value, or else a null result.
   */
  DataAlloc lookForDataAlloc() {
    return this;
  }

  /**
   * Return the atom corresponding to a component of this data allocator, provided that the
   * constructor matches and the argument index is in range.
   */
  Atom select(Cfun cf, int i) {
    return (this.cf == cf && args != null && i < args.length) ? args[i] : null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
   */
  int summary() {
    return summary(cf.summary()) * 33 + 2;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaDataAlloc(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaDataAlloc(Temps thisvars, DataAlloc that, Temps thatvars) {
    return this.cf == that.cf && this.alphaArgs(thisvars, that, thatvars);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (ltype != null) {
      ltype = ltype.canonAllocType(set);
    }
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    cf = cf.canonCfun(set);
    Atom.collect(args, set);
  }

  /**
   * Eliminate a call to a newtype constructor or selector in this Tail by replacing it with a tail
   * that simply returns the original argument of the constructor or selector.
   */
  Tail removeNewtypeCfun() {
    if (cf.isNewtype()) {
      // Look for use of a newtype constructor
      if (args == null || args.length != 1) {
        debug.Internal.error("newtype constructor with arity!=1");
      }
      // and generate a Return instead
      return new Return(args[0]);
    } else if (cf.isSingleton() && cf != Cfun.Unit) {
      // Look for a use of a singleton constructor
      return new DataAlloc(Cfun.Unit).withArgs();
    }
    return this;
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new DataAlloc(cf.specializeCfun(spec, ltype, s));
  }

  /** Rewrite this tail to use the bitdata representations provided by BitdataMap. */
  Tail bitdataRewrite(BitdataMap m) {
    // Look for a possible change of representation
    BitdataRep r = cf.findRep(m);
    if (r == null) {
      // No new representation for this type
      return this;
    } else if (cf.getArity() == 0) {
      // Representation change, but nullary so there is no layout constructor
      return new DataAlloc(cf.bitdataRewrite(r), args);
    } else {
      // Representation change, requires layout constructor
      return new BlockCall(cf.bitdataConsBlock(r), args);
    }
  }

  /**
   * Generate a new version of this Tail to update references to constructor functions consistent
   * with the datatype equalities in the given MergeMap.
   */
  Tail mergeRewrite(MergeMap mmap) {
    return new DataAlloc(cf.lookup(mmap), args);
  }

  Tail repTransform(RepTypeSet set, RepEnv env) {
    return cf.repTransformDataAlloc(set, env, Atom.repArgs(set, env, args));
  }

  /**
   * Create a reference to a statically allocated data structure corresponding to this Allocator,
   * having already established that all of the components (if any) are statically known.
   */
  llvm.Value staticAlloc(LLVMMap lm, llvm.Program prog, llvm.Value[] vals) {
    vals[0] = new llvm.Word(cf.getNum()); // add tag at front of object
    return staticAlloc(prog, vals, lm.cfunLayoutType(cf));
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    if (cf != Cfun.Unit) {
      debug.Internal.error("DataAlloc does not return void");
    }
    return c;
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    llvm.Type objt = lm.cfunLayoutType(cf); // type of a cf object
    return alloc(lm, vm, s, objt, lhs, new llvm.Word(cf.getNum()), c);
  }
}
