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

  /** The arity/number of arguments for this primitive. */
  protected int arity;

  /** The number of results for this primitive. */
  protected int outity;

  /**
   * Purity code for this primitive. This can be used to describe the extent to which a given
   * primitive call may depend on or cause side effects.
   */
  protected int purity;

  /** BlockType for this primitive. */
  protected BlockType blockType;

  /** Default constructor. */
  public Prim(String id, int arity, int outity, int purity, BlockType blockType) {
    this.id = id;
    this.arity = arity;
    this.outity = outity;
    this.purity = purity;
    this.blockType = blockType;

    index = addToPrimTable(this); // TODO: could this be done with a static initializer?
  }

  /** Return the name of this primitive. */
  public String getId() {
    return id;
  }

  /** Return the arity for this primitive. */
  public int getArity() {
    return arity;
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

  protected static final BlockType flagToWordType = new BlockType(flagTuple, wordTuple);

  protected static final BlockType relopType = new BlockType(wordWordTuple, flagTuple);

  public static final PrimUnOp not = new not();

  private static class not extends PrimUnOp {

    private not() {
      super("not", 1, 1, PURE, unaryWordType);
    }

    public int op(int n) {
      return (~n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i32, args[0].toLLVM(tm, vm, s)), c);
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
      super("and", 2, 1, PURE, binaryWordType);
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
      super("or", 2, 1, PURE, binaryWordType);
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
      super("xor", 2, 1, PURE, binaryWordType);
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
      super("bnot", 1, 1, PURE, unaryFlagType);
    }

    public boolean op(boolean b) {
      return !b;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i1, args[0].toLLVM(tm, vm, s)), c);
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
      super("band", 2, 1, PURE, binaryFlagType);
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
      super("bor", 2, 1, PURE, binaryFlagType);
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
      super("bxor", 2, 1, PURE, binaryFlagType);
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
      super("beq", 2, 1, PURE, binaryFlagType);
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
      super("blt", 2, 1, PURE, binaryFlagType);
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
      super("ble", 2, 1, PURE, binaryFlagType);
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
      super("bgt", 2, 1, PURE, binaryFlagType);
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
      super("bge", 2, 1, PURE, binaryFlagType);
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
      super("shl", 2, 1, PURE, binaryWordType);
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
      super("lshr", 2, 1, PURE, binaryWordType);
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
      super("ashr", 2, 1, PURE, binaryWordType);
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
      super("neg", 1, 1, PURE, unaryWordType);
    }

    public int op(int n) {
      return (-n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i32, args[0].toLLVM(tm, vm, s)), c);
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
      super("add", 2, 1, PURE, binaryWordType);
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
      super("sub", 2, 1, PURE, binaryWordType);
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
      super("mul", 2, 1, PURE, binaryWordType);
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
      super("div", 2, 1, IMPURE, binaryWordType);
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
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(
          lhs, this.op(llvm.Type.i32, args[0].toLLVM(tm, vm, s), args[1].toLLVM(tm, vm, s)), c);
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
      super("rem", 2, 1, IMPURE, binaryWordType);
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
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(
          lhs, this.op(llvm.Type.i32, args[0].toLLVM(tm, vm, s), args[1].toLLVM(tm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.SRem(ty, l, r);
    }
  }

  public static final PrimRelOp eq = new eq();

  private static class eq extends PrimRelOp {

    private eq() {
      super("primEq", 2, 1, PURE, relopType);
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
      super("primNeq", 2, 1, PURE, relopType);
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
      super("primSlt", 2, 1, PURE, relopType);
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
      super("primSle", 2, 1, PURE, relopType);
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
      super("primSgt", 2, 1, PURE, relopType);
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
      super("primSge", 2, 1, PURE, relopType);
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
      super("primUlt", 2, 1, PURE, relopType);
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
      super("primUle", 2, 1, PURE, relopType);
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
      super("primUgt", 2, 1, PURE, relopType);
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
      super("primUge", 2, 1, PURE, relopType);
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
      super("flagToWord", 1, 1, PURE, flagToWordType);
    }

    public int op(boolean b) {
      return b ? 1 : 0;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
      return new llvm.Op(lhs, new llvm.Zext(args[0].toLLVM(tm, vm, s), llvm.Type.i32), c);
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
      super("halt", 0, 0, DOESNTRETURN, haltType);
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
      System.out.println(i + ") " + table[i].getId() + "/" + table[i].getArity());
    }
    System.out.println("[total: " + count + " primitives]");
  }

  protected static final BlockType wordToUnitType = new BlockType(wordTuple, Type.empty);

  public static final Prim printWord = new printWord();

  private static class printWord extends Prim {

    private printWord() {
      super("printWord", 1, 0, IMPURE, wordToUnitType);
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
      super("loop", 0, 0, DOESNTRETURN, haltType);
    }
  }

  /**
   * Find the dual for this primitive under bnot. In other words, return a primitive p such that:
   * bnot(this(args)) == p(args), or null if there is no such primitive.
   */
  Prim bnotDual() {
    return null;
  }

  private static final BlockType loadType =
      new BlockType(
          Type.tuple(
              new Type[] {
                DataName.word.asType(), // size
                DataName.word.asType(), // base
                DataName.word.asType(), // addr
                DataName.word.asType(), // index
                DataName.word.asType() // multiplier
              }),
          Type.tuple(DataName.word.asType()));

  private static final BlockType storeType =
      new BlockType(
          Type.tuple(
              new Type[] {
                DataName.word.asType(), // size
                DataName.word.asType(), // base
                DataName.word.asType(), // addr
                DataName.word.asType(), // index
                DataName.word.asType(), // multiplier
                DataName.word.asType() // value
              }),
          Type.empty);

  public static final Prim load = new load();

  private static class load extends Prim {

    private load() {
      super("load", 5, 1, OBSERVER, loadType);
    }
  }

  public static final Prim store = new store();

  private static class store extends Prim {

    private store() {
      super("store", 6, 0, IMPURE, storeType);
    }
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
      // TODO: should we include a pointer back to the Prim from which newP is derived?
      newP = bt.alphaEquiv(blockType) ? this : new Prim(id, arity, outity, purity, bt);
      if (newP != this) {
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
      Prim newP = new Prim(id, arity, outity, purity, inst);
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
    if (thunk) {
      if (outity == 0) {
        call = call.returnUnit(pos, arity);
      }
      call = call.thunk(pos, arity);
    }
    return call.maker(pos, arity);
  }

  /**
   * Calculate an LLVM Global object corresponding to a primitive that is implemented using an
   * external function. The results are cached in a hash table, and a declaration for the primitive
   * is added to the program associated with this TypeMap when the first occurrence is found.
   */
  llvm.Global primGlobalCalc(TypeMap tm) {
    llvm.FunctionType ft = blockType.toLLVM(tm);
    tm.declare(id, ft);
    return new llvm.Global(ft, id);
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is not expected to produce any results, but execution is expected to continue with the given
   * code.
   */
  llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.CallVoid(tm.globalFor(this), Atom.toLLVM(tm, vm, s, args), c);
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is expected to return a result (that should be captured in the specified lhs), and then
   * execution is expected to continue on to the specified code, c.
   */
  llvm.Code toLLVM(TypeMap tm, VarMap vm, TempSubst s, Atom[] args, llvm.Local lhs, llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.Op(
        lhs, new llvm.Call(lhs.getType(), tm.globalFor(this), Atom.toLLVM(tm, vm, s, args)), c);
  }
}
