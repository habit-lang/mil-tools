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
 * Represents a Call of the form k{as} that constructs a closure object with code k and stored data
 * as.
 */
public class ClosAlloc extends Allocator {

  /** The closure definition for the call. */
  private final ClosureDefn k;

  /** Default constructor. */
  public ClosAlloc(final ClosureDefn k) {
    this.k = k;
    k.calledFrom(this);
  }

  /** Constructor for calls where arguments are already known. */
  public ClosAlloc(ClosureDefn k, Atom[] args) {
    this(k);
    this.args = args;
  }

  /** Constructor for calls with one argument that is already known. */
  public ClosAlloc(ClosureDefn k, Atom a) {
    this(k, new Atom[] {a});
  }

  /** Find the dependencies of this AST fragment. */
  Defns dependencies(Defns ds) {
    return k.dependencies(super.dependencies(ds));
  }

  /** Display a printable representation of this code sequence on the specified PrintWriter. */
  void dump(PrintWriter out, Temps ts) {
    dump(out, k.toString(), "{", args, "}", ts);
  }

  /** Test if two Tail expressions are the same. */
  boolean sameTail(Tail that) {
    return that.sameClosAlloc(this);
  }

  /** Test whether this Tail is the same as the given ClosAlloc. */
  boolean sameClosAlloc(ClosAlloc that) {
    return this.k == that.k && this.sameArgs(that);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new ClosAlloc(k, args);
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
    ltype = k.instantiate();
    return outputs = Type.tuple(ltype.alloc(pos, inputs));
  }

  void invokeCall(MachineBuilder builder, int o) {
    builder.alloc(k, args.length, o);
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
    return c.sameClosAllocForm(this);
  }

  boolean sameClosAllocForm(ClosAlloc that) {
    return that.k == this.k;
  }

  ClosAlloc deriveWithKnownCons(Defn d, Call[] calls) {
    // if (calls!=null) { return null; } // disable optimization
    if (calls.length != args.length) {
      debug.Internal.error("ClosAlloc argument list length mismatch in deriveWithKnownCons");
    }
    ClosureDefn nk = k.deriveWithKnownCons(calls);
    if (nk == null) {
      return null;
    } else {
      ClosAlloc ca = new ClosAlloc(nk, specializedArgs(calls));
      return ca;
    }
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return k.usedVars(args, vs);
  }

  public Code rewrite(Defn d, Facts facts) {
    Call[] calls = k.collectCalls(args, facts, false);
    if (calls != null) {
      ClosAlloc ca = deriveWithKnownCons(d, calls);
      if (ca != null) {
        MILProgram.report(
            "deriving specialized closure definition " + ca.k + " for ClosAlloc using " + k);
        return new Done(ca);
      }
    }
    return null;
  }

  boolean isRecursive() {
    return k.isRecursive();
  }

  /**
   * Test if this Code/Tail value corresponds to a closure allocator, returning either a ClosAlloc
   * value, or else a null result.
   */
  ClosAlloc lookForClosAlloc() {
    return this;
  }

  /**
   * Compute a Tail that gives the result of applying this ClosAlloc value to a specified argument
   * a.
   */
  Tail enterWith(Atom[] fargs) {
    return k.withArgs(args, fargs);
  }

  /**
   * Traverse abstract syntax in the context of a definition d with the given parameters as
   * contributions to calculating initial values for all Block and ClosureDefns in the program. We
   * use the rebound list to indicate when Temp values appearing in the parameters have been reused,
   * effectively shadowing the original parameter binding. In practice, we expect such uses to be
   * rare, and so the rebound list will usually be empty. But it is still important to check for
   * rebound parameters, just in case!
   */
  void calcSources(Defn d, Temp[] params, Temps rebound) {
    updateSources(d, params, rebound, k);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
   */
  int summary() {
    return summary(k.summary()) * 33 + 3;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaClosAlloc(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaClosAlloc(Temps thisvars, ClosAlloc that, Temps thatvars) {
    return this.k == that.k && this.alphaArgs(thisvars, that, thatvars);
  }

  Tail eliminateDuplicates() {
    ClosureDefn k1 = k.getReplaceWith();
    return (k1 == null) ? this : new ClosAlloc(k1, args);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (ltype != null) {
      ltype = ltype.canonAllocType(set);
    }
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new ClosAlloc(spec.specializedClosureDefn(k, ltype.apply(s)));
  }

  /**
   * Find the argument variables that are used in this Tail, adding results to an accumulating list.
   * This is mostly just the same as adding the the variables defined in the Tail except that we
   * include updates in the cases for BlockCall and ClosAlloc if the argument lists are not already
   * known.
   */
  public Temps addArgs(Temps vs) throws Failure {
    return (args == null) ? Temps.add(args = k.addArgs(), vs) : vs;
  }

  /**
   * Create a reference to a statically allocated data structure corresponding to this Allocator,
   * having already established that all of the components (if any) are statically known.
   */
  llvm.Value staticAlloc(LLVMMap lm, llvm.Program prog, llvm.Value[] vals) {
    vals[0] = lm.globalFor(k); // add code pointer to start of object
    return staticAlloc(prog, vals, lm.closureLayoutType(k));
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    debug.Internal.error("ClosAlloc does not return void");
    return c;
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    llvm.Type objt = lm.closureLayoutType(k); // type of a k object
    return alloc(lm, vm, s, objt, lhs, lm.globalFor(k), c);
  }
}
