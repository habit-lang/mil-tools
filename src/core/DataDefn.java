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

public class DataDefn extends TyconDefn {

  private TypeExp lhs;

  private DataConDefn[] constrs;

  /** Default constructor. */
  public DataDefn(Position pos, String id, TypeExp lhs, DataConDefn[] constrs) {
    super(pos, id);
    this.lhs = lhs;
    this.constrs = constrs;
  }

  private DataType dt;

  /** An environment to capture the parameters. */
  private TyvarEnv params;

  public void setRecursive() {
    dt.setRecursive();
  }

  /** Add environment entries for the type constructors introduced in this definition. */
  public void introduceTycons(Handler handler, TyconEnv env) {
    params = new TyvarEnv();
    try {
      id = lhs.analyzeDataDefnLhs(params);
      params.checkForMultipleTyvars(handler);
      env.add(dt = params.makeDataType(pos, id));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeDefn(Handler handler, TyconDeps tdeps, TyconEnv env) throws Failure {
    lhs = lhs.tidyInfix(env);
    int len = params.getLen();
    for (int i = 0; i < constrs.length; i++) {
      constrs[i].scopeDefn(handler, params, tdeps, env);
    }
    NotInScopeTyvarFailure.beyond(handler, params, len);
  }

  public void kindInfer(Handler handler) {
    // The type expressions for every constructor should be well-kinded.
    for (int i = 0; i < constrs.length; i++) {
      // (guards against continuing after previously detected error)
      if (constrs[i] != null) {
        constrs[i].kindInfer(handler);
      }
    }
  }

  /** Default kind variables to STAR, unless they have already been bound. */
  public void defaultKinds() {
    params.defaultKinds();
    dt.defaultKinds();
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    // Calculate the range type for constructors in this type
    Type rng = dt.asType();
    // TODO: if the context includes extra variables, determined by the head,
    // then len will be greater than arity, and the following code will
    // generate a kind with too many arguments.
    int len = params.getLen();
    for (int i = 0; i < len; i++) {
      rng = new TAp(rng, Type.gen(i));
    }

    // Figure out what the prefix should be for each cfun type:
    Prefix prefix = params.toPrefix();

    // Make an array to hold the constructor functions for this type:
    Cfun[] cfuns = new Cfun[constrs.length];

    // Define a name for each constructor function:
    for (int i = 0; i < constrs.length; i++) {
      try {
        cfuns[i] = constrs[i].calcCfun(prefix, dt, rng, i);
      } catch (Failure f) {
        handler.report(f);
      }
    }

    // Store the resulting list of constructor functions:
    dt.setCfuns(cfuns);
  }

  /**
   * Extend the specified MIL environment with entries for any functions or values introduced in
   * this definition.
   */
  public void addToMILEnv(Handler handler, CoreProgram prog, MILEnv milenv) {
    Cfun[] cfuns = getCfuns();
    for (int i = 0; i < cfuns.length; i++) {
      try {
        milenv.addCfunAndTop(cfuns[i]);
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  /** Return the array of constructor functions for this type. */
  Cfun[] getCfuns() {
    return dt.getCfuns();
  }
}
