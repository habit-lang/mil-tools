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

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameTopDef(topLevel, i);
  }

  /** Test to determine whether this Atom refers to the ith TopLhs in the given TopLevel. */
  boolean sameTopDef(TopLevel topLevel, int i) {
    return this.topLevel == topLevel && this.i == i;
  }

  /** Return the definition associated with this Top object. */
  public Defn getDefn() {
    return topLevel;
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

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    // Find the type for this specific instance:
    Type inst = type.apply(s);
    // Return the ith component in a specialized version of the associated topLevel:
    return new TopDef(inst, topLevel.specializedTopLevel(spec, inst, i), i);
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return set.topDef(topLevel, i);
  }

  public void setDeclared(Handler handler, Position pos, Scheme scheme) {
    topLevel.setDeclared(handler, pos, i, scheme);
  }

  public llvm.Value staticValue() {
    return topLevel.staticValue(i);
  }

  /**
   * Calculate a static value for this atom, or return null if the result must be determined at
   * runtime.
   */
  llvm.Value calcStaticValue() {
    return topLevel.staticValue(i);
  }
}
