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

/** Represents a type constructor that has an associated list of constructor functions. */
public abstract class DataName extends Tycon {

  /** Default constructor. */
  public DataName(Position pos, String id) {
    super(pos, id);
  }

  /** Holds the list of constructor functions for this DataName. */
  protected Cfun[] cfuns;

  public void setCfuns(Cfun[] cfuns) {
    this.cfuns = cfuns;
  }

  /** Flag to indicate if this is a recursive type. */
  protected boolean isRecursive = false;

  /** Set the flag to indicate that this datatype is recursive. */
  public void setRecursive() {
    isRecursive = true;
  }

  /** Determine whether this has been marked as a recursive type. */
  public boolean isRecursive() {
    return isRecursive;
  }

  /**
   * Get the array of constructor functions associated with this object, or return null if this is
   * not a DataName.
   */
  public Cfun[] getCfuns() {
    return cfuns;
  }

  public obdd.Pat getPat(int num) {
    debug.Internal.error("DataName does not have a bit pattern");
    return null;
  }

  /** Return the canonical version of a Tycon wrt to the given set. */
  Tycon canonTycon(TypeSet set) {
    Tycon ntycon = set.mapsTyconTo(this);
    if (ntycon != null) { // Use previously computed canonical version if available
      return ntycon;
    } else if (set.containsTycon(
        this)) { // Tycon is already in the target?  (TODO: is this still necessary?)
      return this;
    }
    return makeCanonTycon(set); // But otherwise, make a new canonical version
  }

  /**
   * Make a canonical version of a type definition wrt the given set, replacing component types with
   * canonical versions as necessary. We only need implementations of this method for StructType and
   * (subclasses of) DataName.
   */
  abstract Tycon makeCanonTycon(TypeSet set);

  /**
   * Return true if this is a newtype constructor (i.e., a single argument constructor function for
   * a nonrecursive type that only has one constructor).
   */
  public boolean isNewtype() { // Don't treat bitdata types as newtypes
    return false;
  }

  /** Return true if this is a single constructor type. */
  public boolean isSingleConstructor() {
    return false;
  }

  Tycon specializeDataName(MILSpec spec, Type inst) {
    return canonTycon(spec);
  }

  DataType dataType() {
    return null;
  }

  abstract Code repTransformAssert(RepTypeSet set, Cfun cf, Atom a, Code c);

  Block maskTestBlock(int num) {
    debug.Internal.error("DataName does not have a mask test block");
    return null;
  }

  abstract Tail repTransformDataAlloc(RepTypeSet set, Cfun cf, Atom[] args);

  abstract Tail repTransformSel(RepTypeSet set, RepEnv env, Cfun cf, int n, Atom a);

  abstract Code repTransformSel(
      RepTypeSet set, RepEnv env, Temp[] vs, Cfun cf, int n, Atom a, Code c);

  public void addCfunsTo(Handler handler, MILEnv milenv) {
    for (int i = 0; i < cfuns.length; i++) {
      try {
        milenv.addCfunAndTop(cfuns[i]);
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }
}
