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

public class StructDefn extends TyconDefn {

  TypeExp sizeExp;

  TypeExp alignExp;

  private StructRegionExp[] regexps;

  /** Default constructor. */
  public StructDefn(
      Position pos, String id, TypeExp sizeExp, TypeExp alignExp, StructRegionExp[] regexps) {
    super(pos, id);
    this.sizeExp = sizeExp;
    this.alignExp = alignExp;
    this.regexps = regexps;
  }

  private StructType st;

  /**
   * Return the Tycon associated with this definition, if any. TODO: this method is only used in one
   * place, and it's awkward ... look for opportunities to rewrite
   */
  public Tycon getTycon() {
    return st;
  }

  public void introduceTycons(Handler handler, TyconEnv env) {
    env.add(st = new StructType(pos, id));
  }

  /**
   * Determine the list of type definitions (a sublist of defns) that this particular definition
   * depends on.
   */
  public void scopeTycons(Handler handler, CoreDefns defns, TyconEnv env) throws Failure {
    CoreDefns depends = null;
    if (sizeExp != null) {
      sizeExp = sizeExp.tidyInfix(env);
      depends = sizeExp.scopeTyconsType(handler, null, env, defns, depends);
    }
    if (alignExp != null) {
      alignExp = alignExp.tidyInfix(env);
      depends = alignExp.scopeTyconsType(handler, null, env, defns, depends);
    }
    for (int i = 0; i < regexps.length; i++) {
      depends = regexps[i].scopeTycons(handler, null, env, defns, depends);
    }
    this.calls(depends);
  }

  public void kindInfer(Handler handler) {
    if (sizeExp != null) {
      sizeExp.checkKind(handler, KAtom.NAT);
    }
    if (alignExp != null) {
      alignExp.checkKind(handler, KAtom.NAT);
    }
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].kindInfer(handler);
    }
  }

  public void fixKinds() {
    st.fixKinds();
  }

  /** Initialize size information for this definition, if appropriate. */
  void initSizes(Handler handler) {
    try { // TODO: merge this code with the above (only difference is in names st and bt)
      st.setByteSize((sizeExp == null) ? new TVar(Tyvar.nat) : sizeExp.toType(null));
      if (alignExp != null) {
        st.setAlignment(alignExp.calcAlignment());
      }
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Initialize linear equation to calculate size information for this definition, if appropriate.
   */
  public LinearEqns initEqns(Handler handler, LinearEqns eqns) {
    try {
      Type end = st.byteSize(); // size is offset after the last region
      String label = "Size of " + st.getId();
      LinearEqn eqn = new LinearEqn(pos, label);
      for (int i = regexps.length; --i >= 0; ) { // Add an equation for each region
        eqns = new LinearEqns(regexps[i].structRegionEqn(pos, end, label), eqns);
        end = regexps[i].getStart();
        label = "Offset of " + regexps[i].makeLabel();
      }
      // Add equation that requires final value of end to be zero
      eqn.addTerm(end, label);
      return new LinearEqns(eqn, eqns);
    } catch (Failure f) {
      handler.report(f);
    }
    return eqns;
  }

  void checkSizes() throws Failure {
    // Check that we have computed a valid ByteSize for this structure:
    Type size = st.byteSize().simplifyNatType(null);
    BigInteger nat = size.getNat();
    if (nat == null) {
      throw new ByteSizeNotDeterminedFailure(pos, st);
    }
    st.setByteSize(size); // save simplified size value
    debug.Log.println("ByteSize(" + st + ") = " + nat);

    // Validate the width and offset of each region:
    for (int i = 0; i < regexps.length; i++) {
      regexps[i].validate();
    }

    // Calculate the number of fields for this structure:
    int n = 0;
    for (int i = 0; i < regexps.length; i++) {
      n += regexps[i].numFields();
    }

    // Build an array of fields for this structure:
    StructField[] fields = new StructField[n];
    int next = 0;
    for (int i = 0; i < regexps.length; i++) {
      next = regexps[i].collectFields(fields, next);
    }
    st.setFields(fields);

    // Calculate the minimal alignment for this structure and validate field alignment/offsets:
    long minAlignment = 1;
    for (int i = 0; i < fields.length; i++) {
      StructField f = fields[i];
      Type t = f.getType(); // Find the type of this field
      long align = t.alignment(null); // Find the alignment of this field
      if (align < 1) {
        throw new Failure(
            f.getPos(), "Unable to determine alignment for field " + f.getId() + " :: " + t);
      }
      int offset = f.getOffset(); // Check that field alignment divides field offset
      if ((((long) offset) % align) != 0) {
        throw new Failure(
            f.getPos(),
            "Cannot access field "
                + f.getId()
                + " (offset "
                + offset
                + " is not divisible by alignment "
                + align
                + ")");
      }
      minAlignment = lcm(minAlignment, align); // Update minimal alignment
      f.generateSelector(st); // Construct an update primitive
      debug.Log.println("Field " + f.getId() + ": offset=" + offset + ", alignment=" + align);
    }

    // Validate declared alignment, if specified:
    if (alignExp == null) {
      st.setAlignment(minAlignment);
    } else {
      alignExp.checkAlignment(st.getAlignment(), minAlignment);
    }
    debug.Log.println("Structure " + st + " alignment=" + st.getAlignment());
  }

  /** Utility function to calculate the least common multiple (LCM) of two alignment values. */
  private static long lcm(long a, long b) {
    return a * (b / gcd(a, b));
  }

  /** Utility function to calculate the greatest common divisor (GCD) of two alignment values. */
  private static long gcd(long a, long b) {
    while (b > 0) {
      long r = a % b;
      a = b;
      b = r;
    }
    return a;
  }

  /** Calculate types for each of the values that are introduced by this definition. */
  public void calcCfuns(Handler handler) {
    st.debugDump();
  }

  /**
   * Extend the specified MIL environment with entries for any functions/values introduced in this
   * definition.
   */
  public void addToMILEnv(Handler handler, CoreProgram prog, MILEnv milenv) {
    /* Nothing to do here! */
  }

  public TopBindings coreBindings(TopBindings tbs) {
    for (int i = 0; i < regexps.length; i++) {
      tbs = regexps[i].coreBindings(tbs);
    }
    return tbs;
  }
}
