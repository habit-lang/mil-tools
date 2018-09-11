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
import java.math.BigInteger;
import lc.Env;
import lc.LiftEnv;
import mil.*;

class BitdataConDefn extends Name {

  private BitdataRegionExp[] regexps;

  /** Default constructor. */
  BitdataConDefn(Position pos, String id, BitdataRegionExp[] regexps) {
    super(pos, id);
    this.regexps = regexps;
  }

  /**
   * Perform scope analysis on portions of a type constructor definition, returning a list with the
   * elements from defns that it references.
   */
  CoreDefns scopeTycons(
      Handler handler, TyvarEnv params, TyconEnv env, CoreDefns defns, CoreDefns depends) {
    for (int i = 0; i < regexps.length; i++) {
      depends = regexps[i].scopeTycons(handler, params, env, defns, depends);
    }
    return depends;
  }

  public void kindInfer(Handler handler) {
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].kindInfer(handler);
    }
  }

  LinearEqn initEqn(Type size, Object hint) throws Failure {
    LinearEqn eqn = new LinearEqn(pos); // Create an empty equation
    eqn.addRhsTerm(size, hint); // Add the size on the right hand side
    for (int i = 0; i < regexps.length; i++) { // Add terms for the other regions
      regexps[i].addTermTo(eqn);
    }
    return eqn;
  }

  private BitdataLayout layout;

  obdd.Pat calcLayout(BitdataType bt) throws Failure {
    // TODO: it is ugly to use four separate loops and transient width and offset fields
    // in each BitdataRegionExp; but is there a cleaner way to do this?

    // Calculate offsets, widths, and bit pattern for this constructor:
    obdd.Pat pat = obdd.Pat.all(0);
    for (int i = regexps.length; --i >= 0; ) {
      pat = regexps[i].calcPat(pat);
    }

    // Calculate the tagbits for this constructor:
    BigInteger tagbits = BigInteger.ZERO;
    for (int i = regexps.length; --i >= 0; ) {
      tagbits = regexps[i].calcTagbits(tagbits);
    }

    // Calculate the number of fields for this constructor:
    int n = 0;
    for (int i = 0; i < regexps.length; i++) {
      n += regexps[i].numFields();
    }

    // Build an array of fields for this constructor:
    BitdataField[] fields = new BitdataField[n];
    int next = 0;
    for (int i = 0; i < regexps.length; i++) {
      next = regexps[i].collectFields(fields, next);
    }

    layout = new BitdataLayout(pos, id, bt, tagbits, fields, pat);
    return pat;
  }

  /**
   * Calculate a suitable mask test predicate for constructor i, given the list of all constructors
   * and the associated width, w. Reports an error if no suitable mask test predicate is found, or
   * if the type has confusion.
   */
  static void calcMaskTest(BitdataConDefn[] constrs, int i, int w) throws Failure {
    BitdataConDefn ci = constrs[i];
    obdd.Pat cpat = ci.layout.getPat(); // the associated bit pattern
    obdd.MaskTestPat eq = cpat.masktest(false); // a candidate mask-test predicate using ==
    int ceq = (-1); // constructor # that conflicts with eq, if any
    obdd.MaskTestPat neq = cpat.masktest(true); // a candidate mask-test predicate using /=
    int cneq = (-1); // constructor # that conflicts with neq, if any
    obdd.Pat bnot = obdd.Pat.empty(w); // patterns from other constructors

    for (int j = 0; j < constrs.length; j++) {
      if (i != j) {
        BitdataConDefn cj = constrs[j];
        obdd.Pat dpat = cj.layout.getPat();
        bnot = bnot.or(dpat);
        if (!cpat.disjoint(dpat)) {
          throw new CfunConfusionFailure(ci, cj, cpat.and(dpat));
        }
        if (ceq < 0 && !eq.disjoint(dpat)) { // Does eq reject all of dpat?
          ceq = j; // If not, then eq is not usable because it conflicts with constr[j]
        }
        if (cneq < 0 && !neq.disjoint(dpat)) { // Does neq reject all of dpat?
          cneq = j; // If not, then neq is not usable because it conflicts with constr[j]
        }
      }
    }

    // Return one of the surviving mask-test predicates:
    obdd.MaskTestPat test;
    if (ceq < 0) {
      test = eq.blur(bnot);
    } else if (cneq < 0) {
      test = neq.blur(bnot);
    } else {
      throw new NoMaskTestPredicateFailure(ci, constrs[ceq], constrs[cneq]);
    }
    debug.Log.println(test.toString(constrs[i].id));
    constrs[i].layout.setMaskTest(test);
  }

  static void calcCfuns(BitdataType bt, BitdataConDefn[] constrs) {
    Cfun[] cfuns = new Cfun[constrs.length];
    BitdataLayout[] layouts = new BitdataLayout[constrs.length];
    for (int i = 0; i < constrs.length; i++) {
      BitdataConDefn ci = constrs[i];
      cfuns[i] = new Cfun(ci.pos, ci.id, bt, i, ci.layout.cfunType());
      layouts[i] = ci.layout;
      debug.Log.println(cfuns[i] + " :: " + cfuns[i].getAllocType());
      layouts[i].debugDump();
    }
    bt.setCfuns(cfuns);
    bt.setLayouts(layouts);
  }

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].inScopeOf(handler, milenv, env);
    }
  }

  public void inferTypes(Handler handler) throws Failure {
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].inferTypes(handler);
    }
  }

  public void lift(LiftEnv lenv) {
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].lift(lenv);
    }
  }
}
