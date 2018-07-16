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

public class BitdataDefn extends TyconDefn {

  TypeExp sizeExp;

  private BitdataConDefn[] constrs;

  /** Default constructor. */
  public BitdataDefn(Position pos, String id, TypeExp sizeExp, BitdataConDefn[] constrs) {
    super(pos, id);
    this.sizeExp = sizeExp;
    this.constrs = constrs;
  }

  private BitdataName bn;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return bn;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    env.add(bn = new BitdataName(pos, id, KAtom.STAR, 0)); // TODO: shouldn't require arity!
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
    bn.fixKinds();
  }

  /** Initialize size information for this definition, if appropriate. */
  void initSizes(Handler handler) {
    try {
      bn.setBitSize((sizeExp == null) ? new TVar(Tyvar.nat) : sizeExp.toType(null));
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
        eqns = new LinearEqns(constrs[i].initEqn(bn.bitSize(), bn), eqns);
      }
    } catch (Failure f) {
      handler.report(f);
    }
    return eqns;
  }

  void checkSizes() throws Failure {
    // Check that we have computed a valid BitSize for this type
    Type size = bn.bitSize().simplifyNatType(null);
    BigInteger nat = size.getNat();
    if (nat == null) {
      throw new BitSizeNotDeterminedFailure(pos, bn);
    }
    int w = nat.intValue();
    if (w < 0 || w > Type.MAXWIDTH) {
      throw new InvalidWidthFailure(pos, bn, nat);
    }
    bn.setBitSize(size); // save simplified size value
    debug.Log.println("BitSize(" + bn + ") = " + nat);

    // Calculate region lists for each of the constructors and a bit pattern for the full type:
    obdd.Pat pat = obdd.Pat.empty(w);
    for (int i = 0; i < constrs.length; i++) {
      pat = constrs[i].calcLayout(bn).or(pat);
    }
    bn.setPat(pat);

    // Test for junk:
    obdd.Pat junk = pat.not();
    if (!junk.isEmpty()) {
      BigInteger n = junk.size();
      debug.Log.println(
          "Warning: bitdata type "
              + bn
              + " includes "
              + ((n.compareTo(BigInteger.ONE) == 0) ? "a junk value" : (n + " junk values")));
      // !   String[] lines = junk.showBits();
      // !   for (int i=0; i<lines.length; i++) {
      // !     debug.Log.println("  " + lines[i]);
      // !   }
    }

    // Test for confusion and for the existence of mask-test predicates:
    // TODO: we could make a special case for single constructor types, none of which
    // require checking for confusion or a search for a mask-test predicate
    for (int i = 0; i < constrs.length; i++) {
      BitdataConDefn.calcMaskTest(constrs, i, w);
    }
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    BitdataConDefn.calcCfuns(bn, constrs);
  }

  public void addToMILEnv(Handler handler, MILEnv milenv) {
    bn.addCfunsTo(handler, milenv);
  }

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    for (int i = 0; i < constrs.length; i++) {
      constrs[i].inScopeOf(handler, milenv, env);
    }
  }

  public void inferTypes(Handler handler) throws Failure {
    for (int i = 0; i < constrs.length; i++) {
      constrs[i].inferTypes(handler);
    }
  }

  public void lift(LiftEnv lenv) {
    for (int i = 0; i < constrs.length; i++) {
      constrs[i].lift(lenv);
    }
  }
}
