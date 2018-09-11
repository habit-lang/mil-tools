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

public abstract class Call extends Tail {

  /** The list of arguments for this call. */
  protected Atom[] args;

  /**
   * Set the arguments for this body. A typical use is new call.withArgs(args) to construct a Tail
   * representing a call with the specific arguments. In this way, we can specify the arguments at
   * the time of construction, but we also have flexibility to fix the arguments at some later point
   * instead.
   */
  public Call withArgs(Atom[] args) {
    this.args = args;
    return this;
  }

  public int getArity() {
    return args.length;
  }

  public Call withArgs() {
    return withArgs(Atom.noAtoms);
  }

  public Call withArgs(Atom a) {
    return withArgs(new Atom[] {a});
  }

  public Call withArgs(Atom a, Atom b) {
    return withArgs(new Atom[] {a, b});
  }

  public Call withArgs(Atom a, long n) {
    return withArgs(new Atom[] {a, new Word(n)});
  }

  public Call withArgs(long n, Atom b) {
    return withArgs(new Atom[] {new Word(n), b});
  }

  /** Test if this Tail expression includes a free occurrence of a particular variable. */
  public boolean contains(Temp w) {
    return args != null && w.occursIn(args);
  }

  /**
   * Test if this Tail expression includes an occurrence of any of the variables listed in the given
   * array.
   */
  public boolean contains(Temp[] ws) {
    return Atom.occursIn(args, ws);
  }

  /** Add the variables mentioned in this tail to the given list of variables. */
  public Temps add(Temps vs) {
    return Temps.add(args, vs);
  }

  /**
   * Test if the arguments for two Calls are the same. Either both argument lists are null, or else
   * both have the same list of Atoms.
   */
  public boolean sameArgs(Call that) {
    return (this.args == null)
        ? (that.args == null)
        : (that.args != null && Atom.sameAtoms(this.args, that.args));
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return Atom.dependencies(args, ds);
  }

  /**
   * Print a call with a notation that includes the name of the item that is being called and a list
   * of arguments, appropriately wrapped between a given open and close symbol (parentheses for
   * block, primitive, and data constructor calls; braces for closure constructors; and brackets for
   * monadic thunk constructors).
   */
  public static void dump(
      PrintWriter out, String name, String open, Atom[] args, String close, Temps ts) {
    out.print(name);
    if (args != null) {
      out.print(open);
      Atom.dump(out, args, ts);
      out.print(close);
    }
  }

  /**
   * Apply a TempSubst to this Tail. A call to this method, even if the substitution is empty, will
   * force the construction of a new Tail.
   */
  public Tail forceApply(TempSubst s) {
    return callDup(TempSubst.apply(args, s));
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  abstract Call callDup(Atom[] args);

  public Call returnUnit(Position pos, int n) {
    // Define a block:  b[x1,...,x_n] = [] <- call(x1,...,xn); return Unit
    Temp[] params = Temp.makeTemps(n);
    Code c =
        new Bind(Temp.noTemps, this.withArgs(params), new Done(new Return(Cfun.Unit.getTop())));
    return new BlockCall(new Block(pos, params, c));
  }

  public Call thunk(Position pos, int n) {
    // Define a closure: k{x1,...,xn} [] = call(x1,...,xn)
    Temp[] stored = Temp.makeTemps(n);
    ClosureDefn k = new ClosureDefn(pos, stored, Temp.noTemps, this.withArgs(stored));

    // Define a block: b[x1,...,xn] = v <- k{x1,...,xn}; Proc(v)
    stored = Temp.makeTemps(n);
    Temp v = new Temp();
    Code c = new Bind(v, new ClosAlloc(k).withArgs(stored), new Done(Cfun.Proc.withArgs(v)));
    return new BlockCall(new Block(pos, stored, c));
  }

  public Call maker(Position pos, int n) {
    Call call = this;
    for (; n > 0; n--) {
      // Define a closure:  k{x1,...x_n-1} [xn] = call(x1,...,xn)
      Temp[] params = Temp.makeTemps(n);
      Temp[] stored = new Temp[n - 1];
      for (int i = 0; i < n - 1; i++) {
        stored[i] = params[i];
      }
      Temp arg = params[n - 1];
      ClosureDefn k = new ClosureDefn(pos, stored, new Temp[] {arg}, call.withArgs(params));

      // Define a block:  b[x1,...,x_n-1] = v <- k{x1,...,x_n-1} ; Func(v)
      params = Temp.makeTemps(n - 1);
      Temp v = new Temp();
      Code c = new Bind(v, new ClosAlloc(k).withArgs(params), new Done(Cfun.Func.withArgs(v)));
      call = new BlockCall(new Block(pos, params, c));
    }
    return call.withArgs(Atom.noAtoms);
  }

  Type inferType(Position pos) throws Failure {
    return inferCallType(pos, Type.instantiate(args));
  }

  abstract Type inferCallType(Position pos, Type[] inputs) throws Failure;

  /**
   * Generate code for a Tail that appears as a regular call (i.e., in the initial part of a code
   * sequence). The parameter o specifies the offset for the next unused location in the current
   * frame; this will also be the first location where we can store arguments and results.
   */
  void generateCallCode(MachineBuilder builder, int o) {
    for (int i = 0; i < args.length; i++) {
      args[i].copyTo(o + i, builder);
    }
    invokeCall(builder, o);
  }

  abstract void invokeCall(MachineBuilder builder, int o);

  /**
   * Generate code for a Tail that appears in tail position (i.e., at the end of a code sequence).
   * The parameter o specifies the offset of the next unused location in the current frame. For
   * BlockCall and Enter, in particular, we can jump to the next function instead of doing a call
   * followed by a return.
   */
  void generateTailCode(
      MachineBuilder builder, int o) { // For Return, PrimCall, DataAlloc, and ClosAlloc
    generateTailArgs(builder);
    invokeCall(builder, 0);
    builder.retn();
  }

  /**
   * Generate code that loads the values specified by args in to the slots at the start of the
   * frame. This requires care to ensure that we do not overwrite frame slots whose values should be
   * moved to other positions first, even though this is unlikely to occur very often in practice
   * (because most blocks have more temporaries than return results).
   */
  void generateTailArgs(MachineBuilder builder) {
    // Calculate a representation of the assignments that will be needed as an array ns, where, for
    // each index i, ns[i] represents the assignment frameSlot[i] = frameSlot[ns[i]].  Note that we
    // use ns[i]==(-1) if the value to be stored in slot i does not come from another frame slot.
    int[] ns = new int[args.length];
    for (int i = 0; i < args.length; i++) {
      ns[i] = args[i].frameSlot(builder);
      if (ns[i] == i) { // eliminate trivial assignments: v_i := v_i;
        ns[i] = (-1);
      }
    }

    // Copy values from frame slots in to correct positions:
    for (int blocked = findCycle(builder, ns); blocked >= 0; ) {
      builder.load(ns[blocked]);
      ns[blocked] = (-1);
      int dst = blocked;
      blocked = findCycle(builder, ns);
      builder.store(dst);
    }

    // Add global items to the frame:
    for (int i = 0; i < args.length; i++) {
      args[i].copyGlobalTo(i, builder);
    }
  }

  /**
   * Generate code for as many as possible of the assignments in ns. This process terminates either
   * when it has generated code for all of the assignments (in which case the return value is (-1)),
   * or else when no further assignments are possible (because they all involve cycles, in which
   * case the return value is the index of one such cycle in ns).
   */
  private static int findCycle(MachineBuilder builder, int[] ns) {
    int blocked, emitted;
    do {
      emitted = 0;
      blocked = (-1);
      for (int i = 0; i < ns.length; i++) {
        if (ns[i] >= 0) {
          if (notASource(i, ns)) {
            builder.copy(ns[i], i);
            ns[i] = (-1);
            emitted++;
          } else {
            blocked = i;
          }
        }
      }
    } while (emitted > 0);
    return blocked;
  }

  /**
   * Test to see if slot i is a source of any of the assignments in ns: if it is, then we can't
   * assign a new value to slot i until those other assignments have been performed.
   */
  private static boolean notASource(int i, int[] ns) {
    for (int j = 0; j < ns.length; j++) {
      if (ns[j] == i) {
        return false;
      }
    }
    return true;
  }

  /**
   * Allocate new temporary variables for the parameters specified by an array of Call values. The
   * results are returned as an array with one element (an array of new Temporaries) for each of the
   * positions in the original Call array. The array that is returned is either null (if the list of
   * known calls only contains DataAllocs without arguments, in which case the original parameters
   * can be used without change), or else is an array tss[], one entry for each item in calls, such
   * that: tss[i]==null ===> keep parameter i from original definition otherwise ===> use the (zero
   * or more) parameters in tss[i] instead of the original parameter i
   */
  static Temp[][] makeTempsFor(Call[] calls) {
    Temp[][] tss = null;
    for (int i = 0; i < calls.length; i++) {
      if (calls[i] != null && calls[i].cfunNoArgs() == null) {
        if (tss == null) {
          tss = new Temp[calls.length][];
        }
        // Generate a set of new temporary parameters for an allocator, or eliminate the original
        // parameter
        // if the call is a Return with a known value.
        tss[i] =
            (calls[i].isAllocator() == null) ? Temp.noTemps : Temp.makeTemps(calls[i].getArity());
      }
    }
    return tss;
  }

  /** Test to determine whether two arrays of Calls have the same form in every element position. */
  public static boolean sameCallForms(Call[] cs, Call[] ds) {
    if (cs == null || ds == null || cs.length != ds.length) {
      return false;
    }
    for (int i = 0; i < cs.length; i++) {
      // Compare cs[i] and ds[i].  Both must be null, or they must have the same constructor to
      // continue
      // to the next i.
      if (cs[i] == null) {
        if (ds[i] != null) {
          return false;
        }
      } else if (ds[i] == null || !cs[i].sameCallForm(ds[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  abstract boolean sameCallForm(Call c);

  boolean sameReturnForm(Call that) {
    return false;
  }

  boolean sameEnterForm(Enter that) {
    return false;
  }

  boolean samePrimCallForm(PrimCall that) {
    return false;
  }

  boolean sameBlockCallForm(BlockCall that) {
    return false;
  }

  boolean sameClosAllocForm(ClosAlloc that) {
    return false;
  }

  boolean sameDataAllocForm(DataAlloc that) {
    return false;
  }

  Atom[] specializedArgs(Call[] calls) {
    // Compute the number of actual arguments that are needed:
    int len = 0;
    for (int i = 0; i < calls.length; i++) {
      if (calls[i] == null || calls[i].cfunNoArgs() != null) {
        ++len;
      } else if (calls[i].isAllocator() != null) {
        len += calls[i].getArity();
      }
    }

    // Fill in the actual arguments:
    Atom[] nargs = new Atom[len];
    int pos = 0;
    for (int i = 0; i < args.length; i++) {
      if (calls[i] == null || calls[i].cfunNoArgs() != null) {
        nargs[pos++] = args[i];
      } else if (calls[i].isAllocator() != null) {
        pos = calls[i].collectArgs(nargs, pos);
      }
    }

    // Return specialized list of arguments:
    return nargs;
  }

  int collectArgs(Atom[] result, int pos) {
    for (int j = 0; j < args.length; j++) {
      result[pos++] = args[j];
    }
    return pos;
  }

  protected Atom[] removeDuplicateArgs(int[] dups) {
    // TODO: why not store the number of dups in dups[0]?
    int numDups = 0;
    for (int i = 0; i < dups.length; i++) {
      if (dups[i] != 0) {
        numDups++;
      }
    }

    Atom[] newargs = new Atom[args.length - numDups];
    int j = 0;
    for (int i = 0; i < dups.length; i++) { // copy parameters that are not duplicates
      if (dups[i] == 0) {
        newargs[j++] = args[i];
      }
    }
    return newargs;
  }

  int[] hasDuplicateArgs() {
    int[] dups = null;
    for (int i = 1; i < args.length; i++) { // Note: starts at i=1
      for (int j = 0; j < i; j++) { // Did args[i] previously appear in an earlier position?
        if (args[j] == args[i]) {
          if (dups == null) { // Allocate dups array for first duplicate found
            dups = new int[args.length];
          }
          dups[i] = j + 1; // Record the duplicate (+1 because zero indicates no dup)
          break; // No need to search for further occurrences
        }
      }
    }
    return dups;
  }

  /**
   * Find the variables that are used in this Tail expression, adding them to the list that is
   * passed in as a parameter. Variables that are mentioned in BlockCalls or ClosAllocs are only
   * included if the corresponding flag in usedArgs is set; all of the arguments in other types of
   * Call (i.e., PrimCalls and DataAllocs) are considered to be "used".
   */
  Temps usedVars(Temps vs) {
    return Temps.add(args, vs);
  }

  /** Liveness analysis. TODO: finish this comment. */
  Temps liveness(Temps vs) {
    for (int i = 0; i < args.length; i++) {
      args[i] = args[i].shortTopLevel();
      vs = args[i].add(vs);
    }
    return vs;
  }

  public static void dump(PrintWriter out, Call[] calls) {
    out.print("{");
    for (int i = 0; i < calls.length; i++) {
      if (i > 0) {
        out.print(", ");
      }
      if (calls[i] == null) {
        out.print("-");
      } else {
        calls[i].dump(out, (Temps) null);
      }
    }
    out.print("}");
  }

  public static void dump(Call[] calls) {
    PrintWriter out = new PrintWriter(System.out);
    dump(out, calls);
    out.flush();
  }

  /** Calculate a summary value for the arguments in a call, starting with a given seed value. */
  int summary(int sum) {
    for (int i = 0; i < args.length; i++) {
      sum = 53 * sum + args[i].summary();
    }
    return sum;
  }

  /** Test to see if two Call expressions have alpha equivalent argument lists. */
  boolean alphaArgs(Temps thisvars, Call that, Temps thatvars) {
    if (this.args.length != that.args.length) {
      return false;
    }
    for (int i = 0; i < args.length; i++) {
      if (!this.args[i].alphaAtom(thisvars, that.args[i], thatvars)) {
        return false;
      }
    }
    return true;
  }

  /** Generate a specialized version of this Tail. */
  Tail specializeTail(MILSpec spec, TVarSubst s, SpecEnv env) {
    return this.specializeCall(spec, s, env).withArgs(Atom.specialize(spec, s, env, args));
  }

  /** Generate a specialized version of this Call. */
  abstract Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env);

  /**
   * Wrap this Call in a closure definition that assumes m stored arguments and n new arguments
   * (with the assumption that this call requires (m+n) arguments), returning a new Call for the
   * ClosureDefn (without a specified list of arguments) as the result.
   */
  public Call makeClosure(Position pos, int m, int n) {
    Temp[] stored = Temp.makeTemps(m);
    Temp[] args = Temp.makeTemps(n);
    return new ClosAlloc(
        new ClosureDefn(pos, stored, args, this.withArgs(Temp.append(stored, args))));
  }

  /**
   * From this call, which requires n MIL arguments, construct the closure structures for a unary
   * function that takes m word values (that together represent the argument to the function).
   */
  Tail makeUnaryFuncClosure(Position pos, int m) {
    return makeClosure(pos, 0, m) // k0{} [a1,..,am] = ...(a1,..,am)
        .withArgs();
  }

  /**
   * From this call, which requires m+n MIL arguments, construct the closure structures for a binary
   * function that takes m word values (representing the first argument to the function) and then n
   * word values (representing the second argument).
   */
  Tail makeBinaryFuncClosure(Position pos, int m, int n) {
    return makeClosure(pos, m, n) //    k0{a1,..,am} [b1,...,bn] = ...(a1,...,am,b1,...,bn)
        .makeUnaryFuncClosure(pos, m);
  }

  /**
   * From this call, which requires m+n+p MIL arguments, construct the closure structures for a
   * ternary function that takes m word values (representing the first argument), then n words
   * (representing the second argument), and then p words (representing the third argument).
   */
  Tail makeTernaryFuncClosure(Position pos, int m, int n, int p) {
    return makeClosure(pos, m + n, p) //    k0{a1,...,b1,...} [c1,...] = ...(a1,...,b1,....,c1,...)
        .makeBinaryFuncClosure(pos, m, n);
  }

  Tail repTransform(RepTypeSet set, RepEnv env) {
    return callDup(Atom.repArgs(set, env, args));
  }
}
