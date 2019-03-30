/*
    Copyright 2018-19 Mark P Jones, Portland State University

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

public class Ptr extends DataType {

  /** Default constructor. */
  public Ptr(Position pos, String id, Kind kind, int arity) {
    super(pos, id, kind, arity);
  }

  /** A constructor for defining types that have a BuiltinPosition. */
  public Ptr(String id, Kind kind, int arity) {
    this(BuiltinPosition.pos, id, kind, arity);
    TyconEnv.builtin.add(this);
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    return this;
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  Tycon makeCanonTycon(TypeSet set) { // Capture uses of pointer types
    set.addTycon(this);
    return this;
  }

  /**
   * Find out if there is a specialized version of the type with this Tycon as its head and the set
   * of args (canonical) arguments on the stack of this MILSpec object.
   */
  Tycon specInst(MILSpec spec, int args) {
    return null;
  }

  Tycon specializeDataName(MILSpec spec, Type inst) {
    return this;
  }

  /** Determine whether this Tycon is a DataType that is a candidate for bitdata representation. */
  DataType bitdataCandidate() {
    return null;
  }

  /** Determine whether this Tycon is a DataType that is a candidate for merging. */
  DataType mergeCandidate() {
    return null;
  }

  /**
   * Return the representation vector for types formed by applying this type constructor to the
   * argument a. This allows us to provide special representations for types of the form Bit a, Ix
   * a, Ref a, etc. If none of these apply, we just return null. TODO: are there other types we
   * should be including here?
   */
  Type[] repCalc(Type a) {
    return Tycon.wordRep;
  }

  Code repTransformSel(RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c) {
    return new Bind(vs, new Return(a.repAtom(set, env)), c);
  }

  /**
   * Determine if this type, applied to the given a, is a reference type of the form (Ref a) or (Ptr
   * a). TODO: The a parameter is not currently inspected; we could attempt to check that it is a
   * valid area type (but kind checking should have done that already) or else look to eliminate it.
   */
  boolean referenceType(Type[] tenv, Type a) {
    return true;
  }
}
