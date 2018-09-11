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
package core;

import compiler.*;
import mil.*;

public class DataDefn extends TyconDefn {

  private TypeExp[] args;

  private DataConDefn[] constrs;

  /** Default constructor. */
  public DataDefn(Position pos, String id, TypeExp[] args, DataConDefn[] constrs) {
    super(pos, id);
    this.args = args;
    this.constrs = constrs;
  }

  private DataType dt;

  private TyvarEnv params;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return dt;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    params = new TyvarEnv();
    for (int i = 0; i < args.length; i++) {
      try {
        params.add(args[i].scopeTypeParam(null));
      } catch (Failure f) {
        handler.report(f);
      }
    }
    params.checkForMultipleTyvars(handler);
    env.add(dt = new DataType(pos, id, params.toKind(), args.length));
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) {
    CoreDefns depends = null;
    for (int i = 0; i < constrs.length; i++) {
      depends = constrs[i].scopeTycons(handler, params, env, defns, depends);
    }
    calls(depends);
  }

  public void setRecursive() {
    dt.setRecursive();
  }

  public void kindInfer(Handler handler) {
    // The type expressions for every constructor should be well-kinded.
    for (int i = 0; i < constrs.length; i++) {
      if (constrs[i] != null) { // (guards against continuing after previously detected error)
        constrs[i].kindInfer(handler);
      }
    }
  }

  public void fixKinds() {
    params.fixKinds();
    dt.fixKinds();
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    // Calculate the range type for constructors in this type
    Type rng = dt.asType();
    for (int i = 0; i < args.length; i++) {
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

  public void addToMILEnv(Handler handler, MILEnv milenv) {
    dt.addCfunsTo(handler, milenv);
  }
}
