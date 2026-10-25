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

/** Represents an area declaration: id <- area l a [init]. */
public abstract class Area extends TopDefn {

  /** The name of the area being defined. */
  protected String id;

  /** Default constructor. */
  public Area(Position pos, String id) {
    super(pos);
    this.id = id;
  }

  /** The declared or inferred type for this area. */
  protected Scheme declared;

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

  /** Specifies the dot attributes for a node representing a particular type of definition. */
  String dotAttrs() {
    return "style=filled fillcolor=darkolivegreen1";
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
    return declared.instantiate();
  }

  /**
   * Set the initial type for this definition by instantiating the declared type, if present, or
   * using type variables to create a suitable skeleton. Also sets the types of bound variables.
   */
  void setInitialType() throws Failure {
    /* Nothing to do here */
  }

  /** Type check the body of this definition, throwing an exception if there is an error. */
  void checkBody(Position pos) throws Failure {
    /* Nothing to do here */
  }

  void findAmbigTVars(Handler handler, TVars gens) {
    /* Nothing to do here */
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    handler.report(new Failure(pos, "Cannot use area \"" + id + "\" in bytecode"));
  }

  /** Apply inlining. */
  void inlining() {
    /* Nothing to do here */
  }

  /**
   * Count the number of unused arguments for this definition using the current unusedArgs
   * information for any other items that it references.
   */
  int countUnusedArgs() {
    return 0;
  }

  /** Perform flow analysis on this definition. */
  void flow() {
    /* Nothing to do here */
  }

  /**
   * Return references to all components of this top level definition in an array of
   * atoms/arguments.
   */
  Atom[] tops() {
    return new TopArea[] {new TopArea(this)};
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

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    /* Nothing to do here */
  }

  /** Worker function for MILSpec.dump(): Print a signature line for this definition. */
  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

  /**
   * Return an Atom corresponding to a specialized version of this memory area under the given
   * substitution. (We do not currently allow memory areas with polymorphic types, so the
   * substitution will have no effect here. But we keep the plumbing, just in case it might be
   * needed in future, for example, in connection with type expressions that are used to specify
   * size and alignment.
   */
  abstract Atom specializeArea(MILSpec spec, Type inst);

  /** Rewrite a program to use the bitdata representations provided by BitdataMap. */
  void bitdataRewrite(BitdataMap m) {
    /* Nothing to do here */
  }

  /**
   * Rewrite this definition to update references to constructor functions consistent with the
   * datatype equalities in the given MergeMap.
   */
  void mergeRewrite(MergeMap mmap) {
    /* Nothing to do here */
  }

  public void setDeclared(Position pos, Scheme scheme) throws Failure {
    if (declared != null) {
      throw new Failure(pos, "Multiple type annotations for \"" + id + "\"");
    }
    declared = scheme;
  }

  abstract void inScopeOf(Handler handler, MILEnv milenv, AtomExp init) throws Failure;

  /** Add this exported definition to the specified MIL environment. */
  void addExport(MILEnv exports) {
    exports.addTop(id, new TopArea(this));
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  public Temp[] addArgs() throws Failure {
    return null;
  }

  protected llvm.Value staticValue;

  public llvm.Value staticValue() {
    return staticValue;
  }

  protected llvm.Value calcStaticValue(String id) {
    return new llvm.PtrToInt(new llvm.Global(llvm.Type.ptr, id), llvm.Type.word());
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* Nothing to do here */
  }
}
