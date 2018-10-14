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
import compiler.Handler;
import compiler.Position;
import core.*;
import java.io.PrintWriter;

public class TopLhs {

  private String id;

  /** Default constructor. */
  public TopLhs(String id) {
    this.id = id;
  }

  private static int count = 0;

  public TopLhs() {
    this("s" + count++);
  }

  private Scheme declared;

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

  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    if (declared != null) {
      if (isEntrypoint) {
        out.print("entrypoint ");
      }
      out.println(id + " :: " + declared);
    }
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return declared.instantiate();
  }

  Type setInitialType() {
    return defining = (declared != null) ? declared.instantiate() : new TVar(Tyvar.star);
  }

  /** Check that there are declared types for all of the items defined here. */
  boolean allTypesDeclared() {
    return declared != null;
  }

  void generalizeLhsType(Position pos, Handler handler, TVars gens, TVar[] generics)
      throws Failure {
    if (defining != null) {
      debug.Log.println(
          "Generalizing definition for: " + getId() + " with generics " + TVar.show(generics));
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
      findAmbigTVars(handler, gens); // search for ambiguous type variables ...
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

  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

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

  /** Set the type of this specialized TopLhs to the appropriate instance of the defining type. */
  void specialize(TopLhs lorig, TVarSubst s) {
    this.declared = lorig.defining.apply(s);
  }

  /** Return a substitution that can instantiate this Lhs to the given type. */
  TVarSubst specializingSubst(TVar[] generics, Type inst) {
    return declared.specializingSubst(generics, inst);
  }

  static void copyIds(TopLhs[] newLhs, TopLhs[] oldLhs) {
    for (int i = 0; i < oldLhs.length; i++) {
      newLhs[i].id = oldLhs[i].id;
    }
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
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
      Type[] r = lhs[i].declared.repCalc();
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
      b.setIsEntrypoint(tl.isEntrypoint()); // Use the same entrypoint flag as the original ...
      id = id + "_impl"; // ... rename the original entrypoint ...
      tl.setIsEntrypoint(false); // ... and clear the flag for the original entrypoint.
      return b;
    }
    return tl;
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    declared = declared.canonScheme(set);
  }

  public void setDeclared(Handler handler, Position pos, Scheme scheme) {
    if (declared != null) {
      handler.report(new Failure(pos, "Multiple type annotations for \"" + id + "\""));
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
