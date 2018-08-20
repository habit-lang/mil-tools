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

public class Prim {

  /** The name that will be used for this primitive. */
  protected String id;

  /**
   * Purity code for this primitive. This can be used to describe the extent to which a given
   * primitive call may depend on or cause side effects.
   */
  protected int purity;

  /** BlockType for this primitive. */
  protected BlockType blockType;

  /** Default constructor. */
  public Prim(String id, int purity, BlockType blockType) {
    this.id = id;
    this.purity = purity;
    this.blockType = blockType;
  }

  /** Return the name of this primitive. */
  public String getId() {
    return id;
  }

  /** Return the block type for this primitive. */
  public BlockType getBlockType() {
    return blockType;
  }

  public static final int PURE = 0;

  public static final int OBSERVER = 1;

  public static final int VOLATILE = 2;

  public static final int IMPURE = 3;

  public static final int DOESNTRETURN = 4;

  public boolean isPure() {
    return purity == PURE;
  }

  public boolean isRepeatable() {
    return purity <= OBSERVER;
  }

  public boolean hasNoEffect() {
    return purity <= VOLATILE;
  }

  public boolean doesntReturn() {
    return purity >= DOESNTRETURN;
  }

  public static final String[] purityLabels =
      new String[] {"pure", "observer", "volatile", "impure", "doesntReturn"};

  public String purityLabel() {
    return (purity < 0 || purity >= purityLabels.length) ? null : purityLabels[purity];
  }

  public static int purityFromLabel(String p) {
    for (int i = 0; i < purityLabels.length; i++) {
      if (p.equals(purityLabels[i])) {
        return i;
      }
    }
    return (-1);
  }

  public Call withArgs(Atom[] args) {
    return new PrimCall(this).withArgs(args);
  }

  public Call withArgs() {
    return withArgs(Atom.noAtoms);
  }

  public Call withArgs(Atom a) {
    return withArgs(new Atom[] {a});
  }

  public Call withArgs(Atom a, Atom b) {
    return withArgs(new Atom[] {a, b});
  }

  public Call withArgs(Atom a, int n) {
    return withArgs(new Atom[] {a, new IntConst(n)});
  }

  public Call withArgs(int n, Atom b) {
    return withArgs(new Atom[] {new IntConst(n), b});
  }

  protected static final Type flagTuple = Type.tuple(DataName.flag.asType());

  protected static final Type flagFlagTuple =
      Type.tuple(DataName.flag.asType(), DataName.flag.asType());

  protected static final BlockType unaryFlagType = new BlockType(flagTuple, flagTuple);

  protected static final BlockType binaryFlagType = new BlockType(flagFlagTuple, flagTuple);

  protected static final Type wordTuple = Type.tuple(DataName.word.asType());

  protected static final Type wordWordTuple =
      Type.tuple(DataName.word.asType(), DataName.word.asType());

  protected static final BlockType unaryWordType = new BlockType(wordTuple, wordTuple);

  protected static final BlockType binaryWordType = new BlockType(wordWordTuple, wordTuple);

  protected static final BlockType nzdivType =
      new BlockType(Type.tuple(DataName.word.asType(), DataName.nzword.asType()), wordTuple);

  protected static final BlockType flagToWordType = new BlockType(flagTuple, wordTuple);

  protected static final BlockType relopType = new BlockType(wordWordTuple, flagTuple);

  /** Make a clone of this Prim with a (possibly) new type. */
  public Prim clone(BlockType bt) {
    return new Prim(id, purity, bt);
  }

  public static final PrimUnOp not = new not();

  private static class not extends PrimUnOp {

    private not() {
      this(unaryWordType);
    }

    private not(BlockType bt) {
      super("not", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new not(bt);
    }

    public int op(int n) {
      return (~n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i32, args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.Xor(ty, llvm.Int.ONES, v);
    }
  }

  public static final PrimBinOp and = new and();

  private static class and extends PrimBinOp {

    private and() {
      this(binaryWordType);
    }

    private and(BlockType bt) {
      super("and", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new and(bt);
    }

    public int op(int n, int m) {
      return n & m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.And(ty, l, r);
    }
  }

  public static final PrimBinOp or = new or();

  private static class or extends PrimBinOp {

    private or() {
      this(binaryWordType);
    }

    private or(BlockType bt) {
      super("or", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new or(bt);
    }

    public int op(int n, int m) {
      return n | m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Or(ty, l, r);
    }
  }

  public static final PrimBinOp xor = new xor();

  private static class xor extends PrimBinOp {

    private xor() {
      this(binaryWordType);
    }

    private xor(BlockType bt) {
      super("xor", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new xor(bt);
    }

    public int op(int n, int m) {
      return n ^ m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Xor(ty, l, r);
    }
  }

  public static final PrimUnFOp bnot = new bnot();

  private static class bnot extends PrimUnFOp {

    private bnot() {
      this(unaryFlagType);
    }

    private bnot(BlockType bt) {
      super("bnot", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new bnot(bt);
    }

    public boolean op(boolean b) {
      return !b;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i1, args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.Xor(ty, new llvm.Int(1), v);
    }
  }

  public static final PrimBinFOp band = new band();

  private static class band extends PrimBinFOp {

    private band() {
      this(binaryFlagType);
    }

    private band(BlockType bt) {
      super("band", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new band(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n & m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.And(ty, l, r);
    }
  }

  public static final PrimBinFOp bor = new bor();

  private static class bor extends PrimBinFOp {

    private bor() {
      this(binaryFlagType);
    }

    private bor(BlockType bt) {
      super("bor", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new bor(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n | m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Or(ty, l, r);
    }
  }

  public static final PrimBinFOp bxor = new bxor();

  private static class bxor extends PrimBinFOp {

    private bxor() {
      this(binaryFlagType);
    }

    private bxor(BlockType bt) {
      super("bxor", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new bxor(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n ^ m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.beq;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Xor(ty, l, r);
    }
  }

  public static final PrimBinFOp beq = new beq();

  private static class beq extends PrimBinFOp {

    private beq() {
      this(binaryFlagType);
    }

    private beq(BlockType bt) {
      super("beq", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new beq(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n == m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.bxor;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "eq");
    }
  }

  public static final PrimBinFOp blt = new blt();

  private static class blt extends PrimBinFOp {

    private blt() {
      this(binaryFlagType);
    }

    private blt(BlockType bt) {
      super("blt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new blt(bt);
    }

    public boolean op(boolean n, boolean m) {
      return !n & m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.bge;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ult");
    }
  }

  public static final PrimBinFOp ble = new ble();

  private static class ble extends PrimBinFOp {

    private ble() {
      this(binaryFlagType);
    }

    private ble(BlockType bt) {
      super("ble", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new ble(bt);
    }

    public boolean op(boolean n, boolean m) {
      return !n | m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.bgt;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ule");
    }
  }

  public static final PrimBinFOp bgt = new bgt();

  private static class bgt extends PrimBinFOp {

    private bgt() {
      this(binaryFlagType);
    }

    private bgt(BlockType bt) {
      super("bgt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new bgt(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n & !m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.ble;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ugt");
    }
  }

  public static final PrimBinFOp bge = new bge();

  private static class bge extends PrimBinFOp {

    private bge() {
      this(binaryFlagType);
    }

    private bge(BlockType bt) {
      super("bge", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new bge(bt);
    }

    public boolean op(boolean n, boolean m) {
      return n | !m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.blt;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "uge");
    }
  }

  public static final PrimBinOp shl = new shl();

  private static class shl extends PrimBinOp {

    private shl() {
      this(binaryWordType);
    }

    private shl(BlockType bt) {
      super("shl", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new shl(bt);
    }

    public int op(int n, int m) {
      return n << m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Shl(ty, l, r);
    }
  }

  public static final PrimBinOp lshr = new lshr();

  private static class lshr extends PrimBinOp {

    private lshr() {
      this(binaryWordType);
    }

    private lshr(BlockType bt) {
      super("lshr", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new lshr(bt);
    }

    public int op(int n, int m) {
      return n >>> m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.LShr(ty, l, r);
    }
  }

  public static final PrimBinOp ashr = new ashr();

  private static class ashr extends PrimBinOp {

    private ashr() {
      this(binaryWordType);
    }

    private ashr(BlockType bt) {
      super("ashr", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new ashr(bt);
    }

    public int op(int n, int m) {
      return n >> m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.AShr(ty, l, r);
    }
  }

  public static final PrimUnOp neg = new neg();

  private static class neg extends PrimUnOp {

    private neg() {
      this(unaryWordType);
    }

    private neg(BlockType bt) {
      super("neg", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new neg(bt);
    }

    public int op(int n) {
      return (-n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i32, args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.Sub(ty, new llvm.Int(0), v);
    }
  }

  public static final PrimBinOp add = new add();

  private static class add extends PrimBinOp {

    private add() {
      this(binaryWordType);
    }

    private add(BlockType bt) {
      super("add", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new add(bt);
    }

    public int op(int n, int m) {
      return n + m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Add(ty, l, r);
    }
  }

  public static final PrimBinOp sub = new sub();

  private static class sub extends PrimBinOp {

    private sub() {
      this(binaryWordType);
    }

    private sub(BlockType bt) {
      super("sub", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new sub(bt);
    }

    public int op(int n, int m) {
      return n - m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Sub(ty, l, r);
    }
  }

  public static final PrimBinOp mul = new mul();

  private static class mul extends PrimBinOp {

    private mul() {
      this(binaryWordType);
    }

    private mul(BlockType bt) {
      super("mul", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new mul(bt);
    }

    public int op(int n, int m) {
      return n * m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.Mul(ty, l, r);
    }
  }

  public static final Prim div = new div();

  private static class div extends Prim {

    private div() {
      this(binaryWordType);
    }

    private div(BlockType bt) {
      super("div", IMPURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new div(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      int n = stack[fp].getInt();
      int d = stack[fp + 1].getInt();
      if (d == 0) {
        throw new Failure("divide by zero error");
      }
      stack[fp] = new IntValue(n / d);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.i32, args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.SDiv(ty, l, r);
    }
  }

  public static final Prim rem = new rem();

  private static class rem extends Prim {

    private rem() {
      this(binaryWordType);
    }

    private rem(BlockType bt) {
      super("rem", IMPURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new rem(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      int n = stack[fp].getInt();
      int d = stack[fp + 1].getInt();
      if (d == 0) {
        throw new Failure("divide by zero error (for mod)");
      }
      stack[fp] = new IntValue(n % d);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.i32, args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.SRem(ty, l, r);
    }
  }

  public static final Prim nzdiv = new nzdiv();

  private static class nzdiv extends Prim {

    private nzdiv() {
      this(nzdivType);
    }

    private nzdiv(BlockType bt) {
      super("nzdiv", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new nzdiv(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.i32, args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.UDiv(ty, l, r);
    }
  }

  public static final PrimRelOp eq = new eq();

  private static class eq extends PrimRelOp {

    private eq() {
      this(relopType);
    }

    private eq(BlockType bt) {
      super("primEq", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new eq(bt);
    }

    public boolean op(int n, int m) {
      return n == m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.neq;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "eq");
    }
  }

  public static final PrimRelOp neq = new neq();

  private static class neq extends PrimRelOp {

    private neq() {
      this(relopType);
    }

    private neq(BlockType bt) {
      super("primNeq", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new neq(bt);
    }

    public boolean op(int n, int m) {
      return n != m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.eq;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ne");
    }
  }

  public static final PrimRelOp slt = new slt();

  private static class slt extends PrimRelOp {

    private slt() {
      this(relopType);
    }

    private slt(BlockType bt) {
      super("primSlt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new slt(bt);
    }

    public boolean op(int n, int m) {
      return n < m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.sge;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "slt");
    }
  }

  public static final PrimRelOp sle = new sle();

  private static class sle extends PrimRelOp {

    private sle() {
      this(relopType);
    }

    private sle(BlockType bt) {
      super("primSle", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new sle(bt);
    }

    public boolean op(int n, int m) {
      return n <= m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.sgt;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "sle");
    }
  }

  public static final PrimRelOp sgt = new sgt();

  private static class sgt extends PrimRelOp {

    private sgt() {
      this(relopType);
    }

    private sgt(BlockType bt) {
      super("primSgt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new sgt(bt);
    }

    public boolean op(int n, int m) {
      return n > m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.sle;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "sgt");
    }
  }

  public static final PrimRelOp sge = new sge();

  private static class sge extends PrimRelOp {

    private sge() {
      this(relopType);
    }

    private sge(BlockType bt) {
      super("primSge", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new sge(bt);
    }

    public boolean op(int n, int m) {
      return n >= m;
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.slt;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "sge");
    }
  }

  public static final PrimRelOp ult = new ult();

  private static class ult extends PrimRelOp {

    private ult() {
      this(relopType);
    }

    private ult(BlockType bt) {
      super("primUlt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new ult(bt);
    }

    public boolean op(int n, int m) {
      return (n < m) ^ (n < 0) ^ (m < 0);
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.uge;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ult");
    }
  }

  public static final PrimRelOp ule = new ule();

  private static class ule extends PrimRelOp {

    private ule() {
      this(relopType);
    }

    private ule(BlockType bt) {
      super("primUle", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new ule(bt);
    }

    public boolean op(int n, int m) {
      return (n <= m) ^ (n < 0) ^ (m < 0);
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.ugt;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ule");
    }
  }

  public static final PrimRelOp ugt = new ugt();

  private static class ugt extends PrimRelOp {

    private ugt() {
      this(relopType);
    }

    private ugt(BlockType bt) {
      super("primUgt", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new ugt(bt);
    }

    public boolean op(int n, int m) {
      return (n > m) ^ (n < 0) ^ (m < 0);
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.ule;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "ugt");
    }
  }

  public static final PrimRelOp uge = new uge();

  private static class uge extends PrimRelOp {

    private uge() {
      this(relopType);
    }

    private uge(BlockType bt) {
      super("primUge", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new uge(bt);
    }

    public boolean op(int n, int m) {
      return (n >= m) ^ (n < 0) ^ (m < 0);
    }

    /**
     * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
     * bnot(this(args)) == p(args), or null if there is no such primitive.
     */
    Prim bnotDual() {
      return Prim.ult;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.ICmp(ty, l, r, "uge");
    }
  }

  public static final PrimFtoW flagToWord = new flagToWord();

  private static class flagToWord extends PrimFtoW {

    private flagToWord() {
      this(flagToWordType);
    }

    private flagToWord(BlockType bt) {
      super("flagToWord", PURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new flagToWord(bt);
    }

    public int op(boolean b) {
      return b ? 1 : 0;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, new llvm.Zext(args[0].toLLVMAtom(lm, vm, s), llvm.Type.i32), c);
    }
  }

  /**
   * Represents the polymorphic block type forall (r::tuple). [] >>= r. TODO: should this be [] >>=
   * Void ?
   */
  public static final BlockType haltType =
      new PolyBlockType(Type.empty, Type.gen(0), new Prefix(new Tyvar[] {Tyvar.tuple}));

  public static final Prim halt = new halt();

  private static class halt extends Prim {

    private halt() {
      this(haltType);
    }

    private halt(BlockType bt) {
      super("halt", DOESNTRETURN, bt);
    }

    public Prim clone(BlockType bt) {
      return new halt(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      throw new Failure("halt primitive executed");
    }
  }

  BlockType instantiate() {
    return blockType.instantiate();
  }

  private static int count;

  private static Prim[] table;

  private int index;

  int getIndex() {
    return index;
  }

  int addToPrimTable(Prim val) {
    if (table == null) {
      table = new Prim[40];
    } else if (count >= table.length) {
      Prim[] newarray = new Prim[2 * table.length];
      for (int i = 0; i < table.length; i++) {
        newarray[i] = table[i];
      }
      table = newarray;
    }
    table[count] = val;
    return count++;
  }

  Prim(String id, int arity, int outity, int purity, BlockType blockType) {
    index = addToPrimTable(this); // TODO: could this be done with a static initializer?
  }

  static void exec(PrintWriter out, int prim, int fp, Value[] stack) throws Failure {
    if (prim < 0 || prim >= count) {
      throw new Failure("primitive number " + prim + " is not defined");
    }
    table[prim].exec(out, fp, stack);
  }

  static String showPrim(int i) {
    return (i >= 0 && i < count && table[i].id != null) ? table[i].id : ("?prim_" + i);
  }

  public static void printTable() {
    for (int i = 0; i < count; i++) {
      System.out.println(i + ") " + table[i].getId() + " :: " + table[i].getBlockType());
    }
    System.out.println("[total: " + count + " primitives]");
  }

  protected static final BlockType wordToUnitType = new BlockType(wordTuple, Type.empty);

  public static final Prim printWord = new printWord();

  private static class printWord extends Prim {

    private printWord() {
      this(wordToUnitType);
    }

    private printWord(BlockType bt) {
      super("printWord", IMPURE, bt);
    }

    public Prim clone(BlockType bt) {
      return new printWord(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      out.println("printWord: " + stack[fp].getInt());
    }
  }

  void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
    throw new Failure("primitive \"" + id + "\" not available");
  }

  public static final Prim loop = new loop();

  private static class loop extends Prim {

    private loop() {
      this(haltType);
    }

    private loop(BlockType bt) {
      super("loop", DOESNTRETURN, bt);
    }

    public Prim clone(BlockType bt) {
      return new loop(bt);
    }
  }

  /**
   * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
   * bnot(this(args)) == p(args), or null if there is no such primitive.
   */
  Prim bnotDual() {
    return null;
  }

  /**
   * Compute an integer summary for a fragment of MIL code with the key property that alpha
   * equivalent program fragments have the same summary value.
   */
  int summary() {
    return id.hashCode();
  }

  Prim canonPrim(TypeSet set) {
    Prim newP = set.getPrim(this);
    if (newP == null) {
      BlockType bt = blockType.canonBlockType(set);
      if (bt.alphaEquiv(blockType)) {
        newP = this;
      } else {
        newP = this.clone(bt);
        debug.Log.println("new version of primitive " + id + " :: " + bt);
        debug.Log.println("         old version was " + id + " :: " + blockType);
      }
      set.putPrim(this, newP);
    }
    return newP;
  }

  Prim specializePrim(MILSpec spec, BlockType type, TVarSubst s) {
    BlockType inst = type.apply(s).canonBlockType(spec);
    if (inst.alphaEquiv(this.blockType)) {
      return this;
    } else {
      Prims ps = spec.getPrims(this);
      for (; ps != null; ps = ps.next) {
        if (inst.alphaEquiv(ps.head.getBlockType())) {
          return ps.head;
        }
      }
      // TODO: should the new primitive include a pointer back to the Prim from which it was
      // derived?
      Prim newP = this.clone(inst);
      debug.Log.println("specialized version of primitive " + id + " :: " + inst);
      debug.Log.println("            original version was " + id + " :: " + blockType);
      spec.putPrims(this, new Prims(newP, ps));
      return newP;
    }
  }

  Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
    return canonPrim(set).withArgs(targs);
  }

  Tail maker(Position pos, boolean thunk) {
    Call call = new PrimCall(this);
    int arity = blockType.getArity();
    if (thunk) {
      if (blockType.getOutity() == 0) {
        call = call.returnUnit(pos, arity);
      }
      call = call.thunk(pos, arity);
    }
    return call.maker(pos, arity);
  }

  /**
   * Calculate an LLVM Global object corresponding to a primitive that is implemented using an
   * external function. The results are cached in a hash table, and a declaration for the primitive
   * is added to the program associated with this LLVMMap when the first occurrence is found.
   */
  llvm.Global primGlobalCalc(LLVMMap lm) {
    llvm.FunctionType ft = blockType.toLLVM(lm);
    lm.declare(id, ft);
    return new llvm.Global(ft, id);
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is not expected to produce any results, but execution is expected to continue with the given
   * code.
   */
  llvm.Code toLLVMPrimVoid(LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.CallVoid(lm.globalFor(this), Atom.toLLVMValues(lm, vm, s, args), c);
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is expected to return a result (that should be captured in the specified lhs), and then
   * execution is expected to continue on to the specified code, c.
   */
  llvm.Code toLLVMPrimCont(
      LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.Op(
        lhs,
        new llvm.Call(lhs.getType(), lm.globalFor(this), Atom.toLLVMValues(lm, vm, s, args)),
        c);
  }
}
