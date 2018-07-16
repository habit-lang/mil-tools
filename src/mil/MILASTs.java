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
import core.*;

public class MILASTs {

  public MILAST head;

  public MILASTs next;

  /** Default constructor. */
  public MILASTs(MILAST head, MILASTs next) {
    this.head = head;
    this.next = next;
  }

  /**
   * Depth-first search the forward dependency graph. Returns a list with the same Xs in reverse
   * order of finishing times. (In other words, the last node that we finish visiting will be the
   * first node in the result list.)
   */
  static MILASTs searchForward(MILASTs xs, MILASTs result) {
    for (; xs != null; xs = xs.next) {
      result = xs.head.forwardVisit(result);
    }
    return result;
  }

  /**
   * Depth-first search the reverse dependency graph, using the list of bindings that was obtained
   * in the forward search, with the latest finishers first.
   */
  static MILASTSCCs searchReverse(MILASTs xs) {
    MILASTSCCs sccs = null;
    for (; xs != null; xs = xs.next) {
      if (xs.head.getScc() == null) {
        MILASTSCC scc = new MILASTSCC();
        sccs = new MILASTSCCs(scc, sccs);
        xs.head.reverseVisit(scc);
      }
    }
    return sccs;
  }

  /**
   * Calculate the strongly connected components of a list of Xs that have been augmented with
   * dependency information.
   */
  public static MILASTSCCs scc(MILASTs xs) {
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
}
