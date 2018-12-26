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

/**
 * Lists of Facts are used to represent sets of "facts", each of which is a pair (v = t) indicating
 * that the variable v has most recently been bound by the specified tail t (which should be either
 * an allocator or a pure primitive call). We can use lists of facts like this to perform dataflow
 * analysis and optimizations on Code sequences.
 */
public class Facts {

  private Temp v;

  private Tail t;

  private Facts next;

  /** Default constructor. */
  public Facts(Temp v, Tail t, Facts next) {
    this.v = v;
    this.t = t;
    this.next = next;
  }

  /**
   * Remove any facts that are killed as a result of binding the variable v. The returned list will
   * be the same as the input list vs if, and only if there are no changes to the list of facts. In
   * particular, this implies that we will not use any destructive updates, but it also allows us to
   * avoid unnecessarily reallocating copies of the same list when there are no changes, which we
   * expect to be the common case.
   */
  public static Facts kills(Temp v, Facts facts) {
    if (facts != null) {
      Facts fs = Facts.kills(v, facts.next);
      // A binding for the variable v kills any fact (w = t) that mentions v.
      if (facts.v == v || facts.t.contains(v)) {
        // Head item in facts is killed by binding v, so do not include it in the return result:
        return fs;
      } else if (fs != facts.next) {
        // Some items in facts.next were killed, but the head item in facts is not, so we
        // create a new list that retains the head fact together with the facts left in fs:
        return new Facts(facts.v, facts.t, fs);
      }
    }
    return facts;
  }

  /**
   * Kill any facts with tails that are not pure. In particular, this removes any facts for tails
   * that are observers, which means that they may be clobbered/invalidated by a tail that
   * (potentially) has an effect.
   */
  public static Facts killNonPure(Facts facts) {
    if (facts != null) {
      Facts fs = Facts.killNonPure(facts.next);
      if (!facts.t.isPure()) { // Head item is not pure, so exclude from result.
        return fs;
      } else if (fs != facts.next) {
        return new Facts(facts.v, facts.t, fs);
      }
    }
    return facts;
  }

  /** Look for a fact about a specific variable. */
  public static Tail lookupFact(Temp v, Facts facts) {
    for (; facts != null; facts = facts.next) {
      if (facts.v == v) {
        return facts.t;
      }
    }
    return null;
  }

  /**
   * Look for a previous computation of the specified tail in the current set of facts; note that
   * the set of facts should only contain pure computations (allocators and pure primitive calls).
   */
  public static Temp find(Tail t, Facts facts) {
    for (; facts != null; facts = facts.next) {
      if (facts.t.sameTail(t)) {
        return facts.v;
      }
    }
    return null;
  }
}
