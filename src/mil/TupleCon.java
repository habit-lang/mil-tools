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
import compiler.BuiltinPosition;
import core.*;
import java.io.PrintWriter;

public class TupleCon extends Tycon {

  private int arity;

  private TupleCon(int arity) {
    super(BuiltinPosition.pos, "TupleCon" + arity);
    this.arity = arity;
  }

  public Kind getKind() {
    return Kind.tuple(arity);
  }

  public int getArity() {
    return arity;
  }

  private static TupleCon[] tupleCache;

  public static TupleCon tuple(int n) {
    if (tupleCache == null) {
      tupleCache = new TupleCon[10];
    } else if (n >= tupleCache.length) {
      TupleCon[] newCache = new TupleCon[Math.max(n + 1, 2 * tupleCache.length)];
      for (int i = 0; i < tupleCache.length; i++) {
        newCache[i] = tupleCache[i];
      }
      tupleCache = newCache;
    } else if (tupleCache[n] != null) {
      return tupleCache[n];
    }
    // Code to update cache[arg] = ... will be appended here.
    return tupleCache[n] = new TupleCon(n);
  }

  /**
   * Find the arity of this tuple type (i.e., the number of components) or return (-1) if it is not
   * a tuple type. Parameter n specifies the number of arguments that have already been found; it
   * should be 0 for the initial call.
   */
  int tupleArity(Type[] tenv, int n) {
    return (n == arity) ? n : (-1);
  }

  void write(TypeWriter tw, int prec, int args) {
    if (args == arity) {
      tw.write("[");
      if (args > 0) {
        tw.pop().write(tw, TypeWriter.NEVER, 0);
        for (int i = 1; i < args; i++) {
          tw.write(", ");
          tw.pop().write(tw, TypeWriter.NEVER, 0);
        }
      }
      tw.write("]");
    } else if (args == 0) {
      tw.write(id);
    } else {
      applic(tw, prec, args, 0);
    }
  }

  /**
   * Print a definition for this type constructor using source level syntax. TODO: Find a more
   * appropriate place for this code ...
   */
  void dumpTypeDefinition(PrintWriter out) {
    /* do nothing */
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    return this;
  }

  /**
   * Rewrite the argument stack in the given RepTypeSet to account for a change of representation if
   * this is a tuple type with any arguments whose representation is changed. Returns the number of
   * arguments in the rewritten list, or (-1) if no change of representation is required.
   */
  int repTransform(RepTypeSet set, int args) {
    if (arity != args) { // Sanity check
      debug.Internal.error("repTransform on type, arity=" + arity + ", args=" + args);
    }

    // The first step is to calculate (in len) how many components will be required in the rewritten
    // tuple and, if rewrites are required in any of the components, to build a map of the
    // corresponding
    // representation vectors:
    Type[][] reps = null; // Look for changes in representation
    int len = 0;
    for (int i = 0; i < args; i++) {
      Type[] r = set.stackArg(i + 1).repCalc();
      if (r != null) {
        if (reps == null) {
          reps = new Type[args][];
        }
        reps[i] = r;
        len += r.length;
      } else {
        len++;
      }
    }

    if (reps == null) { // No change in representation for any component
      return (-1);
    }

    // If changes are required, then we package them together in a new tuple type of the specified
    // length:
    Type[] ts = set.pop(args); // Rewrite stack, removing old args and pushing new
    for (int i = args; 0 < i--; ) {
      Type[] us = reps[i];
      if (us == null) {
        set.push(ts[i]);
      } else {
        for (int k = us.length; 0 < k--; ) {
          set.push(us[k]);
        }
      }
    }
    return len;
  }

  /**
   * Test to determine if this type is a tuple of the form [t1,...,tn], returning either the
   * components of the tuple in an array, or null if there is no match. The argument is the number
   * of potential tuple components that have already been seen; the initial call should use 0 for
   * this argument.
   */
  Type[] tupleComponents(int n) {
    return (n == arity) ? new Type[n] : null;
  }

  /**
   * Calculate an LLVM type corresponding to (a canonical form of) a MIL type. The full
   * (canononical) type is passed in for reference as we unwind it on the underlying TypeSet stack.
   */
  llvm.Type toLLVMCalc(Type c, LLVMMap lm, int args) {
    if (arity != args) {
      debug.Internal.error("TupleCon toLLVM arity mismatch");
    }
    int nonUnits = 0; // count the number of non unit types
    int lastNon = 0; // position of last non unit
    for (int i = 0; i < arity; i++) {
      if (lm.stackArg(i + 1).nonUnit()) {
        nonUnits++;
        lastNon = i;
      }
    }
    if (nonUnits == 0) {
      return llvm.Type.vd;
    } else if (nonUnits == 1) { // only one?  then lastNon gives its position
      return lm.toLLVM(lm.stackArg(lastNon + 1));
    } else {
      llvm.Type[] tys = new llvm.Type[nonUnits];
      for (int i = 0, j = 0; j < nonUnits; i++) {
        Type t = lm.stackArg(i + 1);
        if (t.nonUnit()) {
          tys[j++] = lm.toLLVM(t);
        }
      }
      // Define a symbolic name for this type:
      llvm.DefinedType dt = new llvm.DefinedType(new llvm.StructType(tys));
      lm.typedef("corresponds to MIL tuple type " + c, dt);
      return dt;
    }
  }
}
