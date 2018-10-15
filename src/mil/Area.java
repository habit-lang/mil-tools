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

public abstract class Area extends TopDefn {

  protected String id;

  /** Default constructor. */
  public Area(Position pos, String id) {
    super(pos);
    this.id = id;
  }

  protected Scheme declared;

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

  String dotAttrs() {
    return "style=filled, fillcolor=darkolivegreen1";
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

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    /* Nothing to do here */
  }

  void printlnSig(PrintWriter out) {
    out.println(id + " :: " + declared);
  }

  abstract Atom specializeArea(MILSpec spec, Type inst);

  void bitdataRewrite(BitdataMap m) {
    /* Nothing to do here */
  }

  public void setDeclared(Handler handler, Position pos, Scheme scheme) {
    if (declared != null) {
      handler.report(new Failure(pos, "Multiple type annotations for \"" + id + "\""));
    }
    declared = scheme;
  }

  public abstract void inScopeOf(Handler handler, MILEnv milenv, AtomExp init) throws Failure;

  /** Add this exported definition to the specified MIL environment. */
  void addExport(MILEnv exports) {
    exports.addTop(id, new TopArea(this));
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  Temp[] addArgs() throws Failure {
    return null;
  }

  protected llvm.Value staticValue;

  public llvm.Value staticValue() {
    return staticValue;
  }

  protected llvm.Value calcStaticValue(String id) {
    return new llvm.PtrToInt(new llvm.Global(llvm.Type.i8.ptr(), id), llvm.Type.word());
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
