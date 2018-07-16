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
import compiler.Handler;
import compiler.Position;
import core.*;

public class TopDef extends Top {

  private TopLevel topLevel;

  private int i;

  /** Default constructor. */
  public TopDef(Type type, TopLevel topLevel, int i) {
    super(type);
    this.topLevel = topLevel;
    this.i = i;
  }

  public TopLevel getTopLevel() {
    return topLevel;
  }

  public TopDef(TopLevel topLevel, int i) {
    this(null, topLevel, i);
  }

  public Top clone() {
    return new TopDef(type, topLevel, i);
  }

  public String getId() {
    return topLevel.getId(i);
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return topLevel.dependencies(ds);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return type = topLevel.instantiate(i);
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  void copyTo(int dst, MachineBuilder builder) {
    builder.gcopy(topLevel, i, dst);
  }

  /** Generate code to load the data for this atom into the value register. */
  void load(MachineBuilder builder) {
    builder.gload(topLevel, i);
  }

  public Tail lookupFact(Facts facts) {
    return topLevel.lookupFact(this.getTopLevel()); /* top level can't use local facts */
  }

  /**
   * Special case treatment for top-level bindings of the form [...,x,...] <- return [...,y,...]; we
   * want to short out such bindings whenever possible by replacing all occurrences of x with y.
   */
  Atom shortTopLevel() {
    return topLevel.shortTopLevel(this, i);
  }

  /**
   * Test to determine if this Atom is known to hold a specific function value, represented by a
   * ClosAlloc/closure allocator, according to the given set of facts.
   */
  ClosAlloc lookForClosAlloc(Facts facts) {
    return topLevel.lookForClosAlloc();
  }

  public Tail entersTopLevel(Atom[] iargs) {
    return topLevel.entersTopLevel(iargs);
  }

  /**
   * Test to determine if this Atom is known to hold a specific data value represented by a
   * DataAlloc allocator, according to the given set of facts.
   */
  DataAlloc lookForDataAlloc(Facts facts) {
    return topLevel.lookForDataAlloc();
  }

  /**
   * Determine whether this src argument is a value base (i.e., a numeric or global/primitive
   * constant) that is suitable for use in complex addressing modes.
   */
  boolean isBase() {
    return topLevel.isBase();
  }

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    // Find the type for this specific instance:
    Type inst = type.apply(s);
    // Return the ith component in a specialized version of the associated topLevel:
    return new TopDef(inst, topLevel.specializedTopLevel(spec, inst, i), i);
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    Atom[] as = set.topDef(topLevel, i);
    return (as != null) ? as : null;
  }

  Defn getDefn() {
    return topLevel;
  }

  void setDeclared(Handler handler, Position pos, Scheme scheme) {
    topLevel.setDeclared(handler, pos, i, scheme);
  }

  public llvm.Value staticValue() {
    return topLevel.staticValue(i);
  }

  /**
   * Calculate a static value for this atom, or else return null if the result must be calculated at
   * runtime.
   */
  llvm.Value staticValueCalc() {
    return topLevel.staticValue(i);
  }

  /**
   * Test to determine whether two Top values refer to the same item. Implemented using a standard
   * double dispatch strategy.
   */
  boolean sameTop(Top that) {
    return that.sameTopDef(topLevel, i);
  }

  boolean sameTopDef(TopLevel topLevel, int i) {
    return this.topLevel == topLevel && this.i == i;
  }
}
