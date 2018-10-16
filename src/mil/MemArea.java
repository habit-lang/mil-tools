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

public class MemArea extends Area {

  private long alignment;

  private Type areaType;

  private Type size;

  /** Default constructor. */
  public MemArea(Position pos, String id, long alignment, Type areaType, Type size) {
    super(pos, id);
    this.alignment = alignment;
    this.areaType = areaType;
    this.size = size;
  }

  private Atom init;

  public void setInit(Atom init) {
    this.init = init;
  }

  /** Find the list of Defns that this Defn depends on. */
  public Defns dependencies() {
    return (init == null) ? null : init.dependencies(null);
  }

  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    super.dump(out, isEntrypoint);
    out.print(id + " <- area " + areaType.toString(TypeWriter.ALWAYS));
    if (init != null) {
      out.print(" " + init);
    }
    out.println(" aligned " + alignment);
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

  /**
   * Calculate a generalized type for this binding, adding universal quantifiers for any unbound
   * type variable in the inferred type. (There are no "fixed" type variables here because all mil
   * definitions are at the top level.)
   */
  void generalizeType(Handler handler) throws Failure {
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

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    areaType = areaType.canonType(set);
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
    if (init != null) {
      init.collect(set);
    }
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  MemArea isMemAreaOfType(Scheme inst) {
    return declared.alphaEquiv(inst) ? this : null;
  }

  MemArea(MemArea a, int num) {
    this(a.pos, mkid(a.id, num), a.alignment, a.areaType, a.size);
  }

  /**
   * Fill in the initializer for this area with a specialized version of the original's initializer.
   */
  void specialize(MILSpec spec, MemArea aorig) {
    // Although the area itself will have a monomorphic type, we still need to ensure that
    // specialization is applied to the initializer.
    debug.Log.println(
        "MemArea specialize: "
            + aorig
            + " :: "
            + aorig.declared
            + "  ~~>  "
            + this
            + " :: "
            + this.declared);
    this.init = aorig.init.specializeAtom(spec, null, null);
  }

  Atom specializeArea(MILSpec spec, Type inst) {
    return new TopArea(inst, spec.specializedMemArea(this, inst));
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    Type t = declared.isMonomorphic();
    if (t != null) {
      MemArea a = spec.specializedMemArea(this, t);
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

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    declared = Tycon.word.asType();
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    areaType = areaType.canonType(set);
    declared = declared.canonScheme(set); // TODO: likely unnecessary; always Word
    if (init != null) {
      set.addInitializer(new Enter(init.repArg(set, null)[0], new TopArea(this)));
      init = null; // Clear away initializer
      declared = null; // Reset "declared" type due to change in representation
    }
  }

  public void inScopeOf(Handler handler, MILEnv milenv, AtomExp init) throws Failure {
    this.init = init.inScopeOf(handler, milenv, null);
  }

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    BigInteger bigsize = size.getNat();
    if (bigsize == null || bigsize.signum() < 0) { // TODO: add upper bound test
      debug.Internal.error("Unable to determine size of area " + id);
    }
    llvm.Type at = new llvm.ArrayType(bigsize.longValue(), llvm.Type.i8);
    String rawName = prog.freshName("raw");
    prog.add(new llvm.GlobalVarDefn(llvm.Mods.INTERNAL, rawName, at.defaultValue(), alignment));
    prog.add(
        new llvm.Alias(
            llvm.Mods.entry(isEntrypoint),
            id,
            new llvm.Bitcast(new llvm.Global(at.ptr(), rawName), llvm.Type.i8.ptr())));
    staticValue = calcStaticValue(id);
  }
}
