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

public class External extends TopDefn {

  private String id;

  private Scheme declared;

  private ExtImp imp;

  /** Default constructor. */
  public External(Position pos, String id, Scheme declared, ExtImp imp) {
    super(pos);
    this.id = id;
    this.declared = declared;
    this.imp = imp;
  }

  private static int count = 0;

  public External(Position pos, Scheme declared, ExtImp imp) {
    this(pos, "e" + count++, declared, imp);
  }

  public External(Position pos, String id, Scheme declared) {
    this(pos, id, declared, new ExtImp());
  }

  public void setImp(ExtImp imp) {
    this.imp = imp;
  }

  /**
   * Return references to all components of this top level definition in an array of
   * atoms/arguments.
   */
  Atom[] tops() {
    return new TopExt[] {new TopExt(this)};
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
    return imp.dependencies();
  }

  boolean dotInclude() {
    return false;
  }

  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    if (isEntrypoint) {
      out.print("export ");
    }
    out.print("external " + id);
    StringTypeWriter tw = new StringTypeWriter(declared.getPrefix());
    imp.dump(out, tw);
    out.println(" :: " + declared.toString(TypeWriter.NEVER, tw));
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

  /**
   * Type check the body of this definition, but reporting rather than throwing an exception error
   * if the given handler is not null.
   */
  void checkBody(Handler handler) throws Failure {
    imp.checkImp(handler);
  }

  /** Type check the body of this definition, throwing an exception if there is an error. */
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
   * Compute a summary for this definition (if it is a block, top-level, or closure) and then look
   * for a previously encountered item with the same code in the given table. Return true if a
   * duplicate was found.
   */
  boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels, ClosureDefns[] closures) {
    return false;
  }

  void eliminateDuplicates() {
    /* Nothing to do here */
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
    imp.collect(set);
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

  External(External e, int num) {
    this(e.pos, mkid(e.id, num), e.declared, e.imp);
  }

  /** Handle specialization of Externals. */
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
    Type[] tenv = eorig.declared.getPrefix().instantiate();
    if (!eorig.declared.getType().match(tenv, (Type) this.declared, null)) {
      debug.Internal.error("Could not match " + eorig.declared + " with " + this.declared);
    }
    imp = imp.specialize(spec, tenv);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    Type t = declared.isMonomorphic();
    if (t != null) {
      External e = spec.specializedExternal(this, t);
      e.id = this.id; // use the same name as in the original program
      return e;
    }
    throw new PolymorphicEntrypointFailure("external", this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
    imp.canonDeclared(spec);
  }

  void bitdataRewrite(BitdataMap m) {
    /* Nothing to do here */
  }

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    declared = declared.canonType(set);
    debug.Log.println("Determining representation for external " + id + " :: " + declared);
    try {
      impl = imp.repImplement(handler, this, declared.repCalc(), set);
    } catch (Failure f) {
      handler.report(f);
    }
  }

  private TopDefn impl = null;

  Atom[] repExt() {
    return (impl == null) ? null : impl.tops();
  }

  TopDefn generatePrim(Type[] reps) throws Failure {
    return generatePrim(id, reps);
  }

  TopDefn generatePrim(String id, Type[] reps) throws Failure {
    if (reps == null) {
      return generatePrim(id, declared);
    } else if (reps.length == 1) {
      return generatePrim(id, reps[0]);
    } else {
      TopLhs[] lhs = new TopLhs[reps.length];
      Atom[] rhs = new Atom[reps.length];
      for (int i = 0; i < reps.length; i++) {
        External ext = new External(pos, mkid(id, i), reps[i]);
        ext.setIsEntrypoint(isEntrypoint);
        rhs[i] = new TopExt(ext);
        lhs[i] = id.equals(this.id) ? new TopLhs() : new TopLhs(mkid(id, i));
      }
      impl = new TopLevel(pos, lhs, new Return(rhs));
      impl.setIsEntrypoint(isEntrypoint && !id.equals(this.id));
      return impl;
    }
  }

  private TopDefn generatePrim(String id, Scheme declared) throws Failure {
    Tail t = declared.generatePrim(pos, id);
    if (t != null) {
      TopLhs lhs = new TopLhs(this.id);
      lhs.setDeclared(declared);
      impl = new TopLevel(pos, new TopLhs[] {lhs}, t);
      if (isEntrypoint && id.equals(this.id)) { // test delayed until impl has been initialized
        throw new Failure(
            pos,
            "External "
                + id
                + " is implemented by a primitive of the same"
                + " name so cannot be declared as an entrypoint.");
      }
    } else {
      External ext = new External(pos, id, declared);
      if (id.equals(this.id)) {
        impl = ext;
      } else {
        TopLhs lhs = new TopLhs(this.id);
        lhs.setDeclared(declared);
        impl = new TopLevel(pos, new TopLhs[] {lhs}, new Return(new TopExt(ext)));
      }
    }
    impl.setIsEntrypoint(isEntrypoint);
    return impl;
  }

  /**
   * Rewrite this definition, replacing TopLevels that introduce curried function values with
   * corresponding uncurried blocks. No changes are made to other forms of definition.
   */
  Defn makeEntryBlock() {
    return (impl == null) ? this : impl.makeEntryBlock();
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    /* Processing of External definitions was completed during the first pass. */
  }

  /** Add this exported definition to the specified MIL environment. */
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

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    Type t = declared.isMonomorphic();
    if (t == null) {
      debug.Internal.error("external " + id + " has polymorphic type " + declared);
    } else if (t.nonUnit()) {
      prog.add(new llvm.GlobalVarDecl(id, lm.toLLVM(t)));
    }
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* Nothing to do here */
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    /* Nothing to do here */
  }

  /**
   * Generate code (in reverse) to initialize each TopLevel (unless all of the components are
   * statically known).
   */
  llvm.Code addRevInitCode(LLVMMap lm, InitVarMap ivm, llvm.Code code) {
    // Generate code to load values of externals in case they are needed later in initialization
    // TODO: Will LLVM optimize away these loads if they are not actually needed?
    // Can we avoid generating them in the first place?
    Type t = declared.isMonomorphic(); // - find the MIL type of this external
    llvm.Type gt = lm.toLLVM(t); // - find the corresponding LLVM type
    llvm.Global g = new llvm.Global(gt.ptr(), id); // - find the global for this external
    llvm.Local l = ivm.reg(gt); // - find a local to hold its value
    ivm.mapGlobal(new TopExt(this), l); // - record the load in the var map
    return new llvm.Op(l, new llvm.Load(g), code); // - emit code to load the value
  }
}
