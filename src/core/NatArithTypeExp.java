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
package core;

import compiler.*;
import mil.*;

public class NatArithTypeExp extends PosTypeExp {

  private NatArithOp op;

  private TypeExp l;

  private TypeExp r;

  /** Default constructor. */
  public NatArithTypeExp(Position pos, NatArithOp op, TypeExp l, TypeExp r) {
    super(pos);
    this.op = op;
    this.l = l;
    this.r = r;
  }

  /**
   * Scope analysis on type expressions in a context where we expect all of the type constructors to
   * be defined, but (if canAdd is true) we will treat undefined type variables as implicitly bound,
   * universally quantified type variables.
   */
  public void scopeType(boolean canAdd, TyvarEnv params, TyconEnv env, int arity) throws Failure {
    l.scopeType(canAdd, params, env, 0);
    r.scopeType(canAdd, params, env, 0);
  }

  public Kind inferKind() throws KindMismatchFailure {
    l.checkKind(KAtom.NAT);
    r.checkKind(KAtom.NAT);
    return KAtom.NAT;
  }

  public Type toType(Prefix prefix) throws Failure {
    Type tl = l.toType(prefix).simplifyNatType(null);
    Type tr = r.toType(prefix).simplifyNatType(null);
    if (tl == null) {
      throw new Failure(pos, "Could not calculate natural number for left argument of " + op);
    } else if (tr == null) {
      throw new Failure(pos, "Could not calculate natural number for right argument of " + op);
    }
    try {
      return new TNat(op.op(tl.getNat(), tr.getNat()));
    } catch (Exception e) {
      throw new Failure(pos, "Failed to calculate natural number for " + op);
    }
  }

  /**
   * Scope analysis on type expressions in a context where we want to determine which (if any)
   * CoreDefn values a particular type expression depends on.
   */
  public CoreDefns scopeTycons(TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends)
      throws Failure {
    return r.scopeTycons(params, env, defns, l.scopeTycons(params, env, defns, depends));
  }
}
