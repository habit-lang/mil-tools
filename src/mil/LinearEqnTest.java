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
package mil;

import compiler.*;
import compiler.BuiltinPosition;
import compiler.Failure;
import compiler.SimpleHandler;
import core.*;

public class LinearEqnTest {

  public static void main(String[] args) {
    LinearEqn e = new LinearEqn(BuiltinPosition.pos);
    System.out.println("e = " + e);

    TVar x = new TVar(Tyvar.nat);
    e.addTerm(1, x, "x");
    System.out.println("e:  " + e);

    TVar y = new TVar(Tyvar.nat);
    e.addTerm(2, y, "y");
    System.out.println("e:  " + e);

    e.addTerm(1, x, "x");
    System.out.println("e:  " + e);

    TVar z = new TVar(Tyvar.nat);
    e.addTerm(3, z, "z");
    System.out.println("e:  " + e);

    e.addTerm(-4, z, "z");
    System.out.println("e:  " + e);

    e.addTerm(-2, y, "y");
    System.out.println("e:  " + e);

    e.addTerm(-1, x, "x");
    System.out.println("e:  " + e);

    e.addTerm(1, z, "z");
    System.out.println("e:  " + e);

    LinearEqn e1 = new LinearEqn(BuiltinPosition.pos);
    LinearEqn e2 = new LinearEqn(BuiltinPosition.pos);
    LinearEqn e3 = new LinearEqn(BuiltinPosition.pos);
    LinearEqns eqns = new LinearEqns(e3, new LinearEqns(e2, new LinearEqns(e1, null)));

    e1.addTerm(1, x, "x");
    e1.addTerm(2, y, "y");
    e1.addConst(-(1 * 7 + 2 * 3 + 0 * 8));
    e2.addTerm(2, x, "x");
    e2.addTerm(1, z, "z");
    e2.addConst(-(2 * 7 + 0 * 3 + 1 * 8));
    e3.addTerm(1, y, "y");
    e3.addTerm(4, z, "z");
    e3.addConst(-(0 * 7 + 1 * 3 + 4 * 8));
    try {
      LinearEqns.solve(eqns);
      System.out.println(
          "Solution is: "
              + x
              + " = "
              + x.skeleton()
              + ", "
              + y
              + " = "
              + y.skeleton()
              + ", "
              + z
              + " = "
              + z.skeleton());
    } catch (Failure f) {
      new SimpleHandler().report(f);
    }
  }
}
