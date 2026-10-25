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
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    l = l.tidyInfix(env);
    r = r.tidyInfix(env);
    return this;
  }

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    l = l.scopeType(params, tdeps, env, 0);
    r = r.scopeType(params, tdeps, env, 0);
    return this;
  }

  public Kind inferKind() throws Failure {
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
}
