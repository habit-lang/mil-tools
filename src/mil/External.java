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

public class External extends TopDefn {

  private String id;

  private Scheme declared;

  private String ref;

  private Type[] ts;

  /** Default constructor. */
  public External(Position pos, String id, Scheme declared, String ref, Type[] ts) {
    super(pos);
    this.id = id;
    this.declared = declared;
    this.ref = ref;
    this.ts = ts;
  }

  private static int count = 0;

  public External(Position pos, Scheme declared, String ref, Type[] ts) {
    this(pos, "e" + count++, declared, ref, ts);
  }

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

  public String toString() {
    return id;
  }

  /** Find the list of Defns that this Defn depends on. */
  public Defns dependencies() {
    return null;
  }

  boolean dotInclude() {
    return false;
  }

  void displayDefn(PrintWriter out) {
    out.print("external " + id);
    if (ref != null) {
      out.print(" {" + ref);
      for (int i = 0; i < ts.length; i++) {
        out.print(" ");
        out.print(ts[i].toString(TypeWriter.ALWAYS));
      }
      out.print("}");
    }
    out.println(" :: " + declared);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return declared.instantiate();
  }

  /**
   * Set the initial type for this definition by instantiating the declared type, if present, or
   * using type variables to create a suitable skeleton. Also sets the types of bound variables.
   */
  void setInitialType() throws Failure {
    /* Nothing to do here */
  }

  /** Type check the body of this definition. */
  void checkBody(Handler handler) throws Failure {
    /* Nothing to do here */
  }

  void checkBody(Position pos) throws Failure {
    /* Nothing to do here */
  }

  /**
   * Calculate a generalized type for this binding, adding universal quantifiers for any unbound
   * type variable in the inferred type. (There are no "fixed" type variables here because all mil
   * definitions are at the top level.)
   */
  void generalizeType(Handler handler) throws Failure {
    /* nothing to do here */
  }

  void findAmbigTVars(Handler handler, TVars gens) {
    /* Nothing to do here */
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    handler.report(new Failure(pos, "Cannot access external symbol \"" + id + "\" from bytecode"));
  }

  /** Apply inlining. */
  public void inlining() {
    /* Nothing to do here */
  }

  /**
   * Count the number of unused arguments for this definition using the current unusedArgs
   * information for any other items that it references.
   */
  int countUnusedArgs() {
    return 0;
  }

  /** Rewrite this program to remove unused arguments in block calls. */
  void removeUnusedArgs() {
    /* Nothing to do here */
  }

  public void flow() {
    /* Nothing to do here */
  }

  /**
   * Compute a summary for this definition (if it is a block or top-level) and then look for a
   * previously encountered item with the same code in the given table. Return true if a duplicate
   * was found.
   */
  boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels) {
    return false;
  }

  void eliminateDuplicates() {
    /* Nothing to do here */
  }

  void collect() {
    /* Nothing to do here */
  }

  void collect(TypeSet set) {
    declared = declared.canonScheme(set);
    for (int i = 0; i < ts.length; i++) {
      ts[i] = ts[i].canonType(set);
    }
  }

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    /* Nothing to do here */
  }

  void printlnSig(PrintWriter out) {
    out.println("external " + id + " :: " + declared);
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  External isExternalOfType(Scheme inst) {
    return declared.alphaEquiv(inst) ? this : null;
  }

  External(External e) {
    this(e.pos, e.declared, e.ref, e.ts);
  }

  /** Handle specialization of Externals */
  void specialize(MILSpec spec, External eorig) {
    debug.Log.println(
        "External specialize: "
            + eorig
            + " :: "
            + eorig.declared
            + "  ~~>  "
            + this
            + " :: "
            + this.declared);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original.
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    throw new ExternalAsEntrypoint(this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
  }

  void repTransform(Handler handler, RepTypeSet set) {
    System.out.println("Looking at external " + getId() + " :: " + declared);
    declared = declared.canonType(set);
    System.out.println("Canon type is: " + declared);
    Type[] r = declared.repCalc(); // TODO: is this enough?  Do we need to use canonType()?
    if (r != null) { // Change of representation
      if (r.length == 1) {
        declared = r[0];
        System.out.println("Changed representation " + getId() + " :: " + declared);
      } else {
        debug.Internal.error(
            "cannot handle representation change for " + getId() + " :: " + declared);
      }
    }
    //  if (tail==null && (tail=generateTail(handler))==null) {
    // System.out.println("not code generated");
    //    declared = declared.canonScheme(set);
    System.out.println("representation " + getId() + " :: " + declared);
    //  } else {
    // System.out.println("tail generated!");
    //    super.repTransform(handler, set);
    //  }
  }

  private TopLevel impl = null;

  Atom[] repExt() {
    if (impl != null || (impl = generateTopLevel()) != null) {
      // TODO: should we check that declared.repCalc()!=null and has same length as tops?
      return impl.tops();
    }
    Type[] r = declared.repCalc();
    if (r != null && r.length != 1) {
      debug.Internal.error("Representation for external " + id + " does not use single word");
    }
    return null;
  }

  TopLevel generateTopLevel() {
    if (ref == null || ts == null) { // Do not generate code if ref or ts is missing
      return null;
    }

    // Use ref and ts to determine how we should generate a TopLevel for an external primitive

    //  if ("primBitFromLiteral".equals(ref) && ts.length>=2) {
    //    // expects: [literal value, wordsize, ...] in ts
    //    BigInteger lvalue = ts[0].getNat();
    //    BigInteger wordsz = ts[1].getNat();
    // System.out.println("generate succeed for " + ref);
    //    if (lvalue!=null && wordsz!=null && wordsz.intValue()==32) {
    //      Tail        t = new Return(new IntConst(lvalue.intValue()));
    //      ClosureDefn k = new ClosureDefn(null/*pos*/, Temp.noTemps, Temp.noTemps, t);
    // k.displayDefn();
    //      return new ClosAlloc(k).withArgs(Atom.noAtoms);
    //    }
    //  } else {
    //  }
    // TODO: should we report an error if there is no matching case?
    return null;
  }

  void addExport(MILEnv exports) {
    exports.addTop(id, new TopExt(this));
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  Temp[] addArgs() throws Failure {
    return null;
  }

  void countCalls() {
    /* Nothing to do here */
  }
}
