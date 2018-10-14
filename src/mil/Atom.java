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
import core.*;
import java.io.PrintWriter;

/**
 * Represents basic atoms in a MIL program, each of which is either a variable or an integer
 * literal.
 */
public abstract class Atom {

  public static final Atom[] noAtoms = new Atom[0];

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
  public static String toString(Atom[] as) {
    return toString(as, null);
  }

  public static String toString(Atom[] as, Temps ts) {
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

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public abstract boolean sameAtom(Atom that);

  /** Test to determine whether this Atom refers to the given Word value. */
  public boolean sameWord(Word c) {
    return false;
  }

  /** Test to determine whether this Atom refers to the specified bit vector constant. */
  public boolean sameBits(Bits c) {
    return false;
  }

  /** Test to determine whether this Atom refers to the given flag constant. */
  public boolean sameFlag(Flag c) {
    return false;
  }

  /** Test to determine whether this Atom refers to the ith TopLhs in the given TopLevel. */
  boolean sameTopDef(TopLevel topLevel, int i) {
    return false;
  }

  /** Test to determine whether this Atom refers to the specified External. */
  boolean sameTopExt(External external) {
    return false;
  }

  /** Test to determine whether this Atom refers to the specified Area. */
  boolean sameTopArea(Area area) {
    return false;
  }

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

  /** Test to determine whether this Atom is an integer constant (or not). */
  public Word isWord() {
    return null;
  }

  /** Test to determine whether this Atom is a flag constant (or not). */
  public Flag isFlag() {
    return null;
  }

  /** Test for an occurrence of this variable in the given array of atoms. */
  public boolean occursIn(Atom[] as) {
    return false;
  }

  /** Test to see if any of the items in bs one array appear in a give array of atoms. */
  public static boolean occursIn(Atom[] bs, Atom[] as) {
    for (int i = 0; i < bs.length; i++) {
      if (bs[i].occursIn(as)) {
        return true;
      }
    }
    return false;
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

  /** Test to determine whether this atom appears in the given list of Temps. */
  boolean isIn(Temps vs) {
    return false;
  }

  /**
   * Add this atom as an argument variable to the given list; only local variables and temporaries
   * are treated as argument variables because wildcards are ignored and all other atoms can be
   * accessed as constants.
   */
  public Temps add(Temps vs) {
    return vs;
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return ds;
  }

  /** Find the list of Defns that this array of Atoms depends on. */
  public static Defns dependencies(Atom[] args, Defns ds) {
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        ds = args[i].dependencies(ds);
      }
    }
    return ds;
  }

  /** Print an array of atoms, separated by commas. */
  public static void dump(PrintWriter out, Atom[] args, Temps ts) {
    int n = 0; // count number of items displayed so far
    for (int i = 0; i < args.length; i++) {
      if (n++ > 0) {
        out.print(", ");
      }
      out.print(args[i].toString(ts));
    }
  }

  /**
   * Print an array of atoms, separated by commas and enclosed in parentheses if the number of atoms
   * is not 1.
   */
  public static void displayTuple(PrintWriter out, Atom[] args, Temps ts) {
    if (args != null && args.length == 1) {
      out.print(args[0].toString(ts));
    } else {
      out.print("[");
      Atom.dump(out, args, ts);
      out.print("]");
    }
  }

  /**
   * Extend the given substitution with a mapping from this Temp to the specified Atom. If this is
   * an Atom but not a Temp, then just return the input substitution without modifications.
   */
  public TempSubst mapsTo(Atom a, TempSubst s) {
    return s;
  }

  /** Apply a TempSubst to this Atom. */
  public abstract Atom apply(TempSubst s);

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public abstract Type instantiate();

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

  boolean isLive() {
    return false;
  }

  Temp notLive() {
    debug.Internal.error("notLive should only be called on a Temp");
    return null;
  }

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
   * A simple test for MIL code fragments that return a known Flag, returning either the constant or
   * null.
   */
  Flag returnsFlag() {
    return null;
  }

  /**
   * Add a fact to indicate that we know which constructor will be used for this Atom, but not what
   * its arguments will be. (Unless the constructor has zero arity.)
   */
  Facts addFact(Cfun cf, Facts facts) {
    return facts;
  }

  /**
   * Special case treatment for top-level bindings of the form [...,x,...] <- return [...,y,...]; we
   * want to short out such bindings whenever possible by replacing all occurrences of x with y.
   */
  Atom shortTopLevel() {
    return this;
  }

  /**
   * Test to determine if this Atom is known to hold a specific function value, represented by a
   * ClosAlloc/closure allocator, according to the given set of facts.
   */
  ClosAlloc lookForClosAlloc(Facts facts) {
    return null;
  }

  public Tail entersTopLevel(Atom[] iargs) {
    return null;
  }

  /**
   * Test to determine if this Atom is known to hold a specific data value represented by a
   * DataAlloc allocator, according to the given set of facts.
   */
  DataAlloc lookForDataAlloc(Facts facts) {
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
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

  Atom isKnown() {
    return null;
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  abstract void collect(TypeSet set);

  static void collect(Atom[] args, TypeSet set) {
    for (int i = 0; i < args.length; i++) {
      args[i].collect(set);
    }
  }

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
