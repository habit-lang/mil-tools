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

/** Represents definitions of one or more top-level variables. */
public abstract class Top extends Atom {

  protected Type type;

  /** Default constructor. */
  public Top(Type type) {
    this.type = type;
  }

  /** Return the definition associated with this Top object. */
  public abstract Defn getDefn();

  /** Return the identifer referenced by this Top. */
  public abstract String getId();

  /** Create a clone of this Top object. */
  public abstract Top clone();

  /** Find the dependencies of this AST fragment. */
  Defns dependencies(Defns ds) {
    return getDefn().dependencies(ds);
  }

  /** Generate a printable description of this atom. */
  public String toString() {
    return getId();
  }

  /** Apply a TempSubst to this Atom. */
  Atom apply(TempSubst s) {
    return this.clone();
  }

  /**
   * Return a type for an instantiated version of this item when used as an Atom (input operand).
   */
  public Type instantiate() {
    return type = getDeclared().instantiate();
  }

  /** Type check the implementation for an external. */
  void checkImp(Handler handler, Position pos) {
    Scheme declared = getDeclared();
    if (declared == null) {
      handler.report(new Failure(pos, "Implementation for external does not have a declared type"));
    } else if (declared.isMonomorphic() == null) {
      handler.report(
          new Failure(pos, "Implementation for external does not have a monomorphic type"));
    }
    instantiate();
  }

  /** Return the declared type for this Top. */
  public abstract Scheme getDeclared();

  /** Determine whether this Atom has a known (static) value. */
  boolean isStatic() {
    return true;
  }

  Atom isKnown(boolean allowWord) {
    return this;
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonType(set);
    }
  }

  /** Return the representation vector for this Atom. */
  Type[] repCalc() {
    return type.repCalc(null);
  }

  public static final Top Unit =
      new TopDef(
          new TopLevel(BuiltinPosition.pos, new TopLhs(), new DataAlloc(Cfun.Unit).withArgs()), 0);

  public void setDeclared(Position pos, Scheme scheme) throws Failure {
    throw new Failure(
        pos, "Cannot use type signature; \"" + getId() + "\" is not a top level variable");
  }

  /**
   * Determine whether this item is for a non-Unit, corresponding to a value that requires a
   * run-time representation in the generated LLVM.
   */
  boolean nonUnit() {
    return type.nonUnit();
  }

  public llvm.Value staticValue() {
    return null;
  }

  Type getType() {
    return type;
  }

  /** Calculate an LLVM Value corresponding to a given MIL argument. */
  llvm.Value toLLVMAtom(LLVMMap lm, VarMap vm) {
    return vm.lookupGlobal(lm, this);
  }
}
