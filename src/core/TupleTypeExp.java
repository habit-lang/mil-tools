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

public class TupleTypeExp extends PosTypeExp {

  private Kind tupleKind;

  private TypeExp[] texps;

  /** Default constructor. */
  public TupleTypeExp(Position pos, Kind tupleKind, TypeExp[] texps) {
    super(pos);
    this.tupleKind = tupleKind;
    this.texps = texps;
  }

  /** Return the width (i.e., number of components) in this tuple type expression. */
  public int width() {
    return texps.length;
  }

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    for (int i = 0; i < texps.length; i++) {
      texps[i] = texps[i].tidyInfix(env);
    }
    return this;
  }

  /** The Tycon that will be used to build a tuple type. */
  private Tycon tycon;

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    if (tupleKind == KAtom.STAR) {
      String id = "Tuple" + texps.length;
      if ((tycon = TyconEnv.findTycon(id, env)) == null) {
        throw new NotInScopeTyconFailure(pos, id);
      }
    } else /* tupleKind==KAtom.TUPLE */ {
      tycon = TupleCon.tuple(texps.length);
    }
    for (int i = 0; i < texps.length; i++) {
      texps[i] = texps[i].scopeType(params, tdeps, env, 0);
    }
    return this;
  }

  public Kind inferKind() throws Failure {
    for (int i = 0; i < texps.length; i++) {
      texps[i].checkKind(KAtom.STAR);
    }
    return tupleKind;
  }

  public Type toType(Prefix prefix) throws Failure {
    Type t = tycon.asType();
    for (int i = 0; i < texps.length; i++) {
      t = new TAp(t, texps[i].toType(prefix));
    }
    return t;
  }
}
