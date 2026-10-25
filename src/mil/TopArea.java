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

/** Represents the area address introduced by an area definition. */
public class TopArea extends Top {

  private Area area;

  /** Default constructor. */
  public TopArea(Type type, Area area) {
    super(type);
    this.area = area;
  }

  public TopArea(Area area) {
    this(null, area);
  }

  /** Return the definition associated with this Top object. */
  public Defn getDefn() {
    return area;
  }

  /** Return the identifer referenced by this Top. */
  public String getId() {
    return area.getId();
  }

  /** Create a clone of this Top object. */
  public Top clone() {
    return new TopArea(type, area);
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  boolean sameAtom(Atom that) {
    return that.sameTopArea(this);
  }

  /** Test whether this Atom is the same as the given TopArea. */
  boolean sameTopArea(TopArea that) {
    return this.area == that.area;
  }

  /** Return the declared type for this Top. */
  public Scheme getDeclared() {
    return area.getDeclared();
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  void copyTo(int dst, MachineBuilder builder) {
    builder.gcopy(area, dst);
  }

  /** Generate code to load the data for this atom into the value register. */
  void load(MachineBuilder builder) {
    builder.gload(area);
  }

  Atom isKnown(boolean allowWord) {
    return null;
  }

  /**
   * Determine the specialized version of an Atom in a context described by a given @TVarSubst@
   * and @SpecEnv@.
   */
  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    // (Substitution s is not expected to have an effect on a monomorphic
    // area type, but we apply it anyway, just to be sure.)
    return area.specializeArea(spec, type.apply(s));
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return new Atom[] {new TopArea(null, area)};
  }

  public void setDeclared(Position pos, Scheme scheme) throws Failure {
    area.setDeclared(pos, scheme);
  }

  public llvm.Value staticValue() {
    return area.staticValue();
  }
}
