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

public class Sel extends Tail {

  private Cfun cf;

  private int n;

  private Atom a;

  /** Default constructor. */
  public Sel(Cfun cf, int n, Atom a) {
    this.cf = cf;
    this.n = n;
    this.a = a;
  }

  /** Test to determine whether a given tail expression has no externally visible side effect. */
  public boolean hasNoEffect() {
    return true;
  }

  /** Test if this Tail expression includes a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return a == w;
  }

  /**
   * Test if this Tail expression includes an occurrence of any of the variables listed in the given
   * array.
   */
  public boolean contains(Temp[] ws) {
    return a.occursIn(ws);
  }

  /** Add the variables mentioned in this tail to the given list of variables. */
  public Temps add(Temps vs) {
    return a.add(vs);
  }

  /** Test if two Tail expressions are the same. */
  public boolean sameTail(Tail that) {
    return that.sameSel(this);
  }

  boolean sameSel(Sel that) {
    return this.cf == that.cf && this.n == that.n && this.a.sameAtom(that.a);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return a.dependencies(ds);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    out.print(cf + " " + n + " " + a.toString(ts));
  }

  /**
   * Apply a TempSubst to this Tail. A call to this method, even if the substitution is empty, will
   * force the construction of a new Tail.
   */
  public Tail forceApply(TempSubst s) {
    return new Sel(cf, n, a.apply(s));
  }

  private AllocType type;

  /** The type tuple that describes the outputs of this Tail. */
  private Type outputs;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(outputs.tvars(tvs));
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public Type resultType() {
    return outputs;
  }

  Type inferType(Position pos) throws Failure {
    type = cf.checkSelIndex(pos, n).instantiate();
    type.resultUnifiesWith(pos, a.instantiate());
    return outputs = Type.tuple(type.storedType(n));
  }

  /**
   * Generate code for a Tail that appears as a regular call (i.e., in the initial part of a code
   * sequence). The parameter o specifies the offset for the next unused location in the current
   * frame; this will also be the first location where we can store arguments and results.
   */
  void generateCallCode(MachineBuilder builder, int o) {
    a.load(builder);
    builder.sel(n, o);
  }

  /**
   * Generate code for a Tail that appears in tail position (i.e., at the end of a code sequence).
   * The parameter o specifies the offset of the next unused location in the current frame. For
   * BlockCall and Enter, in particular, we can jump to the next function instead of doing a call
   * followed by a return.
   */
  void generateTailCode(MachineBuilder builder, int o) {
    a.load(builder);
    builder.sel(n, 0);
    builder.retn();
  }

  /**
   * Skip goto blocks in a Tail (for a ClosureDefn or TopLevel). TODO: can this be simplified now
   * that ClosureDefns hold Tails rather than Calls?
   */
  public Tail inlineTail() {
    return thisUnless(rewriteSel(null));
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return a.add(vs);
  }

  /**
   * Test to determine whether a given tail expression may be repeatable (i.e., whether the results
   * of a previous use of the same tail can be reused instead of repeating the tail). TODO: is there
   * a better name for this?
   */
  public boolean isRepeatable() {
    return true;
  }

  /**
   * Test to determine whether a given tail expression is pure (no externally visible side effects
   * and no dependence on other effects).
   */
  public boolean isPure() {
    return true;
  }

  public Code rewrite(Facts facts) {
    Tail t = rewriteSel(facts);
    return (t == null) ? null : new Done(t);
  }

  /** Liveness analysis. TODO: finish this comment. */
  Temps liveness(Temps vs) {
    a = a.shortTopLevel();
    return a.add(vs);
  }

  /**
   * Attempt to rewrite this selector if the argument is known to have been constructed using a
   * specific DataAlloc. TODO: find a better name?
   */
  Tail rewriteSel(Facts facts) {
    DataAlloc data = a.lookForDataAlloc(facts); // is a a known data allocator?
    if (data != null) {
      Atom a1 = data.select(cf, n); // find matching component
      if (a1 != null) {
        MILProgram.report("rewriting " + cf.getId() + " " + n + " " + a + " -> " + a1);
        return new Return(a1);
      }
    }
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return 4 + cf.summary() + n;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaSel(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaSel(Temps thisvars, Sel that, Temps thatvars) {
    return this.cf == that.cf && this.n == that.n && this.a.alphaAtom(thisvars, that.a, thatvars);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    a.collect(set);
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    if (type != null) {
      type = type.canonAllocType(set);
    }
    cf = cf.canonCfun(set);
  }

  /**
   * Eliminate a call to a newtype constructor or selector in this Tail by replacing it with a tail
   * that simply returns the original argument of the constructor or selector.
   */
  Tail removeNewtypeCfun() {
    if (cf.isNewtype()) { // Look for use of a newtype constructor
      if (n != 0) {
        debug.Internal.error("newtype selector for arg!=0");
      }
      return new Return(a);
    }
    // No point looking for a selector of a singleton type because they do not have any fields.
    return this;
  }

  /** Generate a specialized version of this Tail. */
  Tail specializeTail(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Sel(cf.specializeCfun(spec, type, s), n, a.specializeAtom(spec, s, env));
  }

  Tail bitdataRewrite(BitdataMap m) {
    BitdataRep r = cf.findRep(m); // Look for a possible change of representation
    if (r == null) { // No new representation for this type
      return this;
    } else if (cf.getArity()
        == 0) { // Representation change, but nullary so there is no layout constructor
      return new Sel(cf.bitdataRewrite(r), n, a);
    } else { // Representation change, requires layout constructor
      return new BlockCall(cf.bitdataSelBlock(r, n)).withArgs(a);
    }
  }

  Tail repTransform(RepTypeSet set, RepEnv env) {
    return cf.repTransformSel(set, env, n, a);
  }

  /**
   * Apply a representation transformation to this Tail value in a context where the result of the
   * tail should be bound to the variables vs before continuing to execute the code in c. For the
   * most part, this just requires the construction of a new Bind object. Special treatment,
   * however, is required for Selectors that access data fields whose representation has been spread
   * over multiple locations. This requires some intricate matching to check for selectors using
   * bitdata names or layouts, neither of which require this special treatment.
   */
  Code repTransform(RepTypeSet set, RepEnv env, Temp[] vs, Code c) {
    return cf.repTransformSel(set, env, vs, n, a, c);
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    debug.Internal.error("Sel does not return void");
    return c;
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) { // cf n a
    llvm.Type objt = lm.cfunLayoutType(this.cf).ptr();
    llvm.Local base = vm.reg(objt); // register to hold a pointer to a structure for cfun this.cf
    llvm.Type at = lhs.getType().ptr();
    llvm.Local addr = vm.reg(at); // register to hold pointer to the nth component of this.c
    return new llvm.Op(
        base,
        new llvm.Bitcast(a.toLLVMAtom(lm, vm, s), objt),
        new llvm.Op(
            addr,
            new llvm.Getelementptr(at, base, llvm.Word.ZERO, new llvm.Word(n + 1)),
            new llvm.Op(lhs, new llvm.Load(addr), c)));
  }
}
