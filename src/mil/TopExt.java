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

  /**
   * Test to see if two atoms are the same. For Temp values, we use pointer equality to determine
   * object equality. For all other types of Atom, we use double dispatch to compare component
   * values.
   */
  public boolean sameAtom(Atom that) {
    return that.sameTopExt(external);
  }

  /** Test to determine whether this Atom refers to the specified External. */
  boolean sameTopExt(External external) {
    return this.external == external;
  }

  /** Return the definition associated with this Top object. */
  public Defn getDefn() {
    return external;
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

  Atom specializeAtom(MILSpec spec, TVarSubst s, SpecEnv env) {
    Type inst = type.apply(s);
    return new TopExt(inst, spec.specializedExternal(external, inst));
  }

  Atom[] repArg(RepTypeSet set, RepEnv env) {
    return external.repExt();
  }
}
