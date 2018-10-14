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

public class Enter extends Call {

  private Atom f;

  /** Default constructor. */
  public Enter(Atom f) {
    this.f = f;
  }

  public Enter(Atom f, Atom a) { // f @ a
    this(f, new Atom[] {a});
  }

  public Enter(Atom f, Atom[] args) { // f @ args
    this.f = f;
    this.args = args;
  }

  /** Test if this Tail expression includes a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return f == w || super.contains(w);
  }

  /**
   * Test if this Tail expression includes an occurrence of any of the variables listed in the given
   * array.
   */
  public boolean contains(Temp[] ws) {
    return f.occursIn(ws) || super.contains(ws);
  }

  /** Add the variables mentioned in this tail to the given list of variables. */
  public Temps add(Temps vs) {
    return f.add(super.add(vs));
  }

  /** Test if two Tail expressions are the same. */
  public boolean sameTail(Tail that) {
    return that.sameEnter(this);
  }

  boolean sameEnter(Enter that) {
    return this.f.sameAtom(that.f) && this.sameArgs(that);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return f.dependencies(super.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    out.print(f.toString(ts) + " @ ");
    Atom.displayTuple(out, args, ts);
  }

  /**
   * Apply a TempSubst to this Tail. A call to this method, even if the substitution is empty, will
   * force the construction of a new Tail.
   */
  public Tail forceApply(TempSubst s) {
    return new Enter(f.apply(s), TempSubst.apply(args, s));
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return new Enter(f).withArgs(args);
  }

  /** The type tuple that describes the outputs of this Tail. */
  private Type outputs;

  private Type ftype;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return ftype.tvars(outputs.tvars(tvs));
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public Type resultType() {
    return outputs;
  }

  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    outputs = new TVar(Tyvar.tuple);
    ftype = f.instantiate();
    Type.milfun(Type.tuple(inputs), outputs).unify(pos, ftype);
    return outputs;
  }

  void invokeCall(MachineBuilder builder, int o) {
    f.load(builder);
    builder.ccall(o);
  }

  /**
   * Generate code for a Tail that appears in tail position (i.e., at the end of a code sequence).
   * The parameter o specifies the offset of the next unused location in the current frame. For
   * BlockCall and Enter, in particular, we can jump to the next function instead of doing a call
   * followed by a return.
   */
  void generateTailCode(MachineBuilder builder, int o) {
    int i = f.frameSlot(builder);
    if (i >= 0 && i < args.length) { // if closure will be overwritten:
      builder.copy(i, o); // copy closure to a safe slot
      generateTailArgs(builder);
      builder.load(o);
      builder.cjump();
    } else { // if closure will not be overwritten:
      generateTailArgs(builder);
      f.load(builder);
      builder.cjump();
    }
  }

  /**
   * Test to determine if this Tail is an expression of the form (w @ a1,...,an) for some given w,
   * and any a1,...,an (so long as they do not include w), returning the argument list a1,...,an as
   * a result.
   */
  public Atom[] enters(Temp w) {
    return (f == w && !w.occursIn(args)) ? args : null;
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.sameEnterForm(this);
  }

  boolean sameEnterForm(Enter that) {
    return that.f.sameAtom(this.f);
  }

  /**
   * Skip goto blocks in a Tail (for a ClosureDefn or TopLevel). TODO: can this be simplified now
   * that ClosureDefns hold Tails rather than Calls?
   */
  public Tail inlineTail() {
    return thisUnless(rewriteEnter(null));
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return f.add(super.usedVars(vs));
  }

  public Code rewrite(Facts facts) {
    Tail t = rewriteEnter(facts);
    if (t != null) {
      Code c = t.rewrite(facts);
      return (c == null) ? new Done(t) : c;
    }
    return null;
  }

  /** Liveness analysis. TODO: finish this comment. */
  Temps liveness(Temps vs) {
    f = f.shortTopLevel();
    return f.add(super.liveness(vs));
  }

  Tail rewriteEnter(Facts facts) {
    ClosAlloc clos = f.lookForClosAlloc(facts); // Is f a known closure?
    if (clos != null) {
      // TODO: Is this rewrite always a good idea? Are there cases where it
      // ends up creating more work than it saves?
      MILProgram.report("rewriting " + f + " @ [" + Atom.toString(args) + "]");
      return clos.enterWith(args); // If so, apply it in place
    }
    return f.entersTopLevel(args);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return summary(3);
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaEnter(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaEnter(Temps thisvars, Enter that, Temps thatvars) {
    return this.f.alphaAtom(thisvars, that.f, thatvars) && this.alphaArgs(thisvars, that, thatvars);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (outputs != null) {
      outputs = outputs.canonType(set);
    }
    if (ftype != null) {
      ftype = ftype.canonType(set);
    }
    f.collect(set);
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new Enter(f.specializeAtom(spec, s, env));
  }

  Tail repTransform(RepTypeSet set, RepEnv env) {
    Atom[] fs = f.repArg(set, env);
    Atom[] nargs = Atom.repArgs(set, env, args);
    if (fs != null) {
      if (fs.length != 1) {
        debug.Internal.error("invalid multiple word function representation");
      }
      return new Enter(fs[0]).withArgs(nargs);
    }
    return new Enter(f).withArgs(nargs);
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    llvm.Value[] acts = closureActuals(lm, vm, s); // actual parameters
    llvm.Local cptr = vm.reg(lm.toLLVM(ftype)); // a register to hold the code pointer
    return enterCode(vm, acts[0], cptr, new llvm.CallVoid(isTail, cptr, acts, c));
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    llvm.Value[] acts = closureActuals(lm, vm, s); // actual parameters
    llvm.Local cptr = vm.reg(lm.codePtrType(ftype)); // a register to hold the code pointer
    return enterCode(
        vm,
        acts[0],
        cptr,
        new llvm.Op(lhs, new llvm.Call(isTail, ftype.retType(lm), cptr, acts), c));
  }

  llvm.Value[] closureActuals(LLVMMap lm, VarMap vm, TempSubst s) {
    Atom[] nuargs = Atom.nonUnits(args);
    llvm.Value[] acts = new llvm.Value[1 + nuargs.length]; // make the argument list
    acts[0] = f.toLLVMAtom(lm, vm, s); // a pointer to the closure for f as an llvm value
    for (int i = 0; i < nuargs.length; i++) { // with llvm values for each of the function arguments
      acts[i + 1] = nuargs[i].toLLVMAtom(lm, vm, s);
    }
    return acts;
  }

  llvm.Code enterCode(VarMap vm, llvm.Value clo, llvm.Local cptr, llvm.Code c) {
    // TODO: this method doesn't really belong in Enter because it doesn't use any Enter fields or
    // methods ...
    // We are generating code for a closure entry  f @ as  where f :: dom ->> rng for some tuples of
    // domain and range values.  Specifically here, we are assuming that rng is not [], and that f
    // is
    // represented by a value of type %t.layout*, where:
    //     type %t.layout = { %t.entry* }     -- layout of a generic dom ->> rng closure in memory
    //     type %t.entry  = rng' (%t.layout*, dom')  -- rng' and dom' corresponding to rng and dom
    llvm.Type ct = cptr.getType().ptr();
    llvm.Local cptrptr =
        vm.reg(ct); // a register to hold the address where the code pointer is stored
    return new llvm.Op(
        cptrptr,
        new llvm.Getelementptr(ct, clo, llvm.Word.ZERO, llvm.Word.ZERO), // 0th field of 0th closure
        new llvm.Op(
            cptr,
            new llvm.Load(cptrptr), // load function address
            c));
  }
}
