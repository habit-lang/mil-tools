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

public class SelTypeExp extends PosTypeExp {

  private TypeExp t;

  private String lab;

  /** Default constructor. */
  public SelTypeExp(Position pos, TypeExp t, String lab) {
    super(pos);
    this.t = t;
    this.lab = lab;
  }

  /**
   * Remove occurrences of InfixTypeExp nodes from this type expression using fixity information.
   * For all other node types, we update individual fields in place (in case they included
   * InfixTypeExp nodes) and return the original node (this).
   */
  public TypeExp tidyInfix(TyconEnv env) throws Failure {
    t = t.tidyInfix(env);
    return this;
  }

  /**
   * Worker function that performs the main scope analysis on type expressions. An additional arity
   * parameter is used here so that we can track and validate the number of arguments that are
   * passed to type constructors.
   */
  public TypeExp scopeType(TyvarEnv params, TyconDeps tdeps, TyconEnv env, int arity)
      throws Failure {
    t = t.scopeType(params, tdeps, env, 0);
    return this;
  }

  public Kind inferKind() throws Failure {
    t.checkKind(KAtom.STAR);
    return KAtom.STAR;
  }

  public Type toType(Prefix prefix) throws Failure {
    BitdataType bt = t.toType(prefix).bitdataType();
    if (bt == null) {
      throw new Failure(pos, "Expected bitdata type");
    } else {
      BitdataLayout[] layouts = bt.getLayouts();
      int i = Name.index(lab, layouts);
      if (i >= 0) {
        return layouts[i].asType();
      }
      throw new Failure(pos, "Label " + lab + " is not used in " + bt);
    }
  }
}
