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
import compiler.Position;
import core.*;
import java.io.PrintWriter;

public class PrimCall extends Call {

  private Prim p;

  /** Default constructor. */
  public PrimCall(Prim p) {
    this.p = p;
  }

  /** Test to determine whether a given tail expression has no externally visible side effect. */
  public boolean hasNoEffect() {
    return p.hasNoEffect();
  }

  /** Test if two Tail expressions are the same. */
  public boolean sameTail(Tail that) {
    return that.samePrimCall(this);
  }

  boolean samePrimCall(PrimCall that) {
    return this.p == that.p && this.sameArgs(that);
  }

  /** Display a printable representation of this MIL construct on the specified PrintWriter. */
  public void dump(PrintWriter out, Temps ts) {
    dump(out, p.getId(), "((", args, "))", ts);
  }

  /** Construct a new Call value that is based on the receiver, without copying the arguments. */
  Call callDup(Atom[] args) {
    return p.withArgs(args);
  }

  private BlockType type;

  /** Calculate the list of unbound type variables that are referenced in this MIL code fragment. */
  TVars tvars(TVars tvs) {
    return type.tvars(tvs);
  }

  /** Return the type tuple describing the result that is produced by executing this Tail. */
  public Type resultType() {
    return type.rngType();
  }

  Type inferCallType(Position pos, Type[] inputs) throws Failure {
    type = p.instantiate();
    return type.apply(pos, inputs);
  }

  void invokeCall(MachineBuilder builder, int o) {
    builder.prim(o, p.getIndex());
  }

  /**
   * Determine whether a pair of given Call values are of the same "form", meaning that they are of
   * the same type with the same target (e.g., two block calls to the same block are considered as
   * having the same form, but a block call and a data alloc do not have the same form, and neither
   * do two block calls to distinct blocks. As a special case, two Returns are considered to be of
   * the same form only if they have the same arguments.
   */
  boolean sameCallForm(Call c) {
    return c.samePrimCallForm(this);
  }

  boolean samePrimCallForm(PrimCall that) {
    return that.p == this.p;
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return p.doesntReturn();
  }

  /**
   * Return true if this code enters a non-productive black hole (i.e., immediately calls halt or
   * loop).
   */
  boolean blackholes() {
    return p.blackholes();
  }

  boolean noinline() {
    return p == Prim.noinline;
  }

  /**
   * Skip goto blocks in a Tail (for a ClosureDefn or TopLevel). TODO: can this be simplified now
   * that ClosureDefns hold Tails rather than Calls?
   */
  public Tail inlineTail() {
    Code c = this.rewritePrimCall(null);
    return (c == null) ? this : c.forceToTail(this);
  }

  /**
   * Test to determine whether a given tail expression may be repeatable (i.e., whether the results
   * of a previous use of the same tail can be reused instead of repeating the tail). TODO: is there
   * a better name for this?
   */
  public boolean isRepeatable() {
    return p.isRepeatable();
  }

  /**
   * Test to determine whether a given tail expression is pure (no externally visible side effects
   * and no dependence on other effects).
   */
  public boolean isPure() {
    return p.isPure();
  }

  Atom isBnot() {
    return p == Prim.bnot ? args[0] : null;
  }

  public Code rewrite(Facts facts) {
    return this.rewritePrimCall(facts);
  }

  Code rewritePrimCall(Facts facts) {

    if (p == Prim.bnot) {
      Atom x = args[0];
      Flag a = x.isFlag();
      return (a == null) ? bnotVar(x, facts) : Prim.bnot.fold(a.getVal());
    }

    if (p == Prim.not) {
      Atom x = args[0];
      Word a = x.isWord();
      return (a == null) ? notVar(x, facts) : Prim.not.fold(a.getVal());
    }

    if (p == Prim.neg) {
      Atom x = args[0];
      Word a = x.isWord();
      return (a == null) ? negVar(x, facts) : Prim.neg.fold(a.getVal());
    }

    if (p == Prim.flagToWord) {
      Atom x = args[0];
      Flag a = x.isFlag();
      return (a == null) ? flagToWordVar(x, facts) : Prim.flagToWord.fold(a.getVal());
    }

    if (p == Prim.add) {
      Atom x = args[0], y = args[1];
      Word a = x.isWord(), b = y.isWord();
      if (a == null) {
        return (b == null) ? addVarVar(x, y, facts) : addVarConst(x, b.getVal(), facts);
      } else if (b == null) {
        Code nc = addVarConst(y, a.getVal(), facts);
        return (nc != null) ? nc : done(p, y, x);
      } else {
        return Prim.add.fold(a.getVal(), b.getVal());
      }
    }

    if (p == Prim.mul) {
      Atom x = args[0], y = args[1];
      Word a = x.isWord(), b = y.isWord();
      if (a == null) {
        return (b == null) ? mulVarVar(x, y, facts) : mulVarConst(x, b.getVal(), facts);
      } else if (b == null) {
        Code nc = mulVarConst(y, a.getVal(), facts);
        return (nc != null) ? nc : done(p, y, x);
      } else {
        return Prim.mul.fold(a.getVal(), b.getVal());
      }
    }

    if (p == Prim.or) {
      Atom x = args[0], y = args[1];
      Word a = x.isWord(), b = y.isWord();
      if (a == null) {
        return (b == null) ? orVarVar(x, y, facts) : orVarConst(x, b.getVal(), facts);
      } else if (b == null) {
        Code nc = orVarConst(y, a.getVal(), facts);
        return (nc != null) ? nc : done(p, y, x);
      } else {
        return Prim.or.fold(a.getVal(), b.getVal());
      }
    }

    if (p == Prim.and) {
      Atom x = args[0], y = args[1];
      Word a = x.isWord(), b = y.isWord();
      if (a == null) {
        return (b == null) ? andVarVar(x, y, facts) : andVarConst(x, b.getVal(), facts);
      } else if (b == null) {
        Code nc = andVarConst(y, a.getVal(), facts);
        return (nc != null) ? nc : done(p, y, x);
      } else {
        return Prim.and.fold(a.getVal(), b.getVal());
      }
    }

    if (p == Prim.xor) {
      Atom x = args[0], y = args[1];
      Word a = x.isWord(), b = y.isWord();
      if (a == null) {
        return (b == null) ? xorVarVar(x, y, facts) : xorVarConst(x, b.getVal(), facts);
      } else if (b == null) {
        Code nc = xorVarConst(y, a.getVal(), facts);
        return (nc != null) ? nc : done(p, y, x);
      } else {
        return Prim.xor.fold(a.getVal(), b.getVal());
      }
    }

    if (p == Prim.sub) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      Word b = y.isWord();
      return (a == null)
          ? ((b == null) ? subVarVar(x, y, facts) : subVarConst(x, b.getVal(), facts))
          : ((b == null)
              ? subConstVar(a.getVal(), y, facts)
              : Prim.sub.fold(a.getVal(), b.getVal()));
    }

    if (p == Prim.shl) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      Word b = y.isWord();
      return (a == null)
          ? ((b == null) ? shlVarVar(x, y, facts) : shlVarConst(x, b.getVal(), facts))
          : ((b == null)
              ? shlConstVar(a.getVal(), y, facts)
              : Prim.shl.fold(a.getVal(), b.getVal()));
    }

    if (p == Prim.lshr) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      Word b = y.isWord();
      return (a == null)
          ? ((b == null) ? lshrVarVar(x, y, facts) : lshrVarConst(x, b.getVal(), facts))
          : ((b == null)
              ? lshrConstVar(a.getVal(), y, facts)
              : Prim.lshr.fold(a.getVal(), b.getVal()));
    }

    if (p == Prim.ashr) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      Word b = y.isWord();
      return (a == null)
          ? ((b == null) ? ashrVarVar(x, y, facts) : ashrVarConst(x, b.getVal(), facts))
          : ((b == null)
              ? ashrConstVar(a.getVal(), y, facts)
              : Prim.ashr.fold(a.getVal(), b.getVal()));
    }

    // TODO: We use instanceof below because representation transformation can replace nzdiv
    // with a new canonical form that has a different type.  Perhaps we should use instanceof
    // throughout instead of testing for equality with Prim.XXX?
    if (p instanceof Prim.nzdiv || p == Prim.div) {
      Word y = args[1].isWord();
      long d;
      if (y != null && (d = y.getVal()) > 0) { // Look for a constant denominator
        Atom x = args[0];
        Word a = x.isWord();
        if (a != null) {
          long n = a.getVal(); // Look for a constant numerator
          if (n > 0) { // To be cautious, only consider positive values
            MILProgram.report("constant folding for nzdiv");
            return done(new Word(n / d));
          }
        }
        if (d == 1) { // Look for a (redundant) divide by 1
          return done(x);
        }
        if ((d & (d - 1)) == 0) { // Look for division by a power of two, d=(1L<<n)
          int n = 1; // Calculate the value of n
          for (long i = (d >>> 1); (i >>>= 1) > 0; n++) {
            /* no extra work here */
          }
          MILProgram.report("rewrite: nzdiv((x, " + d + ")) ==> lshr((x, " + n + "))");
          return done(Prim.lshr, x, n);
        }
      }
      return null;
    }

    if (p == Prim.eq) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.eq.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.neq) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.neq.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.slt) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.slt.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.sgt) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.sgt.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.sle) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.sle.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.sge) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.sge.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.ult) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.ult.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.ugt) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.ugt.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.ule) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.ule.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.uge) {
      Atom x = args[0];
      Atom y = args[1];
      Word a = x.isWord();
      if (a != null) {
        Word b = y.isWord();
        if (b != null) {
          return Prim.uge.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.beq) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.beq.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.bxor) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.bxor.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.blt) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.blt.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.bgt) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.bgt.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.ble) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.ble.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.bge) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.bge.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.band) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.band.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    if (p == Prim.bor) {
      Atom x = args[0];
      Atom y = args[1];
      Flag a = x.isFlag();
      if (a != null) {
        Flag b = y.isFlag();
        if (b != null) {
          return Prim.bor.fold(a.getVal(), b.getVal());
        }
      }
      return null;
    }

    return null;
  }

  static Code done(Tail t) {
    return new Done(t);
  }

  static Code done(Atom a) {
    return done(new Return(a));
  }

  static Code done(long n) {
    return done(new Word(n));
  }

  static Code done(boolean b) {
    return done(new Flag(b));
  }

  static Code done(Prim p, Atom[] args) {
    return done(p.withArgs(args));
  }

  static Code done(Prim p, Atom a) {
    return done(p.withArgs(a));
  }

  static Code done(Prim p, Atom a, Atom b) {
    return done(p.withArgs(a, b));
  }

  static Code done(Prim p, Atom a, long n) {
    return done(p.withArgs(a, n));
  }

  static Code done(Prim p, long n, Atom b) {
    return done(p.withArgs(n, b));
  }

  /**
   * Test to see if this tail expression is a call to a specific primitive, returning null in the
   * (most likely) case that it is not.
   */
  Atom[] isPrim(Prim p) {
    return (p == this.p) ? args : null;
  }

  private static Code bnotVar(Atom x, Facts facts) {
    Tail a = x.lookupFact(facts);
    return (a != null) ? a.bnotRewrite() : null;
  }

  /** Look for a rewrite of bnot(x) where x is the result of this Tail. */
  Code bnotRewrite() {
    // Eliminate double negation:
    if (p == Prim.bnot) {
      MILProgram.report("eliminated double bnot");
      return done(args[0]); // bnot(bnot(u)) == u
    }

    // Handle negations of relational operators:
    Prim q = p.bnotDual();
    if (q != null) { //  bnot(p(x,y)) --> q(x,y)
      MILProgram.report("replaced bnot(" + p.getId() + "(x,y)) with " + q.getId() + "(x,y)");
      return done(q, args);
    }

    return null; // No further rewrites apply
  }

  private static Code notVar(Atom x, Facts facts) {
    Tail a = x.lookupFact(facts);
    if (a != null) {
      // Eliminate double negation:
      Atom[] ap = a.isPrim(Prim.not);
      if (ap != null) {
        MILProgram.report("eliminated double not");
        return done(ap[0]); // not(not(u)) == u
      }
    }
    return null;
  }

  private static Code negVar(Atom x, Facts facts) {
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap = a.isPrim(Prim.neg);
      if (ap != null) {
        MILProgram.report("rewrite: -(-x) ==> x");
        return done(ap[0]); // neg(neg(u)) == u
      }
      if ((ap = a.isPrim(Prim.sub)) != null) {
        MILProgram.report("rewrite: -(x - y) ==> y - x");
        return done(Prim.sub, ap[1], ap[0]);
      }
      // TODO: -(x * m) ==> x * (-m)   (but careful about large m)
    }
    return null;
  }

  private static Code flagToWordVar(Atom x, Facts facts) {
    return null;
  }

  /** Look for opportunities to simplify an expression using idempotence. */
  private static Code idempotent(Atom x, Atom y) {
    if (x == y) { // simple idempotence
      // TODO: in an expression of the form (x & y), we could further exploit idempotence if x
      // includes an
      // or with y (or vice versa); handling this would require the addition of Prim and a Facts
      // arguments.
      MILProgram.report("rewrite: x ! x ==> x");
      return done(x);
    }
    return null;
  }

  /**
   * Look for opportunities to rewrite an expression involving three operators and two constants as
   * an expression using just two operators and one constant. The specific patterns that we look for
   * are as follows, with p and q representing specific binary operators:
   *
   * <p>p(q(u,c), q(v,d)) == q(p(u,v), p(c,d)) p(q(u,c), v) == q(p(u,v), c) p(u, q(v,d)) == p(p(u,
   * v), c) --- NOTE: no q on rhs
   *
   * <p>These laws hold if p and q are equal and set to a commutative, associative binary operator
   * (add, mul, and, or, xor). But they also hold in at least one other case where p==sub and
   * q==add.
   *
   * <p>The assumptions on entry to this function are that we're trying to optimize an expression of
   * the form p(x, y) having already looked up facts a and b for each of x and y, respectively.
   */
  private static Code redistBin(PrimBinOp p, Prim q, Atom x, Tail a, Atom y, Tail b) {
    Atom[] ap = (a != null) ? a.isPrim(q) : null;
    Word c = (ap != null) ? ap[1].isWord() : null;

    Atom[] bp = (b != null) ? b.isPrim(q) : null;
    Word d = (bp != null) ? bp[1].isWord() : null;

    if (c != null) {
      if (d != null) { // (u `q` c) `p` (w `q` d)
        MILProgram.report("rewrite: (u ! c) ! (w ! d) ==> (u ! w) ! (c ! d)");
        return varVarConst(p, ap[0], bp[0], q, p.op(c.getVal(), d.getVal()));
      } else { // (u `q` c) `p` y
        MILProgram.report("rewrite: (u ! c) ! y==> (u ! y) ! c");
        return varVarConst(p, ap[0], y, q, c.getVal());
      }
    } else if (d != null) { // x `p` (w `q` d)
      MILProgram.report("rewrite: x ! (w ! d) ==> (x ! w) ! d");
      return varVarConst(p, x, bp[0], p, d.getVal());
    }
    return null;
  }

  /**
   * Special case of redistBin for associative, commutative operators where we can use the same
   * primitive for both the p and q parameters.
   */
  private static Code commuteRearrange(PrimBinOp p, Atom x, Tail a, Atom y, Tail b) {
    return redistBin(p, p, x, a, y, b);
  }

  /**
   * Create code for (a ! b) ! n where ! is a primitive p; a and b are variables; and n is a known
   * constant.
   */
  private static Code varVarConst(Prim p, Atom a, Atom b, Prim q, long n) {
    Temp v = new Temp();
    return new Bind(v, p.withArgs(a, b), done(q, v, n));
  }

  /**
   * Generate code for a deMorgan's law rewrite. We are trying to rewrite an expression of the form
   * p(x, y) with an associated (possibly null) fact a for x and b for y. If both a and b are of the
   * form inv(_) for some specific "inverting" primitive, inv, then we can rewrite the whole
   * formula, p(inv(u), inv(v)) as inv(q(u,v)) where q is a "dual" for p. There are (at least) three
   * special cases for this rule: if p=and, then q=or, inv=not: ~p | ~q = ~(p & q) if p=or, then
   * q=and, inv=not: ~p & ~q = ~(p | q) if p=add, then q=add, inv=neg: -p + -q = -(p + q) (add is
   * self-dual) if p=sub, then q=sub, inv=neg: -p - -q = -(p - q) (sub is self-dual) Assumes Word as
   * type of operands. (TODO: we only implement the first two rewrites above using the deMorgan
   * function; maybe we should also use deMorgan for the last two?)
   */
  private static Code deMorgan(Prim q, Prim inv, Tail a, Tail b) {
    Atom[] ap;
    Atom[] bp;
    if (a != null && (ap = a.isPrim(inv)) != null && b != null && (bp = b.isPrim(inv)) != null) {
      MILProgram.report("applied a version of deMorgan's law");
      Temp v = new Temp();
      return new Bind(v, q.withArgs(ap[0], bp[0]), done(inv, v));
    }
    return null;
  }

  private static Code addVarVar(Atom x, Atom y, Facts facts) {
    Tail a = x.lookupFact(facts);
    Tail b = y.lookupFact(facts);
    if (a != null || b != null) { // Only look for a rewrite if there are some facts
      Code nc;
      // TODO: commuteRearrange calls redistBin, which checks for combinations that cannot
      // occur here (single constant on one side of operation) ... look for ways to clean up!
      return ((null != (nc = commuteRearrange(Prim.add, x, a, y, b)))
              || (null != (nc = distAdd(x, a, y, b))))
          ? nc
          : null;
    }
    return distAddAnyAny(x, y);
  }

  private static Code addVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x + 0 == x
      MILProgram.report("rewrite: x + 0 ==> x");
      return done(x);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.add)) != null) {
        Word c = ap[1].isWord();
        if (c != null) {
          MILProgram.report("rewrite: (x + n) + m == x + (n + m)");
          return done(Prim.add, ap[0], c.getVal() + m);
        }
      } else if ((ap = a.isPrim(Prim.sub)) != null) {
        Word c;
        if ((c = ap[1].isWord()) != null) {
          MILProgram.report("rewrite: (x - n) + m == x + (m - n)");
          return done(Prim.add, ap[0], m - c.getVal());
        }
        if ((c = ap[0].isWord()) != null) {
          MILProgram.report("rewrite: (n - x) + m == (n + m) - x");
          return done(Prim.add, c.getVal() + m, ap[1]);
        }
      } else if ((ap = a.isPrim(Prim.neg)) != null) {
        MILProgram.report("rewrite: (-x) + m  ==> m - x");
        return done(Prim.sub, m, ap[0]);
      }
    }
    return null;
  }

  private static Code distAdd(Atom x, Tail a, Atom y, Tail b) {
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.neg)) != null) {
        return distAddNeg(ap[0], y, b);
      }
      Word m;
      if ((ap = a.isPrim(Prim.mul)) != null && (m = ap[1].isWord()) != null) {
        return distAddCMul(x, ap[0], m.getVal(), y, b);
      }
    }
    return distAddAny(x, y, b);
  }

  private static Code distSub(Atom x, Tail a, Atom y, Tail b) {
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.neg)) != null) {
        return distSubNeg(ap[0], y, b);
      }
      Word m;
      if ((ap = a.isPrim(Prim.mul)) != null && (m = ap[1].isWord()) != null) {
        return distSubCMul(x, ap[0], m.getVal(), y, b);
      }
    }
    return distSubAny(x, y, b);
  }

  private static Code distAddNeg(Atom u, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distAddNegNeg(u, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distAddNegCMul(u, y, bp[0], n.getVal());
      }
    }
    return distAddNegAny(u, y);
  }

  private static Code distAddAny(Atom x, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distAddAnyNeg(x, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distAddAnyCMul(x, bp[0], n.getVal());
      }
    }
    return distAddAnyAny(x, y);
  }

  private static Code distAddCMul(Atom x, Atom u, long c, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distAddCMulNeg(x, u, c, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distCC(u, Prim.mul, c, Prim.add, bp[0], n.getVal());
      }
    }
    return distAddCMulAny(u, c, y);
  }

  private static Code distSubNeg(Atom u, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distSubNegNeg(u, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distSubNegCMul(u, y, bp[0], n.getVal());
      }
    }
    return distSubNegAny(u, y);
  }

  private static Code distSubAny(Atom x, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distSubAnyNeg(x, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distSubAnyCMul(x, bp[0], n.getVal());
      }
    }
    return distSubAnyAny(x, y);
  }

  private static Code distSubCMul(Atom x, Atom u, long c, Atom y, Tail b) {
    if (b != null) {
      Atom[] bp;
      Word n;
      if ((bp = b.isPrim(Prim.neg)) != null) {
        return distSubCMulNeg(x, u, c, bp[0]);
      }
      if ((bp = b.isPrim(Prim.mul)) != null && (n = bp[1].isWord()) != null) {
        return distCC(u, Prim.mul, c, Prim.sub, bp[0], n.getVal());
      }
    }
    return distSubCMulAny(u, c, y);
  }

  private static Code distCC(
      Atom u, Prim m, long c, PrimBinOp a, Atom v, long d) { // (u `m` c) `a` (v `m` d) = ...
    if (u == v) {
      MILProgram.report("rewrite: (u `m` c) `a` (u `m` d) ==> u `m` (c `a` d)");
      return done(m, u, a.op(c, d));
    }
    if (c == d) {
      MILProgram.report("rewrite: (u `m` c) `a` (v `m` c) ==> (u `a` v) `m` c");
      Temp t = new Temp();
      return new Bind(t, a.withArgs(u, v), done(m, t, c));
    }
    return null;
  }

  private static Code distRearrange(PrimBinOp p, PrimBinOp q, Atom x, Tail a, Atom y, Tail b) {
    Atom[] ap, bp;
    Word c, d;
    if (a != null
        && b != null
        && // check for an expression of the form required by distCC
        (ap = a.isPrim(q)) != null
        && (bp = b.isPrim(q)) != null
        && (c = ap[1].isWord()) != null
        && (d = bp[1].isWord()) != null) {
      return distCC(ap[0], q, c.getVal(), p, bp[0], d.getVal());
    }
    return null;
  }

  private static Code distAddNegNeg(Atom u, Atom v) {
    MILProgram.report("rewrite: (-u) + (-v) ==> - (u + v)");
    Temp t = new Temp();
    return new Bind(t, Prim.add.withArgs(u, v), done(Prim.neg, t));
  }

  private static Code distSubNegNeg(Atom u, Atom v) {
    MILProgram.report("rewrite: (-u) - (-v) ==> v - u)");
    return done(Prim.sub, v, u);
  }

  private static Code distAddCMulNeg(Atom x, Atom u, long c, Atom v) { // x@(u * c) + (-v) = ...
    if (u == v) {
      MILProgram.report("rewrite: (u * c) + (-u) ==> u * (c - 1)");
      return done(Prim.mul, u, c - 1);
    }
    return distAddAnyNeg(x, v);
  }

  private static Code distAddNegCMul(Atom u, Atom y, Atom v, long d) { // (-u) + y@(v * d) = ...
    if (u == v) {
      MILProgram.report("rewrite: (-u) + (u * d)  ==>  u * (d - 1)");
      return done(Prim.mul, u, d - 1);
    }
    return distAddNegAny(u, y);
  }

  private static Code distSubCMulNeg(Atom x, Atom u, long c, Atom v) { // x@(u * c) - (-v) = ...
    if (u == v) {
      MILProgram.report("rewrite: (u * c) - (-u) ==> u * (c + 1)");
      return done(Prim.mul, u, c + 1);
    }
    return distAddAnyNeg(x, v);
  }

  private static Code distSubNegCMul(Atom u, Atom y, Atom v, long d) { // (-u) - y@(v * d) = ...
    if (u == v) {
      MILProgram.report("rewrite: (-u) - (u * d)  ==>  u * (-(1 + d))");
      return done(Prim.mul, u, -(1 + d));
    }
    return distAddNegAny(u, y);
  }

  private static Code distAddCMulAny(Atom u, long c, Atom y) { // (u * c) + y = ...
    if (u == y) {
      MILProgram.report("rewrite: (u * c) + u ==> u * (c + 1)");
      return done(Prim.mul, u, c + 1);
    }
    return null;
  }

  private static Code distAddAnyCMul(Atom x, Atom v, long d) { // x + (v * d) = ...
    if (x == v) {
      MILProgram.report("rewrite: v + (v * d)  ==>  v * (1 + d)");
      return done(Prim.mul, v, 1 + d);
    }
    return null;
  }

  private static Code distSubCMulAny(Atom u, long c, Atom y) { // (u * c) - y = ...
    if (u == y) {
      MILProgram.report("rewrite: (u * c) - u ==> u * (c - 1)");
      return done(Prim.mul, u, c - 1);
    }
    return null;
  }

  private static Code distSubAnyCMul(Atom x, Atom v, long d) { // x - (v * d) = ...
    if (x == v) {
      MILProgram.report("rewrite: v - (v * d)  ==>  v * (1 - d)");
      return done(Prim.mul, v, 1 - d);
    }
    return null;
  }

  private static Code distAddNegAny(Atom u, Atom y) {
    MILProgram.report("rewrite: (-u) + y ==> y - u");
    return done(Prim.sub, y, u);
  }

  private static Code distAddAnyNeg(Atom x, Atom v) {
    MILProgram.report("rewrite: x + (-v) ==> x - v");
    return done(Prim.sub, x, v);
  }

  private static Code distSubNegAny(Atom u, Atom y) {
    MILProgram.report("rewrite: (-u) - y ==> -(u + y)");
    Temp t = new Temp();
    return new Bind(t, Prim.add.withArgs(u, y), done(Prim.neg, t));
  }

  private static Code distSubAnyNeg(Atom x, Atom v) {
    MILProgram.report("rewrite: x - (-v) ==> x + v");
    return done(Prim.add, x, v);
  }

  private static Code distAddAnyAny(Atom x, Atom y) {
    if (x == y) {
      MILProgram.report("rewrite: x + x ==> x * 2");
      return done(Prim.mul, x, 2);
    }
    return null;
  }

  private static Code distSubAnyAny(Atom x, Atom y) {
    if (x == y) {
      MILProgram.report("rewrite: x - x ==> 0");
      return done(0);
    }
    return null;
  }

  private static Code mulVarVar(Atom x, Atom y, Facts facts) {
    return commuteRearrange(Prim.mul, x, x.lookupFact(facts), y, y.lookupFact(facts));
  }

  private static Code mulVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x * 0 == 0
      MILProgram.report("rewrite: x * 0 ==> 0");
      return done(0);
    }
    if (m == 1) { // x * 1 == x
      MILProgram.report("rewrite: x * 1 ==> x");
      return done(x);
    }
    if (m == (-1)) { // x * -1 == neg(x)
      MILProgram.report("rewrite: x * (-1) ==> -x");
      return done(Prim.neg, x);
    }
    if (m > 2 && (m & (m - 1)) == 0) { // x * (1 << n) == x << n
      int n = 0;
      long m0 = m;
      while ((m >>= 1) > 0) {
        n++;
      } // calculate n
      MILProgram.report("rewrite: x * " + m0 + " ==> x << " + n);
      return done(Prim.shl, x, n);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.mul)) != null) {
        Word b = ap[1].isWord();
        if (b != null) { // (u * c) * m == u * (c * m)
          return done(Prim.mul, ap[0], b.getVal() * m);
        }
      } else if ((ap = a.isPrim(Prim.add)) != null) {
        Word b = ap[1].isWord();
        if (b != null) { // (u + n) * m == (u * m) + (n * m)
          Temp v = new Temp();
          return new Bind(v, Prim.mul.withArgs(ap[0], m), done(Prim.add, v, b.getVal() * m));
        }
      }
    }
    return null;
  }

  private static Code orVarVar(Atom x, Atom y, Facts facts) {
    Code nc = idempotent(x, y);
    if (nc == null) {
      Tail a = x.lookupFact(facts);
      Tail b = y.lookupFact(facts);
      if ((a != null || b != null)
          && (nc = commuteRearrange(Prim.or, x, a, y, b)) == null
          && (nc = distRearrange(Prim.or, Prim.and, x, a, y, b)) == null) {
        nc = deMorgan(Prim.and, Prim.not, a, b);
      }
    }
    return nc;
  }

  private static Code orVarConst(Atom x, long m, Facts facts) {
    if (m == 0) {
      MILProgram.report("rewrite: x | 0 ==> x");
      return done(x);
    }
    if (m == (~0)) {
      MILProgram.report("rewrite: x | (~0) ==> (~0)");
      return done(~0);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.or)) != null) {
        Word c = ap[1].isWord();
        if (c != null) {
          MILProgram.report("rewrite: (u | c) | m ==> u | (c | n)");
          return done(Prim.or.withArgs(ap[0], c.getVal() | m));
        }
      } else if ((ap = a.isPrim(Prim.not)) != null) {
        MILProgram.report("rewrite: (~u) | m ==> ~(u & ~m)");
        Temp v = new Temp();
        return new Bind(v, Prim.and.withArgs(ap[0], ~m), done(Prim.not.withArgs(v)));
      } else if ((ap = a.isPrim(Prim.and)) != null) {
        Word c = ap[1].isWord(); // (_ & c) | m
        if (c != null) {
          Tail b = ap[0].lookupFact(facts); // ((b) & c) | m
          if (b != null) {
            Atom[] bp = b.isPrim(Prim.or); // ((_ | _) & c) | m
            if (bp != null) {
              Word d = bp[1].isWord(); // ((_ | d) & c) | m
              if (d != null) {
                MILProgram.report("rewrite: ((u | d) & c) | m ==> (u & c) | ((d & c) | m)");
                Temp v = new Temp();
                long n = (d.getVal() & c.getVal()) | m;
                return new Bind(v, Prim.and.withArgs(bp[0], c), done(Prim.or.withArgs(v, n)));
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static Code andVarVar(Atom x, Atom y, Facts facts) {
    Code nc = idempotent(x, y);
    if (nc == null) {
      Tail a = x.lookupFact(facts);
      Tail b = y.lookupFact(facts);
      if ((a != null || b != null)
          && (nc = commuteRearrange(Prim.and, x, a, y, b)) == null
          && (nc = distRearrange(Prim.and, Prim.or, x, a, y, b)) == null) {
        nc = deMorgan(Prim.or, Prim.not, a, b);
      }
    }
    return nc;
  }

  private static Code andVarConst(Atom x, long m, Facts facts) {
    if (m == 0) {
      MILProgram.report("rewrite: x & 0 ==> 0");
      return done(0);
    }
    if (m == (~0)) {
      MILProgram.report("rewrite: x & (~0) ==> x");
      return done(x);
    }
    Tail a = x.lookupFact(facts); // (a) & m
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.and)) != null) {
        Word c = ap[1].isWord();
        if (c != null) {
          MILProgram.report("rewrite: (u & c) & m ==> u & (c & n)");
          return done(Prim.and.withArgs(ap[0], c.getVal() & m));
        }
      } else if ((ap = a.isPrim(Prim.not)) != null) {
        MILProgram.report("rewrite: (~u) & m ==> ~(u | ~m)");
        Temp v = new Temp();
        return new Bind(v, Prim.or.withArgs(ap[0], ~m), done(Prim.not.withArgs(v)));
      } else if ((ap = a.isPrim(Prim.or)) != null) {
        Word c = ap[1].isWord(); // (_ | c) & m
        if (c != null) {
          MILProgram.report("rewrite: (a | c) & m ==> (a & m) | (c & m)");
          Temp v = new Temp();
          return new Bind(
              v, Prim.and.withArgs(ap[0], m), done(Prim.or.withArgs(v, c.getVal() & m)));
        }
      } else if ((ap = a.isPrim(Prim.shl)) != null) {
        // TODO: would it be better to rewrite (x << c) & m ==> (x & (m>>c)) << c?
        // (observation: rewriting would avoid repeated triggering the logic here)
        // Q1: is this valid (intuition: (x<<c)&m = (x<<c)&((m>>c)<<c) = (x&(m>>c))<<c)
        // Q2: does this interfere with rewrites for (x & m) << c?  May need to remove those ...
        Word c = ap[1].isWord(); // (_ << c) & m
        if (c != null) {
          long w = c.getVal();
          if (w > 0 && w < Word.size()) {
            // left shifting by w bits performs an effective mask by em on the result:
            long em = ~((1L << w) - 1);
            if ((m & em) == em) { // if specified mask doesn't do more than effective mask ...
              MILProgram.report(
                  "rewrite: (x << " + w + ") & 0x" + Long.toHexString(m) + " ==> (x << " + w + ")");
              return done(x);
            }
          }
        }
      } else if ((ap = a.isPrim(Prim.lshr)) != null) {
        Word c = ap[1].isWord(); // (_ >> c) & m
        if (c != null) {
          long w = c.getVal();
          if (w > 0 && w < Word.size()) {
            // right shifting by w bits performs an effective mask by em on the result:
            long em = (1L << (Word.size() - w)) - 1;
            if ((m & em) == em) { // if specified mask doesn't do more than effective mask ...
              MILProgram.report(
                  "rewrite: (x >> " + w + ") & 0x" + Long.toHexString(m) + " ==> (x >> " + w + ")");
              return done(x);
            }
          }
        }
      } else if ((ap = a.isPrim(Prim.add)) != null) {
        // TODO: generalize this to work with other primitives (e.g., sub, mul)
        // and to eliminate masks on either left or right hand sides
        Tail b = ap[0].lookupFact(facts); // ((b) + y) & m
        if (b != null) {
          Atom[] bp = b.isPrim(Prim.and);
          if (bp != null) { // ((u & v) + y) & m
            Word c = bp[1].isWord();
            if (c != null && modarith(c.getVal(), m)) { // ((u & m) + y) & m
              MILProgram.report(
                  "rewrite: ((x & 0x"
                      + Long.toHexString(c.getVal())
                      + ") + y) & 0x"
                      + Long.toHexString(m)
                      + " ==> (x + y) & 0x"
                      + Long.toHexString(m));
              Temp v = new Temp();
              return new Bind(v, Prim.add.withArgs(bp[0], ap[1]), done(Prim.and.withArgs(v, m)));
            }
          }
        }
      }
    }
    return null;
  }

  /** Return true if ((x & m1) + y) & m2 == (x + y) & m2. */
  private static boolean modarith(long m1, long m2) {
    // if m is a run of bits, then  m | ~(m-1)  has  the same run of bits
    // with all more significant bits set to 1
    return bitrun(m1) && bitrun(m2) && ((m1 & (m2 | ~(m2 - 1))) == m1);
  }

  /** Return true if value m is a single run of 1 bits (no zero bits between 1s). */
  private static boolean bitrun(long m) {
    // If m is a run of bits, then m | (m-1) will be a run of bits with the
    // same most significant bit and all lower bits set to 1.
    long v = (m | (m - 1));
    // In which case, that value plus one will be a power of two:
    return (v & (v + 1)) == 0;
  }

  private static Code xorVarVar(Atom x, Atom y, Facts facts) {
    if (x == y) { // simple annihilator
      // TODO: in an expression of the form (x ^ y), we could further
      // exploit annihilation if x includes an or with y (or vice versa).
      MILProgram.report("rewrite: x ^ x ==> 0");
      return done(0);
    }
    return commuteRearrange(Prim.xor, x, x.lookupFact(facts), y, y.lookupFact(facts));
  }

  private static Code xorVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x ^ 0 == x
      MILProgram.report("rewrite: x ^ 0 ==> x");
      return done(x);
    }
    if (m == (~0)) { // x ^ (~0) == not(x)
      MILProgram.report("rewrite: x ^ (~0) ==> not(x)");
      return done(Prim.not.withArgs(x));
    }
    return null;
  }

  private static Code subVarVar(Atom x, Atom y, Facts facts) {
    if (x == y) { // x - x == 0
      MILProgram.report("rewrite: x - x ==> 0");
      return done(0);
    }
    Tail a = x.lookupFact(facts);
    Tail b = y.lookupFact(facts);
    if (a != null || b != null) { // Only look for a rewrite if there are some facts
      Code nc;
      return ((null != (nc = redistBin(Prim.sub, Prim.add, x, a, y, b)))
              || (null != (nc = distSub(x, a, y, b))))
          ? nc
          : null;
    }
    return distSubAnyAny(x, y);
  }

  private static Code subVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x - 0 == x
      MILProgram.report("rewrite: x - 0 ==> x");
      return done(x);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.add)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x + n) - m == x + (n - m)");
          return done(Prim.add, ap[0], b.getVal() - m);
        }
      } else if ((ap = a.isPrim(Prim.sub)) != null) {
        Word c;
        if ((c = ap[1].isWord()) != null) {
          MILProgram.report("rewrite: (x - n) - m == x - (n + m)");
          return done(Prim.sub, ap[0], c.getVal() + m);
        }
        if ((c = ap[0].isWord()) != null) {
          MILProgram.report("rewrite: (n - x) - m == (n - m) - x");
          return done(Prim.sub, c.getVal() - m, ap[1]);
        }
      } else if ((ap = a.isPrim(Prim.neg)) != null) {
        MILProgram.report("rewrite: (-x) - m  == -(x + m)");
        Temp v = new Temp();
        return new Bind(v, Prim.add.withArgs(ap[0], m), done(Prim.neg, v));
      }
    }

    // TODO: not sure about this one; it turns simple decrements like sub(x,1) into
    // adds like add(x, -1); I guess this could be addressed by a code generator that
    // doesn't just naively turn every add(x,n) into an add instruction ...
    //
    // If n==0,  then add(x,n) shouldn't occur ...
    //    n==1,  then add(x,n) becomes an increment instruction
    //    n>1,   then add(x,n) becomes an add with immediate argument
    //    n==-1, then add(x,n) becomes a decrement instruction
    //    n< -1, then add(x,n) becomes a subtract with immediate argument
    //
    return done(Prim.add, x, (-m)); // x - n == x + (-n)
  }

  private static Code subConstVar(long n, Atom y, Facts facts) {
    if (n == 0) { // 0 - y == -y
      MILProgram.report("rewrite: 0 - y ==> -y");
      return done(Prim.neg, y);
    }
    Tail b = y.lookupFact(facts);
    if (b != null) {
      Atom[] bp;
      if ((bp = b.isPrim(Prim.add)) != null) {
        Word c = bp[1].isWord();
        if (c != null) {
          MILProgram.report("rewrite: n - (x + m) == (n - m) - x");
          return done(Prim.sub, n - c.getVal(), bp[0]);
        }
      } else if ((bp = b.isPrim(Prim.sub)) != null) {
        Word c;
        if ((c = bp[1].isWord()) != null) {
          MILProgram.report("rewrite: n - (x - m) == (n + m) - x");
          return done(Prim.sub, n + c.getVal(), bp[0]);
        }
        if ((c = bp[0].isWord()) != null) {
          MILProgram.report("rewrite: n - (m - x) == (n - m) + x");
          return done(Prim.add, n - c.getVal(), bp[0]);
        }
      } else if ((bp = b.isPrim(Prim.neg)) != null) {
        MILProgram.report("rewrite: n - (-x) == x + n");
        return done(Prim.add, bp[0], n);
      }
    }
    return null;
  }

  private static Code shlVarVar(Atom x, Atom y, Facts facts) {
    return null;
  }

  private static Code shlVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x << 0 == x
      MILProgram.report("rewrite: x << 0 ==> x");
      return done(x);
    }
    final int wordsize = Word.size();
    if (m < 0 || m >= wordsize) { // x << m == x << (m % Wordsize)
      long n = m % wordsize;
      // TODO: Is this architecture dependent?
      MILProgram.report("rewrite: x << " + m + " ==> x << " + n);
      return done(Prim.shl, x, n);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.shl)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          long n = b.getVal();
          if (n >= 0 && n < wordsize && m >= 0 && m < wordsize) {
            if (n + m >= wordsize) {
              MILProgram.report("rewrite: (x << " + n + ") << " + m + " ==> 0");
              return done(0);
            } else {
              MILProgram.report("rewrite: (x << " + n + ") << " + m + " ==> x << " + (n + m));
              return done(Prim.shl, ap[0], n + m);
            }
          }
        }
      } else if ((ap = a.isPrim(Prim.lshr)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          long n = b.getVal();
          if (n == m && n > 0 && n < wordsize) {
            long mask = (-1) << m;
            MILProgram.report(
                "rewrite: (x >>> " + m + ") << " + m + " ==>  x & 0x" + Long.toHexString(mask));
            return done(Prim.and, ap[0], mask);
          }
        }
      } else if ((ap = a.isPrim(Prim.and)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          // TODO: is this a good idea?  Unless n << m == 0, this makes the mask bigger ...
          MILProgram.report("rewrite: (x & n) << m  ==  (x<<m) & (n<<m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.shl.withArgs(ap[0], m), done(Prim.and, v, n << m));
        }
      } else if ((ap = a.isPrim(Prim.or)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          // TODO: is this a good idea?  Unless n << m == 0, this makes the constant bigger ...
          // (But it might reduce the need for a shift if the shift on x can be combined with
          // another shift (i.e., if x = (y << p), say) ... which can happen in practice ...
          MILProgram.report("rewrite: (x | n) << m  ==  (x<<m) | (n<<m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.shl.withArgs(ap[0], m), done(Prim.or, v, n << m));
        }
      } else if ((ap = a.isPrim(Prim.xor)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x ^ n) << m  ==  (x<<m) ^ (n<<m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.shl.withArgs(ap[0], m), done(Prim.xor, v, n << m));
        }
      } else if ((ap = a.isPrim(Prim.add)) != null) {
        // TODO: we are using the same basic pattern here for &, |, ^, and +
        // ... can we generalize and perhaps include other operators too?
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x + n) << m  ==  (x<<m) + (n<<m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.shl.withArgs(ap[0], m), done(Prim.add, v, n << m));
        }
      }
    }
    return null;
  }

  private static Code shlConstVar(long n, Atom y, Facts facts) {
    if (n == 0) { // 0 << y == 0
      MILProgram.report("rewrite: 0 << y ==> 0");
      return done(0);
    }
    return null;
  }

  private static Code lshrVarVar(Atom x, Atom y, Facts facts) {
    return null;
  }

  private static Code lshrVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x >>> 0 == x
      MILProgram.report("rewrite: lshr((x, 0)) ==> x");
      return done(x);
    }
    final int wordsize = Word.size();
    if (m < 0 || m >= wordsize) { // x >>> m == x >>> (m % Wordsize)
      long n = m % wordsize;
      // TODO: Is this architecture dependent?
      MILProgram.report("rewrite: x >>> " + m + " ==> x >>> " + n);
      return done(Prim.lshr, x, n);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.lshr)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          long n = b.getVal();
          if (n >= 0 && n < wordsize && m >= 0 && m < wordsize) {
            if (n + m >= wordsize) {
              MILProgram.report("rewrite: (x >>> " + n + ") >>> " + m + " ==> 0");
              return done(0);
            } else {
              MILProgram.report("rewrite: (x >>> " + n + ") >>> " + m + " ==> x >>> " + (n + m));
              return done(Prim.lshr, ap[0], n + m);
            }
          }
        }
      } else if ((ap = a.isPrim(Prim.shl)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          long n = b.getVal();
          if (n == m && n > 0 && n < wordsize) {
            long mask = (-1) >>> m;
            MILProgram.report(
                "rewrite: (x << " + m + ") >>> " + m + " ==>  x & 0x" + Long.toHexString(mask));
            return done(Prim.and, ap[0], mask);
          }
        }
      } else if ((ap = a.isPrim(Prim.and)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x & n) >>> m  ==  (x>>>m) & (n>>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.lshr.withArgs(ap[0], m), done(Prim.and, v, n >>> m));
        }
      } else if ((ap = a.isPrim(Prim.or)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x | n) >>> m  ==  (x>>>m) | (n>>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.lshr.withArgs(ap[0], m), done(Prim.or, v, n >>> m));
        }
      } else if ((ap = a.isPrim(Prim.xor)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x ^ n) >>> m  ==  (x>>>m) ^ (n>>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.lshr.withArgs(ap[0], m), done(Prim.xor, v, n >>> m));
        }
      }
    }
    return null;
  }

  private static Code lshrConstVar(long n, Atom y, Facts facts) {
    if (n == 0) { // 0 >>> y == 0
      MILProgram.report("rewrite: lshr((0, y)) ==> 0");
      return done(0);
    }
    return null;
  }

  private static Code ashrVarVar(Atom x, Atom y, Facts facts) {
    return null;
  }

  private static Code ashrVarConst(Atom x, long m, Facts facts) {
    if (m == 0) { // x >> 0 == x
      MILProgram.report("rewrite: ashr((x, 0)) ==> x");
      return done(x);
    }
    final int wordsize = Word.size();
    if (m < 0 || m >= wordsize) { // x >>> m == x >>> (m % Wordsize)
      long n = m % wordsize;
      // TODO: Is this architecture dependent?
      MILProgram.report("rewrite: x >> " + m + " ==> x >> " + n);
      return done(Prim.ashr, x, n);
    }
    Tail a = x.lookupFact(facts);
    if (a != null) {
      Atom[] ap;
      if ((ap = a.isPrim(Prim.ashr)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          long n = b.getVal();
          if (n >= 0 && n < wordsize && m >= 0 && m < wordsize) {
            if (n + m >= wordsize) {
              MILProgram.report(
                  "rewrite: (x >> " + n + ") >> " + m + " ==> x >> " + (wordsize - 1));
              return done(Prim.ashr, ap[0], wordsize - 1);
            } else {
              MILProgram.report("rewrite: (x >> " + n + ") >> " + m + " ==> x >> " + (n + m));
              return done(Prim.ashr, ap[0], n + m);
            }
          }
        }
      } else if ((ap = a.isPrim(Prim.and)) != null) {
        // It seems unlikely that arithmetic shifts will be used with bitwise operators,
        // but it shouldn't do any harm to include these optimization cases ...
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x & n) >> m  ==  (x>>m) & (n>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.ashr.withArgs(ap[0], m), done(Prim.and, v, n >> m));
        }
      } else if ((ap = a.isPrim(Prim.or)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x | n) >> m  ==  (x>>m) | (n>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.ashr.withArgs(ap[0], m), done(Prim.or, v, n >> m));
        }
      } else if ((ap = a.isPrim(Prim.xor)) != null) {
        Word b = ap[1].isWord();
        if (b != null) {
          MILProgram.report("rewrite: (x ^ n) >> m  ==  (x>>m) ^ (n>>m)");
          long n = b.getVal();
          Temp v = new Temp();
          return new Bind(v, Prim.ashr.withArgs(ap[0], m), done(Prim.xor, v, n >> m));
        }
      }
    }
    return null;
  }

  private static Code ashrConstVar(long n, Atom y, Facts facts) {
    if (n == 0) { // 0 >> y == 0
      MILProgram.report("rewrite: ashr((0, y)) ==> 0");
      return done(0);
    } else if (n == (~0)) { // ~0 >> y = ~0
      MILProgram.report("rewrite: ashr((~0, y)) ==> ~0");
      return done(n);
    }
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return summary(p.summary()) * 33 + 1;
  }

  /** Test to see if two Tail expressions are alpha equivalent. */
  boolean alphaTail(Temps thisvars, Tail that, Temps thatvars) {
    return that.alphaPrimCall(thatvars, this, thisvars);
  }

  /** Test two items for alpha equivalence. */
  boolean alphaPrimCall(Temps thisvars, PrimCall that, Temps thatvars) {
    return this.p == that.p && this.alphaArgs(thisvars, that, thatvars);
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (type != null) {
      type = type.canonBlockType(set);
    }
    p = p.canonPrim(set);
    Atom.collect(args, set);
  }

  /** Generate a specialized version of this Call. */
  Call specializeCall(MILSpec spec, TVarSubst s, SpecEnv env) {
    return new PrimCall(p.specializePrim(spec, type, s));
  }

  Tail repTransform(RepTypeSet set, RepEnv env) {
    return p.repTransformPrim(set, Atom.repArgs(set, env, args));
  }

  /**
   * Generate LLVM code to execute this Tail with NO result from the right hand side of a Bind. Set
   * isTail to true if the code sequence c is an immediate ret void instruction.
   */
  llvm.Code toLLVMBindVoid(LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Code c) {
    return p.toLLVMPrimVoid(lm, vm, s, args, isTail, c);
  }

  /**
   * Generate LLVM code to execute this Tail and return a result from the right hand side of a Bind.
   * Set isTail to true if the code sequence c will immediately return the value in the specified
   * lhs.
   */
  llvm.Code toLLVMBindCont(
      LLVMMap lm, VarMap vm, TempSubst s, boolean isTail, llvm.Local lhs, llvm.Code c) {
    return p.toLLVMPrimCont(lm, vm, s, args, isTail, lhs, c);
  }
}
