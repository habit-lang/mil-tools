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
import java.math.BigInteger;
import lc.TopBindings;
import mil.*;

public class BitdataDefn extends TyconDefn {

  TypeExp sizeExp;

  private BitdataConDefn[] constrs;

  /** Default constructor. */
  public BitdataDefn(Position pos, String id, TypeExp sizeExp, BitdataConDefn[] constrs) {
    super(pos, id);
    this.sizeExp = sizeExp;
    this.constrs = constrs;
  }

  private BitdataType bt;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return bt;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    env.add(bt = new BitdataType(pos, id));
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) {
    CoreDefns depends = null;
    if (sizeExp != null) {
      depends = sizeExp.scopeTycons(handler, null, env, defns, depends);
    }
    for (int i = 0; i < constrs.length; i++) {
      depends = constrs[i].scopeTycons(handler, null, env, defns, depends);
    }
    calls(depends);
  }

  public void kindInfer(Handler handler) {
    if (sizeExp != null) {
      sizeExp.checkKind(handler, KAtom.NAT);
    }
    for (int i = 0; i < constrs.length; i++) {
      constrs[i].kindInfer(handler);
    }
  }

  public void fixKinds() {
    bt.fixKinds();
  }

  void undeterminedBitPat() throws Failure {
    throw new UndeterminedBitPatternFailure(pos, bt.asType());
  }

  /** Initialize size information for this definition, if appropriate. */
  void initSizes(Handler handler) {
    try {
      bt.setBitSize((sizeExp == null) ? new TVar(Tyvar.nat) : sizeExp.toType(null));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Initialize linear equation to calculate size information for this definition, if appropriate.
   */
  public LinearEqns initEqns(Handler handler, LinearEqns eqns) {
    try {
      for (int i = 0; i < constrs.length; i++) {
        eqns = new LinearEqns(constrs[i].initEqn(bt.bitSize()), eqns);
      }
    } catch (Failure f) {
      handler.report(f);
    }
    return eqns;
  }

  private int width;

  void checkSizes() throws Failure {
    // Check that we have computed a valid BitSize for this type
    // TODO: combine with very similar code in Type.bitWidth()?
    Type size = bt.bitSize().simplifyNatType(null);
    BigInteger nat = size.getNat();
    if (nat == null) {
      throw new BitSizeNotDeterminedFailure(pos, bt);
    } else if (nat.signum() < 0 || nat.compareTo(Type.BIG_MAX_BIT_WIDTH) > 0) {
      throw new InvalidWidthFailure(pos, bt, nat);
    }
    width = nat.intValue();
    bt.setBitSize(size); // save simplified size value
    debug.Log.println("BitSize(" + bt + ") = " + nat);
  }

  BitdataDefn checkPats() throws Failure {
    // Calculate region lists for each of the constructors and a bit pattern for the full type:
    if (constrs.length == 0) {
      bt.setPat(obdd.Pat.all(width).restrict());
    } else {
      obdd.Pat pat = obdd.Pat.empty(width);
      for (int i = 0; i < constrs.length; i++) {
        obdd.Pat cpat = constrs[i].calcLayout(bt);
        if (cpat == null) {
          return this; // not yet able to determine bit pattern
        }
        pat = constrs[i].calcLayout(bt).or(pat);
      }
      bt.setPat(pat);

      // Test for junk:
      if (!pat.isAll()) {
        obdd.Pat junk = pat.not();
        BigInteger n = junk.size();
        debug.Log.println(
            "Warning: bitdata type "
                + bt
                + " includes "
                + ((n.compareTo(BigInteger.ONE) == 0) ? "a junk value" : (n + " junk values")));
      }

      // Test for confusion and for the existence of mask-test predicates:
      // TODO: we could make a special case for single constructor types, none of which
      // require checking for confusion or a search for a mask-test predicate
      for (int i = 0; i < constrs.length; i++) {
        BitdataConDefn.calcMaskTest(constrs, i, width);
      }
    }
    return null;
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    BitdataConDefn.calcCfuns(bt, constrs);
  }

  /**
   * Extend the specified MIL environment with entries for any functions/values introduced in this
   * definition.
   */
  public void addToMILEnv(Handler handler, CoreProgram prog, MILEnv milenv) {
    Cfun[] cfuns = getTycon().getCfuns();
    for (int i = 0; i < cfuns.length; i++) {
      try {
        milenv.addCfunAndTop(cfuns[i]);
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  public TopBindings coreBindings(TopBindings tbs) {
    for (int i = 0; i < constrs.length; i++) {
      tbs = constrs[i].coreBindings(tbs);
    }
    return tbs;
  }
}
