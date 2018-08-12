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

public class TopExt extends Top {

  private External external;

  /** Default constructor. */
  public TopExt(Type type, External external) {
    super(type);
    this.external = external;
  }

  public TopExt(External external) {
    this(null, external);
  }

  public Top clone() {
    return new TopExt(type, external);
  }

  public String getId() {
    return external.getId();
  }

  /** Find the dependencies of this AST fragment. */
  public Defns dependencies(Defns ds) {
    return external.dependencies(ds);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return type = external.instantiate();
  }

  /** Generate code to copy the data for this atom into the specified frame slot. */
  void copyTo(int dst, MachineBuilder builder) {
    builder.gcopy(external, dst);
  }

  /** Generate code to load the data for this atom into the value register. */
  void load(MachineBuilder builder) {
    builder.gload(external);
  }

  /**
   * Determine whether this src argument is a value base (i.e., a numeric or global/primitive
   * constant) that is suitable for use in complex addressing modes.
   */
  boolean isBase() {
    return true;
  }

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    Type inst = type.apply(s);
    return new TopExt(inst, spec.specializedExternal(external, inst));
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return external.repExt();
  }

  Defn getDefn() {
    return external;
  }

  /**
   * Test to determine whether two Top values refer to the same item. Implemented using double
   * dispatch.
   */
  boolean sameTop(Top that) {
    return that.sameTopExt(external);
  }

  /** Test to determine whether this Top refers to the specified External. */
  boolean sameTopExt(External external) {
    return this.external == external;
  }
}
