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

/** Represents a left hand side (lhs) of a top-level variable definition. */
public class TopLhs {

  private String id;

  /** Default constructor. */
  public TopLhs(String id) {
    this.id = id;
  }

  /** Each left hand side is assigned a distinct name of the form sN for some numeric suffix N. */
  private static int count = 0;

  public TopLhs() {
    this("s" + count++);
  }

  /** Holds a declared or inferred type for a top-level lhs. */
  private Scheme declared;

  /** Holds the type of the defining occurrence of a top-level lhs. */
  private Type defining;

  /** Get the declared type, or null if no type has been set. */
  public Scheme getDeclared() {
    return declared;
  }

  /** Set the declared type. */
  public void setDeclared(Scheme declared) {
    this.declared = declared;
  }

  /** Return the identifier that is associated with this definition. */
  public String getId() {
    return id;
  }

  /** Display a header for this definition. */
  void dumpHeader(PrintWriter out, boolean isEntrypoint) {
    if (declared != null) {
      if (isEntrypoint) {
        out.print("entrypoint ");
      }
      out.println(id + " :: " + declared);
    }
  }

  /**
   * Return a type for an instantiated version of this item when used as an Atom (input operand).
   */
  public Type instantiate() {
    return (declared != null) ? declared.instantiate() : defining;
  }

  Type setInitialType() {
    defining = (declared != null) ? declared.instantiate() : new TVar(Tyvar.star);
    return defining;
  }

  /** Check that there are declared types for all of the items defined here. */
  boolean allTypesDeclared() {
    return declared != null;
  }

  void generalizeLhsType(Position pos, Handler handler, TVars gens, TVar[] generics)
      throws Failure {
    // defining will be null if previous stage of type checker failed
    if (defining != null) {
      debug.Log.println(
          "Generalizing definition for: " + id + " with generics " + TVar.show(generics));
      Scheme inferred = defining.generalize(generics);
      debug.Log.println("Inferred " + id + " :: " + inferred);
      if (declared != null && !declared.alphaEquiv(inferred)) {
        throw new Failure(
            pos,
            "Declared type \""
                + declared
                + "\" for \""
                + id
                + "\" is more general than inferred type \""
                + inferred
                + "\"");
      } else {
        declared = inferred;
      }
      // search for ambiguous type variables ...
      findAmbigTVars(handler, gens);
    }
  }

  void findAmbigTVars(Handler handler, TVars gens) {}

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
    if (defining != null) {
      defining = defining.canonType(set);
    }
  }

  /** Worker function for MILSpec.dump(): Print a signature line for this definition. */
  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
  }

  /** Create an array of new TopLhs objects for a specialized TopLevel. */
  static TopLhs[] makeLhs(TopLhs[] lhs, int n) {
    if (n == 0 && lhs.length == 1) {
      return new TopLhs[] {new TopLhs(lhs[0].id)};
    } else {
      TopLhs[] nlhs = new TopLhs[lhs.length];
      for (int i = 0; i < lhs.length; i++) {
        nlhs[i] = new TopLhs();
      }
      return nlhs;
    }
  }

  static void copyIds(TopLhs[] newLhs, TopLhs[] oldLhs) {
    for (int i = 0; i < oldLhs.length; i++) {
      newLhs[i].id = oldLhs[i].id;
    }
  }

  /** Return a substitution that can instantiate this Lhs to the given type. */
  TVarSubst specializingSubst(TVar[] generics, Type inst) {
    return declared.specializingSubst(generics, inst);
  }

  /** Set the type of this specialized TopLhs to the appropriate instance of the defining type. */
  void specialize(TopLhs lorig, TVarSubst s) {
    this.declared = lorig.defining.apply(s);
  }

  /**
   * Calculate an array of representation vectors for the variables introduced in a top-level
   * definition. Individual entries may be set to null to indicate that there is no change of
   * representation for that component. A single null result, rather than an array, indicates that
   * there are no changes of representation for any components.
   */
  static Type[][] reps(TopLhs[] lhs) {
    Type[][] reps = null;
    for (int i = 0; i < lhs.length; i++) {
      Type[] r = lhs[i].declared.repCalc(null);
      if (r != null) {
        if (reps == null) {
          reps = new Type[lhs.length][];
        }
        reps[i] = r;
      }
    }
    return reps;
  }

  /**
   * Rewrite the given TopLevel with this as its only left hand side, to replace the definition of a
   * (possibly curried) function value (involving ->>) with an (uncurried) MIL Block.
   */
  Defn makeEntryBlock(Position pos, TopLevel tl) {
    Block b = declared.liftToBlock0(pos, id, tl);
    if (b != null) {
      // Use the same entrypoint flag as the original, but rename the original
      // entrypoint, and clear its flag:
      b.setIsEntrypoint(tl.isEntrypoint());
      id = id + "_impl";
      tl.setIsEntrypoint(false);
      return b;
    }
    return tl;
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    declared = declared.canonScheme(set);
  }

  public void setDeclared(Position pos, Scheme scheme) throws Failure {
    if (declared != null) {
      throw new Failure(pos, "Multiple type annotations for \"" + id + "\"");
    }
    declared = scheme;
  }

  void addExport(MILEnv exports, TopDef t) {
    exports.addTop(id, t);
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return defining.nonUnit();
  }

  static boolean hasNonUnits(TopLhs[] lhs) {
    for (int i = 0; i < lhs.length; i++) {
      if (lhs[i].nonUnit()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a global variable definition for a component of a TopLevel definition that is
   * initialized to the specified value.
   */
  llvm.GlobalVarDefn globalVarDefn(LLVMMap lm, int mods, llvm.Value val) {
    return new llvm.GlobalVarDefn(mods, id, val, 0);
  }

  /**
   * A variation of globalVarDefn that is used for variables whose value will be calculated at
   * runtime; in this case, we set the initial value to a simple default that is appropriate to the
   * type of the variable.
   */
  llvm.GlobalVarDefn globalVarDefn(LLVMMap lm, int mods) {
    return globalVarDefn(lm, mods, lm.toLLVM(defining).defaultValue());
  }

  /** Make a new temporary to hold a value for this left hand side. */
  Temp makeTemp() {
    return new Temp(defining);
  }
}
