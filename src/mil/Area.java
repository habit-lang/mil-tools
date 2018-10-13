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
import java.math.BigInteger;

public class Area extends TopDefn {

  private String id;

  private long alignment;

  private Type areaType;

  /** Default constructor. */
  public Area(Position pos, String id, long alignment, Type areaType) {
    super(pos);
    this.id = id;
    this.alignment = alignment;
    this.areaType = areaType;
  }

  private Scheme declared;

  private Type size;

  private Atom init;

  /**
   * Return references to all components of this top level definition in an array of
   * atoms/arguments.
   */
  Atom[] tops() {
    return new TopArea[] {new TopArea(this)};
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
    return (init == null) ? null : init.dependencies(null);
  }

  String dotAttrs() {
    return "style=filled, fillcolor=darkolivegreen1";
  }

  /** Display a printable representation of this definition on the specified PrintWriter. */
  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    if (declared != null) {
      if (isEntrypoint) {
        out.print("entrypoint ");
      }
      out.println(id + " :: " + declared);
    }

    out.print(id + " <- area " + areaType.toString(TypeWriter.ALWAYS));
    if (init != null) {
      out.print(" " + init);
    }
    out.println(" aligned " + alignment);
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
    if (init != null) {
      init.instantiate().unify(pos, Type.init(areaType));
    }
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
    // Check that area has a known ByteSize
    // TODO: shouldn't this calculation be done just once when this Area is constructed?
    size = areaType.byteSize(null);
    if (size == null || size.getNat() == null) {
      throw new Failure(
          pos, "Cannot determine size in bytes for values of type \"" + areaType + "\"");
    }

    // Validate declared type:
    Type inferred = (init == null) ? Tycon.word.asType() : Type.ref(areaType);
    if (declared != null && !declared.alphaEquiv(inferred)) {
      throw new Failure(
          pos,
          "Declared type \""
              + declared
              + "\" for \""
              + id
              + "\" does not match inferred type \""
              + inferred
              + "\"");
    }
    declared = inferred;
  }

  void findAmbigTVars(Handler handler, TVars gens) {
    /* Nothing to do here */
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    handler.report(new Failure(pos, "Cannot use area \"" + id + "\" in bytecode"));
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

  void collect(TypeSet set) {
    areaType = areaType.canonType(set);
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
    if (init != null) {
      init.collect(set);
    }
  }

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    /* Nothing to do here */
  }

  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  Area isAreaOfType(Scheme inst) {
    return declared.alphaEquiv(inst) ? this : null;
  }

  Area(Area a, int num) {
    this(a.pos, mkid(a.id, num), a.alignment, a.areaType);
  }

  /**
   * Fill in the initializer for this area with a specialized version of the original's initializer.
   */
  void specialize(MILSpec spec, Area aorig) {
    // Although the area itself will have a monomorphic type, we still need to ensure that
    // specialization is applied to the initializer.
    debug.Log.println(
        "Area specialize: "
            + aorig
            + " :: "
            + aorig.declared
            + "  ~~>  "
            + this
            + " :: "
            + this.declared);
    this.init = aorig.init.specializeAtom(spec, null, null);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    Type t = declared.isMonomorphic();
    if (t != null) {
      Area a = spec.specializedArea(this, t);
      a.id = this.id;
      return a;
    }
    throw new PolymorphicEntrypointFailure("area", this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
    areaType = areaType.canonType(spec);
  }

  void bitdataRewrite(BitdataMap m) {
    /* Nothing to do here */
  }

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    declared = Tycon.word.asType();
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    areaType = areaType.canonType(set);
    declared = declared.canonScheme(set);
    if (init != null) {
      set.addInitializer(new Enter(init.repArg(set, null)[0], new TopArea(this)));
      init = null; // Clear away initializer
      declared = null; // Reset "declared" type due to change in representation
    }
  }

  public void setDeclared(Handler handler, Position pos, Scheme scheme) {
    if (declared != null) {
      handler.report(new Failure(pos, "Multiple type annotations for \"" + id + "\""));
    }
    declared = scheme;
  }

  public void inScopeOf(Handler handler, MILEnv milenv, AtomExp init) throws Failure {
    this.init = init.inScopeOf(handler, milenv, null);
  }

  /** Add this exported definition to the specified MIL environment. */
  void addExport(MILEnv exports) {
    exports.addTop(id, new TopArea(this));
  }

  public void setInit(Atom init) {
    this.init = init;
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  Temp[] addArgs() throws Failure {
    return null;
  }

  private llvm.Value staticValue;

  public llvm.Value staticValue() {
    return staticValue;
  }

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    BigInteger bigsize = size.getNat();
    if (bigsize == null || bigsize.signum() < 0) { // TODO: add upper bound test
      debug.Internal.error("Unable to determine size of area " + id);
    }
    llvm.ArrayType at = new llvm.ArrayType(bigsize.longValue(), llvm.Type.i8);
    String rawName = prog.freshName("raw");
    llvm.Global rawGlobal = new llvm.Global(at.ptr(), rawName);
    prog.add(new llvm.GlobalVarDefn(true, rawName, at.defaultValue(), alignment));
    prog.add(new llvm.Alias(!isEntrypoint, id, new llvm.BitcastVal(rawGlobal, llvm.Type.i8.ptr())));
    staticValue = new llvm.PtrToIntVal(new llvm.Global(llvm.Type.i8.ptr(), id), llvm.Type.word());
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
}
