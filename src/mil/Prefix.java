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

public class Prefix {

  private Tyvar[] vars;

  /** Default constructor. */
  public Prefix(Tyvar[] vars) {
    this.vars = vars;
    numGenerics = vars.length;
  }

  private int numGenerics;

  public Prefix() {
    this(Tyvar.noTyvars);
  }

  public Tyvar getGen(int i) {
    return vars[i];
  }

  public int numGenerics() {
    return numGenerics;
  }

  public boolean isEmpty() {
    return numGenerics == 0;
  }

  /** Add a variable to the end of this prefix, returning the offset at which it is added. */
  public int add(Tyvar var) {
    if (numGenerics >= vars.length) {
      Tyvar[] newvars = new Tyvar[Math.max(1, 2 * numGenerics)];
      for (int offset = 0; offset < numGenerics; offset++) {
        newvars[offset] = vars[offset];
      }
      vars = newvars;
    }
    vars[numGenerics] = var;
    return numGenerics++;
  }

  /**
   * Find a variable in this prefix, returning the offset at which it appears, or else a negative
   * result.
   */
  public int find(Tyvar var) {
    for (int offset = 0; offset < numGenerics; offset++) {
      if (vars[offset] == var) {
        return offset;
      }
    }
    return (-1);
  }

  /**
   * Generate a type scheme that uses this prefix to specify the quantified variables in the given
   * type. If the prefix is empty, then we can return the type directly, without constructing a
   * Forall object.
   */
  public Scheme forall(Type type) {
    return (numGenerics > 0) ? new Forall(this, type) : type;
  }

  public static final Prefix star = new Prefix(new Tyvar[] {Tyvar.star});

  public static final Prefix nat = new Prefix(new Tyvar[] {Tyvar.nat});

  public static final Prefix area = new Prefix(new Tyvar[] {Tyvar.area});

  public static final Prefix tuple = new Prefix(new Tyvar[] {Tyvar.tuple});

  public static final Prefix nat_nat = new Prefix(new Tyvar[] {Tyvar.nat, Tyvar.nat});

  public static final Prefix star_star = new Prefix(new Tyvar[] {Tyvar.star, Tyvar.star});

  public static final Prefix star_nat = new Prefix(new Tyvar[] {Tyvar.star, Tyvar.nat});

  public static final Prefix nat_area = new Prefix(new Tyvar[] {Tyvar.nat, Tyvar.area});

  public static final Prefix nat_nat_nat =
      new Prefix(new Tyvar[] {Tyvar.nat, Tyvar.nat, Tyvar.nat});

  public static final Prefix area_lab_area =
      new Prefix(new Tyvar[] {Tyvar.area, Tyvar.lab, Tyvar.area});

  public Type[] instantiate() {
    Type[] tenv = new Type[numGenerics];
    for (int i = 0; i < numGenerics; i++) {
      tenv[i] = new TVar(vars[i]);
    }
    return tenv;
  }

  /**
   * Construct a prefix from a list of type variables. The resulting prefix will contain a total of
   * n generic variables taken, in reverse order, from the start of the list gens (which should have
   * >= n entries).
   */
  public Prefix(TVar[] generics) {
    numGenerics = generics.length;
    vars = new Tyvar[numGenerics];
    for (int i = 0; i < numGenerics; i++) {
      vars[i] = generics[i].getTyvar();
    }
    // TODO: maybe we don't need both Tyvar[] and TVar[]?
  }

  /**
   * Create a polymorphic BlockType that uses this Prefix to document the generic variables in the
   * type. (If the Prefix is empty, then the result can be described by a simple BlockType.)
   */
  public BlockType forall(Type dom, Type rng) {
    return (numGenerics > 0) ? new PolyBlockType(dom, rng, this) : new BlockType(dom, rng);
  }

  /**
   * Create a fresh instance of a BlockType using this prefix to determine whether we need to
   * allocate any fresh type variables.
   */
  public BlockType instantiateBlockType(Type dom, Type rng) {
    return (numGenerics > 0) ? new TIndBlockType(dom, rng, instantiate()) : new BlockType(dom, rng);
  }

  /**
   * Create a polymorphic AllocType that uses this Prefix to document the generic variables in the
   * type. (If the Prefix is empty, then the result can be described by a simple AllocType.)
   */
  public AllocType forall(Type[] stored, Type result) {
    return (numGenerics > 0)
        ? new PolyAllocType(stored, result, this)
        : new AllocType(stored, result);
  }

  /**
   * Create a fresh instance of an AllocType using this prefix to determine whether we need to
   * allocate any fresh type variables.
   */
  public AllocType instantiateAllocType(Type[] stored, Type result) {
    return (numGenerics > 0)
        ? new TIndAllocType(stored, result, instantiate())
        : new AllocType(stored, result);
  }
}
