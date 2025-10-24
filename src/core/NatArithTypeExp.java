/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    l = l.tidyInfix(env);
    r = r.tidyInfix(env);
    return this;
  }

  /**
   * Worker function for scopeType that is intended to be called after order of any infix operators
   * have been determined and has the option to rewrite the type expression if needed.
   */
  public TypeExp scopeTypeRewrite(boolean canAdd, TyvarEnv params, TyconEnv env, int arity)
      throws Failure {
    l = l.scopeTypeRewrite(canAdd, params, env, 0);
    r = r.scopeTypeRewrite(canAdd, params, env, 0);
    return this;
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
  public CoreDefns scopeTyconsType(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    return r.scopeTyconsType(
        handler, params, env, defns, l.scopeTyconsType(handler, params, env, defns, depends));
  }
}
