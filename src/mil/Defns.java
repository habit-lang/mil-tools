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
import compiler.Failure;
import compiler.Handler;
import core.*;

public class Defns {

  public Defn head;

  public Defns next;

  /** Default constructor. */
  public Defns(Defn head, Defns next) {
    this.head = head;
    this.next = next;
  }

  /**
   * Depth-first search the forward dependency graph. Returns a list with the same Xs in reverse
   * order of finishing times. (In other words, the last node that we finish visiting will be the
   * first node in the result list.)
   */
  static Defns searchForward(Defns xs, Defns result) {
    for (; xs != null; xs = xs.next) {
      result = xs.head.forwardVisit(result);
    }
    return result;
  }

  /**
   * Depth-first search the reverse dependency graph, using the list of bindings that was obtained
   * in the forward search, with the latest finishers first.
   */
  static DefnSCCs searchReverse(Defns xs) {
    DefnSCCs sccs = null;
    for (; xs != null; xs = xs.next) {
      if (xs.head.getScc() == null) {
        DefnSCC scc = new DefnSCC();
        sccs = new DefnSCCs(scc, sccs);
        xs.head.reverseVisit(scc);
      }
    }
    return sccs;
  }

  /**
   * Calculate the strongly connected components of a list of Xs that have been augmented with
   * dependency information.
   */
  public static DefnSCCs scc(Defns xs) {
    /*
          // Compute the transpose (i.e., fill in the callers fields)
          for (X\s bs=xs; bs!=null; bs=bs.next) {
            for (X\s cs=bs.head.callees; cs!=null; cs=cs.next) {
              cs.head.callers = new X\s(bs.head, cs.head.callers);
            }
          }

          debug.Log.println("Beginning SCC algorithm");
          for (X\s bs=xs; bs!=null; bs=bs.next) {
            debug.Log.print(bs.head.getId() + ": callees {");
            String punc = "";
            for (X\s cs = bs.head.callees; cs!=null; cs=cs.next) {
              debug.Log.print(punc);
              punc = ", ";
              debug.Log.print(cs.head.getId());
            }
            debug.Log.print("}, callers {");
            punc = "";
            for (X\s cs = bs.head.callers; cs!=null; cs=cs.next) {
              debug.Log.print(punc);
              punc = ", ";
              debug.Log.print(cs.head.getId());
            }
            debug.Log.println("}");
          }
    */

    // Run the two depth-first searches of the main algorithm.
    return searchReverse(searchForward(xs, null));
  }

  /** Test for membership in a list. */
  public static boolean isIn(Defn val, Defns list) {
    for (; list != null; list = list.next) {
      if (list.head == val) {
        return true;
      }
    }
    return false;
  }

  /** Return the length of a linked list of elements. */
  public static int length(Defns list) {
    int len = 0;
    for (; list != null; list = list.next) {
      len++;
    }
    return len;
  }

  static void inferTypes(Handler handler, Defns defns) throws Failure {
    // Step 1: set the type of each item in this binding group to an appropriate initial type
    // (typically an
    // instance of the declared type, or a fresh type skeleton if there is no declared type).
    for (Defns ds = defns; ds != null; ds = ds.next) {
      ds.head.setInitialType();
    }

    // Step 2: run type inference on the body of each definition in this SCC.
    for (Defns ds = defns; ds != null; ds = ds.next) {
      // !   ds.head.displayDefn();
      ds.head.checkBody(handler);
    }

    // Step 3: calculate most general types for each definition in this SCC.
    for (Defns ds = defns; ds != null; ds = ds.next) {
      ds.head.generalizeType(handler);
    }
  }
}
