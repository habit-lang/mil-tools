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

  public Top clone() {
    return new TopArea(type, area);
  }

  public String getId() {
    return area.getId();
  }

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameTopArea(area);
  }

  /** Test to determine whether this Atom refers to the specified Area. */
  boolean sameTopArea(Area area) {
    return this.area == area;
  }

  /** Return the definition associated with this Top object. */
  public Defn getDefn() {
    return area;
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return area.dependencies(ds);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return type = area.instantiate();
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  void copyTo(int dst, MachineBuilder builder) {
    builder.gcopy(area, dst);
  }

  /** Generate code to load the data for this atom into the value register. */
  void load(MachineBuilder builder) {
    builder.gload(area);
  }

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    // (Substitution is not expected to have an effect on monomorphic area type)
    return area.specializeArea(spec, type.apply(s));
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return new Atom[] {new TopArea(null, area)};
  }

  public void setDeclared(Handler handler, Position pos, Scheme scheme) {
    area.setDeclared(handler, pos, scheme);
  }

  public llvm.Value staticValue() {
    return area.staticValue();
  }
}
