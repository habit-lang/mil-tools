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
import lc.Env;
import lc.LiftEnv;
import mil.*;

public class CoreProgram {

  private CoreDefns coreDefns = null;

  private CoreDefns coreDefnsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(CoreDefn elem) {
    CoreDefns ns = new CoreDefns(elem, null);
    coreDefnsLast = (coreDefnsLast == null) ? (coreDefns = ns) : (coreDefnsLast.next = ns);
  }

  public TyconEnv typeEnv(Handler handler, TyconEnv enclosing) throws Failure {
    TyconEnv tenv = new TyconEnv(enclosing);

    // Step 1: Add type entries for each definition that introduces one or more tycon names.
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.introduceTycons(handler, tenv);
    }
    handler.abortOnFailures();

    // Step 2: Check that there is at most one definition for any given name.
    tenv.checkForMultipleTycons(handler);
    handler.abortOnFailures();

    // Step 3: Run scope analysis and compute dependencies for tycon names.
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.scopeTycons(handler, coreDefns, tenv);
    }
    handler.abortOnFailures();

    // Step 4: Compute SCCs.
    CoreDefnSCCs sccs = CoreDefns.scc(coreDefns);

    // Step 5: Perform kind inference.
    for (CoreDefnSCCs ss = sccs; ss != null; ss = ss.next) {
      CoreDefns defns = ss.head.getBindings();
      // Check for a recursive binding group:
      if (ss.head.isRecursive()) {
        for (CoreDefns ds = defns; ds != null; ds = ds.next) {
          ds.head.setRecursive();
        }
      }
      // Run kind inference over all of the type definitions in this SCC:
      for (CoreDefns ds = defns; ds != null; ds = ds.next) {
        ds.head.kindInfer(handler);
      }
      // Eliminate any remaining variables in inferred kinds:
      for (CoreDefns ds = defns; ds != null; ds = ds.next) {
        ds.head.fixKinds();
      }
    }
    handler.abortOnFailures();

    // Step 6: Calculate size and layout information ...
    for (CoreDefnSCCs ss = sccs; ss != null; ss = ss.next) {
      ss.head.calcSizes(handler);
    }
    handler.abortOnFailures();

    // Step 7: Calculate types for constructor functions, etc...
    for (CoreDefnSCCs ss = sccs; ss != null; ss = ss.next) {
      for (CoreDefns ds = ss.head.getBindings(); ds != null; ds = ds.next) {
        ds.head.calcCfuns(handler);
      }
    }
    handler.abortOnFailures();

    return tenv;
  }

  public MILEnv newmil(Handler handler, TyconEnv tenv, MILEnv milenv) {
    milenv = new MILEnvChain(tenv, milenv);
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.addToMILEnv(handler, milenv);
    }
    return milenv;
  }

  public void inScopeOf(Handler handler, MILEnv milenv, Env env) throws Failure {
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.inScopeOf(handler, milenv, env);
    }
  }

  public void inferTypes(Handler handler) throws Failure {
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.inferTypes(handler);
    }
  }

  public void liftCoreProgram(LiftEnv lenv) {
    for (CoreDefns ds = coreDefns; ds != null; ds = ds.next) {
      ds.head.lift(lenv);
    }
  }
}
