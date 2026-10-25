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
 * Represents basic atoms (operands) in a MIL program, each of which is either a constant or a
 * (top-level or temporary) variable.
 */
public abstract class Atom {

  /** Test whether this Atom is an Word. */
  public Word isWord() {
    return null;
  }

  /** Test whether this Atom is an Flag. */
  public Flag isFlag() {
    return null;
  }

  /**
   * Test to see if a temporary has had its name replaced with an underscore to mark it as unused.
   */
  boolean markedUnused() {
    return false;
  }

  /**
   * Mark a temporary to indicate that its value is not referenced in the remainder of a given code
   * sequence by setting its name to an underscore.
   */
  Temp markUnused() {
    debug.Internal.error("markUnused should only be called on a Temp");
    return null;
  }

  /** Build a new array that combines the elements from the left array with those from the right. */
  public static Atom[] append(Atom[] left, Atom[] right) {
    int l = left.length;
    if (l == 0) {
      return right;
    }
    int r = right.length;
    if (r == 0) {
      return left;
    }
    Atom[] n = new Atom[l + r];
    for (int i = 0; i < l; i++) {
      n[i] = left[i];
    }
    for (int i = 0; i < r; i++) {
      n[l + i] = right[i];
    }
    return n;
  }

  /** A constant holding an empty array of Atom values. */
  public static Atom[] noAtoms = new Atom[0];

  /** Find the dependencies of this AST fragment. */
  Defns dependencies(Defns ds) {
    return ds;
  }

  /** Test whether this atom appears in the given list of Temps. */
  boolean isIn(Temps vs) {
    return false;
  }

  /** Add the temporaries used in this node to the given list of Temps. */
  Temps add(Temps vs) {
    return vs;
  }

  /** Generate a printable description of this atom. */
  public abstract String toString();

  /**
   * Generate a printable description of this atom with a renaming of Temp values that is specified
   * by the list ts.
   */
  String toString(Temps ts) {
    return toString();
  }

  /**
   * Generate a printable description of the given list of atoms as a comma separated list of the
   * individual atoms.
   */
  static String toString(Atom[] as) {
    return toString(as, null);
  }

  /**
   * Generate a printable description of the atoms in the array as a comma separated list with the
   * renaming of Temp values specified by ts.
   */
  static String toString(Atom[] as, Temps ts) {
    if (as == null || as.length == 0) { // An empty list?
      return "";
    } else if (as.length == 1) { // A single atom?
      return as[0].toString(ts);
    } else { // General case
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < as.length; i++) {
        if (i > 0) {
          buf.append(", ");
        }
        buf.append(as[i].toString(ts));
      }
      return buf.toString();
    }
  }

  /** Print an array of atoms, separated by commas. */
  static void dump(PrintWriter out, Atom[] args, Temps ts) {
    int n = 0; // count number of items displayed so far
    for (int i = 0; i < args.length; i++) {
      if (n++ > 0) {
        out.print(", ");
      }
      out.print(args[i].toString(ts));
    }
  }

  /**
   * Print an array of atoms, separated by commas and enclosed in parentheses unless there is
   * exactly one atom.
   */
  static void displayTuple(PrintWriter out, Atom[] args, Temps ts) {
    if (args != null && args.length == 1) {
      out.print(args[0].toString(ts));
    } else {
      out.print("[");
      Atom.dump(out, args, ts);
      out.print("]");
    }
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  abstract boolean sameAtom(Atom that);

  /** Test whether this Atom is the same as the given Word. */
  boolean sameWord(Word that) {
    return false;
  }

  /** Test whether this Atom is the same as the given Bits. */
  boolean sameBits(Bits that) {
    return false;
  }

  /** Test whether this Atom is the same as the given Flag. */
  boolean sameFlag(Flag that) {
    return false;
  }

  /** Test whether this Atom is the same as the given ProxyNat. */
  boolean sameProxyNat(ProxyNat that) {
    return false;
  }

  /** Test whether this Atom is the same as the given ProxyLab. */
  boolean sameProxyLab(ProxyLab that) {
    return false;
  }

  /** Test whether this Atom is the same as the given Temp. */
  boolean sameTemp(Temp that) {
    return false;
  }

  /** Test whether this Atom is the same as the given TopDef. */
  boolean sameTopDef(TopDef that) {
    return false;
  }

  /** Test whether this Atom is the same as the given TopExt. */
  boolean sameTopExt(TopExt that) {
    return false;
  }

  /** Test whether this Atom is the same as the given TopArea. */
  boolean sameTopArea(TopArea that) {
    return false;
  }

  /** Test whether two arrays of Atom values are the same. */
  static boolean sameAtoms(Atom[] as, Atom[] bs) {
    if (as.length != bs.length) {
      return false;
    }
    for (int i = 0; i < as.length; i++) {
      if (!as[i].sameAtom(bs[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Extend the given substitution with a mapping from this Temp to the specified Atom. If this is
   * an Atom but not a Temp, then just return the input substitution without modifications.
   */
  TempSubst mapsTo(Atom a, TempSubst s) {
    return s;
  }

  /** Apply a TempSubst to this Atom. */
  abstract Atom apply(TempSubst s);

  /**
   * Return a type for an instantiated version of this item when used as an Atom (input operand).
   */
  public abstract Type instantiate();

  /** Type check the implementation for an external. */
  void checkImp(Handler handler, Position pos) {
    /* nothing to do */
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  abstract void copyTo(int dst, MachineBuilder builder);

  /** Generate code to load the data for this atom into the value register. */
  abstract void load(MachineBuilder builder);

  /**
   * Return the frame slot corresponding to this atom, or (-1) if there is no associated frame slot.
   */
  int frameSlot(MachineBuilder builder) {
    return (-1);
  }

  /**
   * Generate code that will load the value of a global atom into a frame slot; if the atom is a
   * Temp instead, then we do not generate any code.
   */
  void copyGlobalTo(int dst, MachineBuilder builder) {
    copyTo(dst, builder);
  }

  /** Test for an occurrence of this variable in the given array of atoms. */
  boolean occursIn(Atom[] as) {
    return false;
  }

  /** Test if any of the items in one array (bs) appear in another array (as). */
  static boolean occursIn(Atom[] bs, Atom[] as) {
    for (int i = 0; i < bs.length; i++) {
      if (bs[i].occursIn(as)) {
        return true;
      }
    }
    return false;
  }

  /** Determine whether this Atom has a known (static) value. */
  boolean isStatic() {
    return false;
  }

  public Facts addCfunFact(Cfun cf, Facts facts) {
    return facts;
  }

  public Tail lookupFact(Facts facts) {
    return null;
  }

  /**
   * A simple test for MIL code fragments that return a known Flag; this is a helper for the method
   * without an argument that adds a counter that is decreased for every BlockCall encountered to
   * avoid triggering an unbounded loop in pathological corner cases.
   */
  Flag returnsFlag(int n) {
    return null;
  }

  /**
   * Add a fact to indicate that we know which constructor will be used for this Atom, but not what
   * its arguments will be. (Unless the constructor has zero arity.)
   */
  Facts addFact(Cfun cf, Facts facts) {
    return facts;
  }

  Atom isKnown(boolean allowWord) {
    return null;
  }

  /**
   * Special case treatment for top-level bindings of the form [...,x,...] <- return [...,y,...]; we
   * want to short out such bindings whenever possible by replacing all occurrences of x with y.
   */
  Atom shortTopLevel() {
    return this;
  }

  /**
   * Test if this Atom is known to hold a specific function value, represented by a
   * ClosAlloc/closure allocator, according to the given set of facts.
   */
  ClosAlloc lookForClosAlloc(Facts facts) {
    return null;
  }

  public Tail entersTopLevel(Atom[] iargs) {
    return null;
  }

  /**
   * Test whether this Atom is known to hold a specific data value represented by a DataAlloc
   * allocator, according to the given set of facts.
   */
  DataAlloc lookForDataAlloc(Facts facts) {
    return null;
  }

  /**
   * Update the sources arrays to indicate that we have found a use of this Atom as the ith argument
   * in a call to b as part of the definition d.
   */
  void updateSources(Defn d, Temp[] params, Temps rebound, Defn b, int i) {
    b.setSource(i, Src.any);
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
   */
  int summary() {
    return -17;
  }

  /** Test to see if two atoms are the same upto alpha renaming. */
  boolean alphaAtom(Temps thisvars, Atom that, Temps thatvars) {
    return this.sameAtom(that);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaTemp(Temps thisvars, Temp that, Temps thatvars) {
    return false;
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  /** Return an array of atoms that stores canonical versions of each atom in the input list. */
  static void collect(Atom[] args, TypeSet set) {
    for (int i = 0; i < args.length; i++) {
      args[i].collect(set);
    }
  }

  /**
   * Determine the specialized version of an Atom in a context described by a given @TVarSubst@
   * and @SpecEnv@.
   */
  abstract Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env);

  /** Generate a specialized list of source atoms from the given list. */
  static Atom[] specialize(MILSpec spec, TVarSubst s, SpecEnv env, Atom[] ss) {
    Atom[] nss = new Atom[ss.length];
    for (int i = 0; i < ss.length; i++) {
      nss[i] = ss[i].specializeAtom(spec, s, env);
    }
    return nss;
  }

  /** Return the representation vector for this Atom. */
  abstract Type[] repCalc();

  static Atom[] repArgs(RepTypeSet set, RepEnv env, Atom[] args) {
    // Analyze the argument list for possible representation changes:
    Atom[][] reps = null;
    int len = 0;
    for (int i = 0; i < args.length; i++) {
      Atom[] as = args[i].repArg(set, env);
      if (as != null) {
        if (reps == null) {
          reps = new Atom[args.length][];
        }
        reps[i] = as;
        len += as.length;
      } else {
        len++;
      }
    }

    // Use the original argument list if there are no changes:
    if (reps == null) {
      return args;
    }

    // Make the new list of parameters:
    Atom[] nargs = new Atom[len];
    int j = 0;
    for (int i = 0; i < reps.length; i++) {
      if (reps[i] == null) { // No representation change for this argument
        nargs[j++] = args[i];
      } else { // This argument's representation changes
        Atom[] as = reps[i];
        for (int k = 0; k < as.length; k++) {
          nargs[j++] = as[k];
        }
      }
    }
    if (j != nargs.length) {
      debug.Internal.error("Atom.repArgs mismatch");
    }
    return nargs;
  }

  abstract Atom[] repArg(RepTypeSet set, RepEnv env);

  /**
   * Representation transformation on a single Atom, always returns a non-null array as its result,
   * even if there is no change of representation (in which case the result is just a singleton
   * array that packages up the original Atom).
   */
  Atom[] repAtom(RepTypeSet set, RepEnv env) {
    Atom[] as = repArg(set, env);
    return (as == null) ? new Atom[] {this} : as;
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  abstract boolean nonUnit();

  /**
   * Filter all unit values from this array producing either a new (shorter) array, or just
   * returning the original array if all of the elements are non-units.
   */
  static Atom[] nonUnits(Atom[] xs) {
    int nonUnits = 0; // count number of non unit components
    for (int i = 0; i < xs.length; i++) {
      if (xs[i].nonUnit()) {
        nonUnits++;
      }
    }
    if (nonUnits >= xs.length) { // all components are non unit
      return xs; // so there is no change
    }
    Atom[] nxs = new Atom[nonUnits]; // make array with just the non units
    for (int i = 0, j = 0; j < nonUnits; i++) {
      if (xs[i].nonUnit()) {
        nxs[j++] = xs[i];
      }
    }
    return nxs;
  }

  /**
   * Calculate a static value for this atom, or return null if the result must be determined at
   * runtime.
   */
  llvm.Value calcStaticValue() {
    return null;
  }

  /** Calculate a sequence of LLVM values corresponding to an array of MIL arguments. */
  static llvm.Value[] toLLVMValues(LLVMMap lm, VarMap vm, TempSubst s, Atom[] args) {
    Atom[] nuargs = Atom.nonUnits(args);
    llvm.Value[] vals = new llvm.Value[args.length];
    for (int i = 0; i < args.length; i++) {
      vals[i] = args[i].toLLVMAtom(lm, vm, s);
    }
    return vals;
  }

  /**
   * Calculate an LLVM Value corresponding to a given MIL argument, prior to having applied the
   * given substitution.
   */
  llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm, TempSubst s) {
    return this.apply(s).toLLVMAtom(lm, vm);
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  abstract llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm);
}
