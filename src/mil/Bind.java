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

/**
 * Represents a code sequence that binds the variable(s) in vs to the result produced by running t
 * and then continues by executing the code in c.
 */
public class Bind extends Code {

  /** The variable(s) that will capture the result. */
  private Temp[] vs;

  /** The tail whose result will be stored in vs. */
  private Tail t;

  /** The rest of the code sequence. */
  private Code c;

  /** Default constructor. */
  public Bind(Temp[] vs, Tail t, Code c) {
    this.vs = vs;
    this.t = t;
    this.c = c;
  }

  public Bind(Temp v, Tail t, Code c) {
    this(new Temp[] {v}, t, c);
  }

  /** Test for a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return t.contains(w) || (!w.occursIn(vs) && c.contains(w));
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return t.dependencies(c.dependencies(ds));
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    Temps ts1 = Defn.renameTemps ? Temps.push(vs, ts) : ts;
    indent(out);
    Atom.displayTuple(out, vs, ts1);
    out.print(" <- ");
    t.displayln(out, ts);
    c.dump(out, ts1);
  }

  /**
   * Force the application of a TempSubst to this Code sequence, forcing construction of a fresh
   * copy of the input code structure, including the introduction of new temporaries in place of any
   * variables introduced by Binds.
   */
  public Code forceApply(TempSubst s) { // vs <- t; c
    Tail t1 = t.forceApply(s);
    Temp[] ws = new Temp[vs.length];
    for (int i = 0; i < vs.length; i++) {
      s = vs[i].mapsTo(ws[i] = new Temp(), s);
    }
    return new Bind(ws, t1, c.forceApply(s));
  }

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return c.tvars(t.tvars(tvs));
  }

  Type inferType(Position pos) throws Failure { // vs <- t; c
    t.inferType(pos).unify(pos, Type.tuple(Type.freshTypes(vs)));
    return c.inferType(pos);
  }

  /**
   * Generate bytecode for this code sequence, assuming that o is the offset of the next unused
   * location in the current frame.
   */
  void generateCode(MachineBuilder builder, int o) {
    t.generateCallCode(builder, o);
    builder.extend(vs, o);
    c.generateCode(builder, o + vs.length);
  }

  /**
   * Generate a new version of this code sequence to add a trailing enter operation that applies the
   * value that would have been returned by the code in the original block to the specified argument
   * parameter.
   */
  Code deriveWithEnter(Atom[] iargs) {
    return new Bind(vs, t, c.deriveWithEnter(iargs));
  }

  /**
   * Given an expression of the form (w <- b[..]; c), attempt to construct an equivalent code
   * sequence that instead calls a block whose code includes a trailing enter.
   */
  public Code enters(Temp w, BlockCall bc) {
    Atom[] iargs = t.enters(w);
    return (iargs != null && !c.contains(w)) ? new Bind(vs, bc.deriveWithEnter(iargs), c) : null;
  }

  /**
   * Modify this code sequence to add a trailing enter operation that passes the value that would
   * have been returned by the code in the original block to the specified continuation parameter.
   */
  Code deriveWithCont(Atom cont) {
    return new Bind(vs, t, c.deriveWithCont(cont));
  }

  Code copy() {
    return new Bind(vs, t, c.copy());
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return t.doesntReturn() || c.doesntReturn();
  }

  boolean detectLoops(
      Block src, Blocks visited) { // look for src[x] = (vs <- b[x]; ...), possibly with some
    // initial prefix of pure bindings xs1 <- pt1; ...; xsn <- ptn
    return t.detectLoops(src, visited) || (t.hasNoEffect() && c.detectLoops(src, visited));
  }

  /**
   * Return a possibly shortened version of this code sequence by applying some simple
   * transformations. The src Block is passed as an argument for use in reporting any optimizations
   * that are enabled.
   */
  Code cleanup(Block src) {
    if (Temp.noneLive(vs)
        && t.hasNoEffect()) { // Rewrite (_ <- t; c) ==> c, if t has no visible effect
      MILProgram.report("inlining eliminated a wildcard binding in " + src.getId());
      return c.cleanup(src);
    } else if (c.isReturn(vs)) { // Rewrite (vs <- t; return vs) ==> t
      MILProgram.report("applied right monad law in " + src.getId());
      return new Done(t);
    } else if (t.blackholes()) { // Rewrite (vs <- loop(()); c) ==> loop(())
      MILProgram.report("rewrite (vs <- loop(()); c) ==> loop(())");
      return new Done(Prim.loop.withArgs());
    } else if (t.doesntReturn()
        && !c.blackholes()) { // Rewrite (vs <- t; c) ==> vs <- t; loop(()), if t doesn't return
      MILProgram.report("removed code after a tail that does not return in " + src.getId());
      return new Bind(vs, t, new Done(Prim.loop.withArgs()));
    } else {
      c = c.cleanup(src);
      return this;
    }
  }

  /**
   * Perform inlining on this Code, decrementing the limit each time a successful inlining is
   * performed, and declining to pursue further inlining at this node once the limit reaches zero.
   */
  Code inlining(Block src, int limit) {
    if (limit > 0) { // Is this an opportunity for prefix inlining?
      Code ic = t.prefixInline(src, vs, c);
      if (ic != null) {
        return ic.inlining(src, limit - 1);
      }
    }
    c = c.inlining(src);

    // Rewrite an expression (v <- b[x,..]; v @ [a1...]) ==> b'[x,..,a1,...]
    //                       ...
    BlockCall bc = t.isBlockCall();
    if (bc != null) {
      Code nc;
      if ((nc = c.enters(vs, bc)) != null) {
        MILProgram.report("pushed enter into call in " + src.getId());
        return nc;
      } else if ((nc = c.casesOn(vs, bc)) != null) {
        MILProgram.report("pushed case into call in " + src.getId());
        return nc;
      }

      t = bc.inlineBlockCall();
    }
    return this;
  }

  Code prefixInline(TempSubst s, Temp[] us, Code d) {
    Temp[] ws = Temp.makeTemps(vs.length);
    return new Bind(ws, t.apply(s), c.prefixInline(TempSubst.extend(vs, ws, s), us, d));
  }

  int prefixInlineLength(int len) {
    return (t.blackholes() || t.noinline()) ? 0 : c.prefixInlineLength(len + 1);
  }

  /**
   * Compute the length of this Code sequence for the purposes of prefix inlining. The returned
   * value is either the length of the code sequence (counting one for each Bind and Done node) or 0
   * if the code sequence ends with something other than Done. The argument should be initialized to
   * 0 for the first call.
   */
  int suffixInlineLength(int len) {
    return t.noinline() ? (-1) : c.suffixInlineLength(len + 1);
  }

  /**
   * Determine if, for the purposes of suffix inlining, it is possible to get back to the specified
   * source block via a sequence of tail calls. (i.e., without an If or Case guarding against an
   * infinite loop.)
   */
  boolean guarded(Block src) {
    return c.guarded(src);
  }

  void liftAllocators() {
    t = t.liftStaticAllocator();
    if (t.isAllocator() != null) {
      // This bind uses an allocator, so we can only look for lifting opportunities in the rest of
      // the code.
      c.liftAllocators();
    } else {
      // This bind does not have an allocator, but it could be used as a non-allocator parent for
      // the following
      // code ... which might turn this node into an allocator, prompting the need to repeat the
      // call to
      // this.liftAllocators().
      if (c.liftAllocators(this)) {
        this.liftAllocators();
      }
    }
  }

  boolean liftAllocators(Bind parent) {
    t = t.liftStaticAllocator();
    if (t.isAllocator() != null) {
      // This bind uses an allocator, so it can be swapped with the parent Bind, if that is safe.
      if (!Atom.occursIn(this.vs, parent.vs)
          && !Atom.occursIn(parent.vs, this.vs)
          && !this.t.contains(parent.vs)
          && !parent.t.contains(this.vs)) {
        Temp[] tempvs = parent.vs;
        parent.vs = this.vs;
        this.vs = tempvs; // swap vars
        Tail tempt = parent.t;
        parent.t = this.t;
        this.t = tempt; // swap tails
        // For the purposes of the following message, we assume that allocators return exactly one
        // result.  With that assumption, we could also have simplified the preceding safety check
        // ...
        MILProgram.report("lifted allocator for " + parent.vs[0]);
        c.liftAllocators(this); // Now this node is a non-allocator parent of c.

        return true;
      }

      // We can't change this Bind, but can still scan the rest of the code:
      c.liftAllocators();
      return false;
    } else {
      // This bind does not have an allocator, but it could be used as a non-allocator parent for
      // the following code ... if that changes this node, then we might have a second opportunity
      // to rewrite this node, hence the tail call:
      return c.liftAllocators(this) && this.liftAllocators(parent);
    }
  }

  /**
   * Find the list of variables that are used in this code sequence. Variables that are mentioned in
   * BlockCalls or ClosAllocs are only included if the corresponding flag in usedArgs is set.
   */
  Temps usedVars() {
    return t.usedVars(Temps.remove(vs, c.usedVars()));
  }

  Code removeUnusedArgs() {
    return new Bind(vs, t.removeUnusedArgs(), c.removeUnusedArgs());
  }

  /** Optimize a Code block using a simple flow analysis. */
  public Code flow(Facts facts, TempSubst s) {
    t = t.apply(s); // Update tail to reflect substitution
    s = TempSubst.extend(vs, vs, s); // Remove bindings for vs from the substitution
    // (TODO: this could be done more efficiently!)
    //  s = TempSubst.remove(vs, s);    // Update substitution

    // Common subexpression elimination:
    // TODO: do we need to limit the places where this is used?
    Temp p = Facts.find(t, facts); // Look for previously computed value
    if (p != null && vs.length == 1) {
      MILProgram.report("cse: using previously computed value " + p + " for " + vs[0]);
      return c.flow(facts, vs[0].mapsTo(p, s));
    }
    // TODO: this code needs careful attention!
    // Apply left monad law: (vs <- return as; c) == [as/vs]c
    Atom[] as = t.returnsAtom(); // Check for vs <- return as; c
    if (as != null) {
      MILProgram.report(
          "applied left monad law for " + Atom.toString(vs) + " <- return " + Atom.toString(as));
      return c.flow(facts, TempSubst.extend(vs, as, s));
    }

    // Look for opportunities to rewrite this tail, perhaps using previous results
    Code nc = t.rewrite(facts); // Look for ways to rewrite the tail
    if (nc != null) {
      return nc.andThen(vs, c).flow(facts, s);
    }

    // Propagate analysis to the following code, updating facts as necessary.
    for (int i = 0; i < vs.length; i++) { // Kill any facts for the bound variables
      facts = vs[i].kills(facts);
    }
    if (!t.hasNoEffect()) {
      // If this tail can have an effect, then kill any non pure facts
      // (e.g., observers) that may now be clobbered
      facts = Facts.killNonPure(facts);
    }
    if (vs.length == 1) { // Try to add a fact for a single LHS variable
      facts = vs[0].addFact(t, facts);
    }
    c = c.flow(facts, s);
    return this;
  }

  public Code andThen(Temp[] vs, Code rest) {
    c = c.andThen(vs, rest);
    return this;
  }

  /**
   * Live variable analysis on a section of code; rewrites bindings v <- t using a wildcard, _ <- t,
   * if the variable v is not used in the following code.
   */
  Temps liveness() {
    Temps us = c.liveness();

    // Stub out any unused variables with a wildcard
    Temp[] nvs = null;
    for (int i = 0; i < vs.length; i++) {
      if (vs[i].isLive() && !vs[i].isIn(us)) {
        if (nvs == null) {
          nvs = new Temp[vs.length];
          for (int j = 0; j < i; j++) {
            nvs[j] = vs[j];
          }
        }
        MILProgram.report("liveness replaced " + vs[i] + " with a wildcard");
        nvs[i] = vs[i].notLive();
      }
    }
    if (nvs != null) {
      vs = nvs;
    }

    // Return the set of variables that are used in (vs <- t; c)
    return t.liveness(Temps.remove(vs, us));
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return t.summary() * 17 + c.summary() * 11 + 511;
  }

  /** Test to see if two Code sequences are alpha equivalent. */
  boolean alphaCode(Temps thisvars, Code that, Temps thatvars) {
    return that.alphaBind(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaBind(Temps thisvars, Bind that, Temps thatvars) {
    // Do the cheap test first, even if the order is counterintuitive:
    if (this.vs.length == that.vs.length && this.t.alphaTail(thisvars, that.t, thatvars)) {
      // TODO: in a binding  v, v <- b[...]; c, where the same variable name is bound twice on the
      // left of the <-,
      // which one should be in scope in c? That decision will impact the correctness of the
      // following code ...
      for (int i = 0; i < this.vs.length; i++) {
        thisvars = this.vs[i].add(thisvars);
        thatvars = that.vs[i].add(thatvars);
      }
      return this.c.alphaCode(thisvars, that.c, thatvars);
    }
    return false;
  }

  void eliminateDuplicates() {
    t.eliminateDuplicates();
    c.eliminateDuplicates();
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    Atom.collect(vs, set);
    t.collect(set);
    c.collect(set);
  }

  /** Simplify uses of constructor functions in this code sequence. */
  Code cfunSimplify() {
    t = t.removeNewtypeCfun();
    c = c.cfunSimplify();
    return this;
  }

  /** Generate a specialized version of this code sequence. */
  Code specializeCode(MILSpec spec, TVarSubst s, SpecEnv env) {
    Temp[] svs = Temp.specialize(s, vs);
    return new Bind(
        svs, t.specializeTail(spec, s, env), c.specializeCode(spec, s, new SpecEnv(vs, svs, env)));
  }

  Code bitdataRewrite(BitdataMap m) {
    return new Bind(vs, t.bitdataRewrite(m), c.bitdataRewrite(m));
  }

  Code repTransform(RepTypeSet set, RepEnv env) {
    Temp[][] nvss = Temp.reps(vs);
    Temp[] nvs = Temp.repParams(vs, nvss);
    return t.repTransform(set, env, nvs, c.repTransform(set, Temp.extend(vs, nvss, env)));
  }

  /** Find the argument variables that are used in this Code sequence. */
  Temps addArgs() throws Failure {
    return t.addArgs(Temps.remove(vs, c.addArgs()));
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    t.countCalls();
    c.countCalls();
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    t.countAllCalls();
    c.countAllCalls();
  }

  /**
   * Search this fragment of MIL code for tail calls, adding new blocks that should be included in
   * the code for a current function to the list bs.
   */
  Blocks identifyBlocks(Defn src, Blocks bs) {
    return c.identifyBlocks(src, bs);
  }

  /**
   * Determine whether this code sequence contains at most len Bind instructions before reaching a
   * Done. This is used as a heuristic for deciding whether it would be better to make a copy of the
   * code for a simple block than to incur the overhead of calling a single instance of the code as
   * a function.
   */
  boolean isSmall(int len) {
    return (len > 0) && c.isSmall(len - 1);
  }

  /** Find the CFG successors for this MIL code fragment. */
  Label[] findSuccs(CFG cfg, Node src) {
    return c.findSuccs(cfg, src);
  }

  /**
   * Generate LLVM code to execute this Code sequence as part of the given CFG. The TempSubst s is
   * used to capture renamings of MIL temporaries, and succs provides the successor labels for the
   * end of the code.
   */
  llvm.Code toLLVMCode(LLVMMap lm, VarMap vm, TempSubst s, Label[] succs) {
    return t.toLLVMBind(lm, vm, s, vs, false, c, succs);
  }
}
