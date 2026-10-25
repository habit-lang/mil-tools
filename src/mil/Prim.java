/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import java.io.PrintWriter;

/** Represents a primitive for sequencing initializers: initSeq :: [Init a, Init a] >>= [Init a] */
public class Prim {

  /** The name that will be used for this primitive. */
  protected String id;

  /**
   * BlockType for this primitive, describing the number and types of this primitive's arguments and
   * results.
   */
  protected BlockType blockType;

  /** Purity code for this primitive. */
  protected int purity;

  /** Default constructor. */
  public Prim(String id, BlockType blockType, int purity) {
    this.id = id;
    this.blockType = blockType;
    this.purity = purity;

    this.index = addToPrimTable(this);
  }

  /** Return the name of this primitive. */
  String getId() {
    return id;
  }

  /** Return the block type for this primitive. */
  BlockType getBlockType() {
    return blockType;
  }

  /**
   * PURE indicates that, for any fixed argument x, the call p((x)) will always return the same
   * result. This applies to primitives for basic arithmetic like addition, logical operators,
   * comparison, etc. (For a primitive to be considered pure in this sense, there must be no
   * possibility of overflow or other exceptions, of setting flags that are visible to later
   * instructions, of non-termination, etc., at least to the extent that any such features of the
   * underlying hardware may be visible in MIL.)
   */
  static final int PURE = 0;

  /**
   * OBSERVER indicates that distinct calls to p((x)) with the same arguments can only be guaranteed
   * to return the same result if there is no side effecting operation between them. (In other
   * words, the primitive is able to "observe" changes in state.) Examples include operations that
   * read from local memory or special registers. However, observer status may not be appropriate in
   * the presence of concurrency or other operations where an implicit side effect might be
   * triggered, such as a page fault, or setting accessed or dirty bits in a page table data
   * structure, etc.
   */
  static final int OBSERVER = 1;

  /**
   * VOLATILE indicates that distinct calls to p((x)), even with the same arguments, may return
   * different results. Examples include reading a real-time clock or counter; loading from volatile
   * memory or device registers (so long as reads do not have a side effect such as automatically
   * reseting a register contents, etc.); and so on.
   */
  static final int VOLATILE = 2;

  /**
   * IMPURE indicates that any call p((x)) may have an externally visible effect. Standard examples
   * of primitives in this category include: division (which may raise an exception); writing to
   * memory; effectful I/O operations such as putchar or getchar; etc.
   */
  static final int IMPURE = 3;

  /**
   * DOESNTRETURN indicates that any call p((x)) is guaranteed not to return to the caller, so any
   * code that follows such a call can be deleted. For example, if p has the DOESNTRETURN property,
   * then any code sequence of the form (y <- p((x)); c) can be replaced with a simple tail call
   * p((x)).
   */
  static final int DOESNTRETURN = 4;

  /**
   * String labels corresponding to purity codes. The order of the strings should match the numeric
   * codes.
   */
  private static final String[] purityLabels =
      new String[] {"pure", "observer", "volatile", "impure", "doesntReturn"};

  /** Return a string version of the purity code for this primitive. */
  String purityLabel() {
    return (purity < 0 || purity >= purityLabels.length) ? null : purityLabels[purity];
  }

  /** Return a purity code for this string, or null if no matching label. */
  static int purityFromLabel(String p) {
    for (int i = 0; i < purityLabels.length; i++) {
      if (p.equals(purityLabels[i])) {
        return i;
      }
    }
    return (-1);
  }

  /** A table storing all of the primitives available at source level. */
  private static Prim[] table;

  /** A count of the number of primitives stored in the table. */
  private static int numPrims;

  /** Each primitive has an index, recording its position in the table. */
  private int index;

  /** Return the index of this primitive in the table. */
  int getIndex() {
    return index;
  }

  /** Add a new primitive to the table. */
  private static int addToPrimTable(Prim val) {
    if (table == null) {
      table = new Prim[40];
    } else if (numPrims >= table.length) {
      Prim[] newarray = new Prim[2 * table.length];
      for (int i = 0; i < table.length; i++) {
        newarray[i] = table[i];
      }
      table = newarray;
    }
    table[numPrims] = val;
    return numPrims++;
  }

  /** Write a list of primitives to the specified PrintWriter. */
  public static void dumpPrimitives(PrintWriter out) {
    out.println("Primitives: -----------------------------");
    for (int i = 0; i < numPrims; i++) {
      table[i].dump(out);
    }
    out.println(numPrims + " primitives listed");
    out.println("-----------------------------------------");
  }

  /** Write a description of this primitive to the specified PrintWriter. */
  void dump(PrintWriter out) {
    out.print("primitive ");
    out.print(id);
    out.print(" ");
    out.print(purityLabel());
    out.println(" :: " + blockType);
  }

  /**
   * Test whether this primitive is the same as another primitive. For PrimSing this requires class
   * equality; for other primitives, we use pointer equality.
   */
  public boolean samePrim(Prim p) {
    return this == p;
  }

  /** Represents tuple type [Flag] */
  protected static final Type flagTuple = Type.tuple(Tycon.flag.asType());

  /** Represents tuple type [Flag, Flag] */
  protected static final Type flagFlagTuple = Type.tuple(Tycon.flag.asType(), Tycon.flag.asType());

  /** Represents [Flag] >>= [Flag] for unary ops on flags. */
  protected static final BlockType unaryFlagType = new BlockType(flagTuple, flagTuple);

  /** Represents [Flag, Flag] >>= [Flag] for binary ops on flags. */
  protected static final BlockType binaryFlagType = new BlockType(flagFlagTuple, flagTuple);

  /** Represents tuple type [Word] */
  protected static final Type wordTuple = Type.tuple(Tycon.word.asType());

  /** Represents tuple type [Word, Word] */
  protected static final Type wordWordTuple = Type.tuple(Tycon.word.asType(), Tycon.word.asType());

  /** Represents [Word] >>= [Word] for unary ops on words. */
  protected static final BlockType unaryWordType = new BlockType(wordTuple, wordTuple);

  /** Represents [Word, Word] >>= [Word] for binary ops on words. */
  protected static final BlockType binaryWordType = new BlockType(wordWordTuple, wordTuple);

  /** Represents [Word, NZWord] >>= [Word] for div/rem on words. */
  protected static final BlockType nzdivType =
      new BlockType(Type.tuple(Tycon.word.asType(), Tycon.nzword.asType()), wordTuple);

  /** Represents [Flag] >>= [Word] for flag to word conversion. */
  protected static final BlockType flagToWordType = new BlockType(flagTuple, wordTuple);

  /** Represents [Word] >>= [Flag] for word to flag conversion. */
  protected static final BlockType wordToFlagType = new BlockType(wordTuple, flagTuple);

  /** Represents [Word, Word] >>= [Flag] for word comparison ops. */
  protected static final BlockType relopType = new BlockType(wordWordTuple, flagTuple);

  /** Make a clone of this Prim with a (possibly) new type. */
  Prim clone(BlockType bt) {
    return new Prim(id, bt, purity);
  }

  public static final class not extends PrimUnOp {

    private not() {
      this(unaryWordType);
    }

    private not(BlockType bt) {
      super("not", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new not(bt);
    }

    /** Primitive semantics: unary operators on Word values. */
    public long op(long n) {
      return (~n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.IOp(ty, llvm.Word.ONES, v, "xor");
    }
  }

  public static final PrimUnOp not = new not();

  public static final class and extends PrimBinOp {

    private and() {
      this(binaryWordType);
    }

    private and(BlockType bt) {
      super("and", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new and(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n & m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "and");
    }
  }

  public static final PrimBinOp and = new and();

  public static final class or extends PrimBinOp {

    private or() {
      this(binaryWordType);
    }

    private or(BlockType bt) {
      super("or", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new or(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n | m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "or");
    }
  }

  public static final PrimBinOp or = new or();

  public static final class xor extends PrimBinOp {

    private xor() {
      this(binaryWordType);
    }

    private xor(BlockType bt) {
      super("xor", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new xor(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n ^ m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "xor");
    }
  }

  public static final PrimBinOp xor = new xor();

  public static final class bnot extends PrimUnFOp {

    private bnot() {
      this(unaryFlagType);
    }

    private bnot(BlockType bt) {
      super("bnot", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new bnot(bt);
    }

    /** Primitive semantics: unary operators on Flag values. */
    public boolean op(boolean b) {
      return !b;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.i1, args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.IOp(ty, new llvm.Word(1), v, "xor");
    }
  }

  public static final PrimUnFOp bnot = new bnot();

  public static final class band extends PrimBinFOp {

    private band() {
      this(binaryFlagType);
    }

    private band(BlockType bt) {
      super("band", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new band(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
    public boolean op(boolean n, boolean m) {
      return n & m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "and");
    }
  }

  public static final PrimBinFOp band = new band();

  public static final class bor extends PrimBinFOp {

    private bor() {
      this(binaryFlagType);
    }

    private bor(BlockType bt) {
      super("bor", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new bor(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
    public boolean op(boolean n, boolean m) {
      return n | m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "or");
    }
  }

  public static final PrimBinFOp bor = new bor();

  public static final class bxor extends PrimBinFOp {

    private bxor() {
      this(binaryFlagType);
    }

    private bxor(BlockType bt) {
      super("bxor", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new bxor(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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
      return new llvm.IOp(ty, l, r, "xor");
    }
  }

  public static final PrimBinFOp bxor = new bxor();

  public static final class beq extends PrimBinFOp {

    private beq() {
      this(binaryFlagType);
    }

    private beq(BlockType bt) {
      super("beq", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new beq(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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

  public static final PrimBinFOp beq = new beq();

  public static final class blt extends PrimBinFOp {

    private blt() {
      this(binaryFlagType);
    }

    private blt(BlockType bt) {
      super("blt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new blt(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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

  public static final PrimBinFOp blt = new blt();

  public static final class ble extends PrimBinFOp {

    private ble() {
      this(binaryFlagType);
    }

    private ble(BlockType bt) {
      super("ble", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new ble(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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

  public static final PrimBinFOp ble = new ble();

  public static final class bgt extends PrimBinFOp {

    private bgt() {
      this(binaryFlagType);
    }

    private bgt(BlockType bt) {
      super("bgt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new bgt(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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

  public static final PrimBinFOp bgt = new bgt();

  public static final class bge extends PrimBinFOp {

    private bge() {
      this(binaryFlagType);
    }

    private bge(BlockType bt) {
      super("bge", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new bge(bt);
    }

    /** Primitive semantics: binary operators on Flag values. */
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

  public static final PrimBinFOp bge = new bge();

  public static final class shl extends PrimBinOp {

    private shl() {
      this(binaryWordType);
    }

    private shl(BlockType bt) {
      super("shl", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new shl(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n << m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "shl");
    }
  }

  public static final PrimBinOp shl = new shl();

  public static final class lshr extends PrimBinOp {

    private lshr() {
      this(binaryWordType);
    }

    private lshr(BlockType bt) {
      super("lshr", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new lshr(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n >>> m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "lshr");
    }
  }

  public static final PrimBinOp lshr = new lshr();

  public static final class ashr extends PrimBinOp {

    private ashr() {
      this(binaryWordType);
    }

    private ashr(BlockType bt) {
      super("ashr", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new ashr(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n >> m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "ashr");
    }
  }

  public static final PrimBinOp ashr = new ashr();

  public static final class neg extends PrimUnOp {

    private neg() {
      this(unaryWordType);
    }

    private neg(BlockType bt) {
      super("neg", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new neg(bt);
    }

    /** Primitive semantics: unary operators on Word values. */
    public long op(long n) {
      return (-n);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(lhs, this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s)), c);
    }

    /**
     * Generate an LLVM right hand side for this unary MIL primitive with the given value as input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value v) {
      return new llvm.IOp(ty, llvm.Word.ZERO, v, "sub");
    }
  }

  public static final PrimUnOp neg = new neg();

  public static final class add extends PrimBinOp {

    private add() {
      this(binaryWordType);
    }

    private add(BlockType bt) {
      super("add", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new add(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n + m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "add");
    }
  }

  public static final PrimBinOp add = new add();

  public static final class sub extends PrimBinOp {

    private sub() {
      this(binaryWordType);
    }

    private sub(BlockType bt) {
      super("sub", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new sub(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n - m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "sub");
    }
  }

  public static final PrimBinOp sub = new sub();

  public static final class mul extends PrimBinOp {

    private mul() {
      this(binaryWordType);
    }

    private mul(BlockType bt) {
      super("mul", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new mul(bt);
    }

    /** Primitive semantics: binary operators on Word values. */
    public long op(long n, long m) {
      return n * m;
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "mul");
    }
  }

  public static final PrimBinOp mul = new mul();

  public static final class div extends PrimSing {

    private div() {
      this(binaryWordType);
    }

    private div(BlockType bt) {
      super("div", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new div(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      long n = stack[fp].getInt();
      long d = stack[fp + 1].getInt();
      if (d == 0) {
        throw new Failure("divide by zero error");
      }
      stack[fp] = new WordValue(n / d);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "sdiv");
    }
  }

  public static final PrimSing div = new div();

  public static final class rem extends PrimSing {

    private rem() {
      this(binaryWordType);
    }

    private rem(BlockType bt) {
      super("rem", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new rem(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      long n = stack[fp].getInt();
      long d = stack[fp + 1].getInt();
      if (d == 0) {
        throw new Failure("divide by zero error (for mod)");
      }
      stack[fp] = new WordValue(n % d);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "srem");
    }
  }

  public static final PrimSing rem = new rem();

  public static final class nzdiv extends PrimSing {

    private nzdiv() {
      this(nzdivType);
    }

    private nzdiv(BlockType bt) {
      super("nzdiv", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new nzdiv(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "udiv");
    }
  }

  public static final PrimSing nzdiv = new nzdiv();

  public static final class nzrem extends PrimSing {

    private nzrem() {
      this(nzdivType);
    }

    private nzrem(BlockType bt) {
      super("nzrem", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new nzrem(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      debug.Internal.error(id + " is not a void primitive");
      return c;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(
          lhs,
          this.op(llvm.Type.word(), args[0].toLLVMAtom(lm, vm, s), args[1].toLLVMAtom(lm, vm, s)),
          c);
    }

    /**
     * Generate an LLVM right hand side for this binary MIL primitive with the given values as
     * input.
     */
    llvm.Rhs op(llvm.Type ty, llvm.Value l, llvm.Value r) {
      return new llvm.IOp(ty, l, r, "urem");
    }
  }

  public static final PrimSing nzrem = new nzrem();

  public static final class eq extends PrimRelOp {

    private eq() {
      this(relopType);
    }

    private eq(BlockType bt) {
      super("primEq", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new eq(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimRelOp eq = new eq();

  public static final class neq extends PrimRelOp {

    private neq() {
      this(relopType);
    }

    private neq(BlockType bt) {
      super("primNeq", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new neq(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimRelOp neq = new neq();

  public static final class slt extends PrimSCmpOp {

    private slt() {
      this(relopType);
    }

    private slt(BlockType bt) {
      super("primSlt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new slt(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimSCmpOp slt = new slt();

  public static final class sle extends PrimSCmpOp {

    private sle() {
      this(relopType);
    }

    private sle(BlockType bt) {
      super("primSle", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new sle(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimSCmpOp sle = new sle();

  public static final class sgt extends PrimSCmpOp {

    private sgt() {
      this(relopType);
    }

    private sgt(BlockType bt) {
      super("primSgt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new sgt(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimSCmpOp sgt = new sgt();

  public static final class sge extends PrimSCmpOp {

    private sge() {
      this(relopType);
    }

    private sge(BlockType bt) {
      super("primSge", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new sge(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimSCmpOp sge = new sge();

  public static final class ult extends PrimUCmpOp {

    private ult() {
      this(relopType);
    }

    private ult(BlockType bt) {
      super("primUlt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new ult(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimUCmpOp ult = new ult();

  public static final class ule extends PrimUCmpOp {

    private ule() {
      this(relopType);
    }

    private ule(BlockType bt) {
      super("primUle", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new ule(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimUCmpOp ule = new ule();

  public static final class ugt extends PrimUCmpOp {

    private ugt() {
      this(relopType);
    }

    private ugt(BlockType bt) {
      super("primUgt", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new ugt(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimUCmpOp ugt = new ugt();

  public static final class uge extends PrimUCmpOp {

    private uge() {
      this(relopType);
    }

    private uge(BlockType bt) {
      super("primUge", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new uge(bt);
    }

    /** Primitive semantics: comparisons of Word values. */
    public boolean op(long n, long m) {
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

  public static final PrimUCmpOp uge = new uge();

  public static final class flagToWord extends PrimFtoW {

    private flagToWord() {
      this(flagToWordType);
    }

    private flagToWord(BlockType bt) {
      super("flagToWord", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new flagToWord(bt);
    }

    /** Primitive semantics: operators from Flag to Word. */
    public long op(boolean b) {
      return b ? 1 : 0;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(lhs, new llvm.Zext(args[0].toLLVMAtom(lm, vm, s), llvm.Type.word()), c);
    }
  }

  public static final PrimFtoW flagToWord = new flagToWord();

  public static final class wordToFlag extends PrimWtoF {

    private wordToFlag() {
      this(wordToFlagType);
    }

    private wordToFlag(BlockType bt) {
      super("wordToFlag", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new wordToFlag(bt);
    }

    /** Primitive semantics: operators from Word to Flag. */
    public boolean op(long n) {
      return (n & 1) != 0;
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return new llvm.Op(lhs, new llvm.Trunc(args[0].toLLVMAtom(lm, vm, s), llvm.Type.i1), c);
    }
  }

  public static final PrimWtoF wordToFlag = new wordToFlag();

  /** Represents the polymorphic block type forall (r::tuple). [] >>= r. */
  static final BlockType haltType = new PolyBlockType(Type.empty, Type.gen(0), Prefix.tuple);

  public static final class halt extends Prim {

    private halt() {
      this(haltType);
    }

    private halt(BlockType bt) {
      super("halt", bt, DOESNTRETURN);
    }

    Prim clone(BlockType bt) {
      return new halt(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      throw new Failure("halt primitive executed");
    }

    /**
     * Return true if this code enters a non-productive black hole (i.e., if it immediately calls
     * halt or loop).
     */
    boolean blackholes() {
      return true;
    }
  }

  public static final Prim halt = new halt();

  public static final class loop extends Prim {

    private loop() {
      this(haltType);
    }

    private loop(BlockType bt) {
      super("loop", bt, DOESNTRETURN);
    }

    Prim clone(BlockType bt) {
      return new loop(bt);
    }

    /**
     * Return true if this code enters a non-productive black hole (i.e., if it immediately calls
     * halt or loop).
     */
    boolean blackholes() {
      return true;
    }
  }

  public static final Prim loop = new loop();

  /** Create a call with the given arguments. */
  public Call withArgs(Atom[] args) {
    return new PrimCall(this, args);
  }

  /** Create a call with no arguments. */
  public Call withArgs() {
    return withArgs(Atom.noAtoms);
  }

  /** Create a call with a single argument. */
  public Call withArgs(Atom a) {
    return withArgs(new Atom[] {a});
  }

  /** Create a call with two Atom arguments. */
  public Call withArgs(Atom a, Atom b) {
    return withArgs(new Atom[] {a, b});
  }

  /** Create a call with two arguments: an Atom and a Word. */
  public Call withArgs(Atom a, long n) {
    return withArgs(new Atom[] {a, new Word(n)});
  }

  /** Create a call with two arguments: a Word and an Atom. */
  public Call withArgs(long n, Atom b) {
    return withArgs(new Atom[] {new Word(n), b});
  }

  static void addBuiltins(Builtin builtin) throws Failure {
    for (int i = 0; i < numPrims; i++) {
      builtin.addNewPrim(table[i]);
    }
  }

  BlockType instantiate() {
    return blockType.instantiate();
  }

  static void exec(PrintWriter out, int prim, int fp, Value[] stack) throws Failure {
    if (prim < 0 || prim >= numPrims) {
      throw new Failure("primitive number " + prim + " is not defined");
    }
    table[prim].exec(out, fp, stack);
  }

  static String showPrim(int i) {
    return (i >= 0 && i < numPrims && table[i].id != null) ? table[i].id : ("?prim_" + i);
  }

  protected static final BlockType wordToUnitType = new BlockType(wordTuple, Type.empty);

  public static final class printWord extends Prim {

    private printWord() {
      this(wordToUnitType);
    }

    private printWord(BlockType bt) {
      super("printWord", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new printWord(bt);
    }

    void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
      out.println("printWord: " + stack[fp].getInt());
    }
  }

  public static final Prim printWord = new printWord();

  void exec(PrintWriter out, int fp, Value[] stack) throws Failure {
    throw new Failure("primitive \"" + id + "\" not available");
  }

  /** Test for code that is guaranteed not to return. */
  boolean doesntReturn() {
    return purity >= DOESNTRETURN;
  }

  /**
   * Determine if a given tail expression has no externally visible side effects. This is true for
   * returns, allocators, and selections, and for calls to primitives that are not impure.
   */
  boolean hasNoEffect() {
    return purity < IMPURE;
  }

  /**
   * Return true if this code enters a non-productive black hole (i.e., if it immediately calls halt
   * or loop).
   */
  boolean blackholes() {
    return false;
  }

  protected static final BlockType nopType = new BlockType(Type.empty, Type.empty);

  public static final class noinline extends PrimNop {

    private noinline() {
      this(nopType);
    }

    private noinline(BlockType bt) {
      super("noinline", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new noinline(bt);
    }
  }

  public static final PrimNop noinline = new noinline();

  /**
   * Test whether a given tail expression may be repeatable (i.e., whether the results of a previous
   * use of the same tail can be reused instead of repeating the tail). TODO: is there a better name
   * for this?
   */
  public boolean isRepeatable() {
    return purity <= OBSERVER;
  }

  /**
   * Test whether a given tail expression is pure (no externally visible side effects and no
   * dependence on other effects).
   */
  public boolean isPure() {
    return purity == PURE;
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
   * equivalent program fragments have the same summary value. TODO: should we calculate long
   * summary values instead of using int?
   */
  int summary() {
    return id.hashCode();
  }

  /** Generate a new version of this primitive that has canonical types wrt the given set. */
  Prim canonPrim(TypeSet set) {
    Prim newP = set.getPrim(this);
    if (newP == null) {
      BlockType bt = blockType.canonBlockType(set);
      if (bt.alphaEquiv(blockType)) {
        newP = this;
      } else {
        newP = this.clone(bt);
        debug.Log.println("new version of primitive " + newP.id + " :: " + bt);
        debug.Log.println("         old version was " + this.id + " :: " + blockType);
      }
      set.putPrim(this, newP);
    }
    return newP;
  }

  /**
   * Generate a specialized version of this primitive at a given monotype. Reuses previously
   * specialized versions if available, or returns the original primitive if type is monomorphic.
   */
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
      // TODO: should the new primitive include a pointer back to the Prim
      // from which it was derived?
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

  /**
   * A class for primitives whose implementation will be provided by the specified implementation
   * Block, substituted in for the primitive during representation transformation.
   */
  public static class BlockImpl extends Prim {

    private Block impl;

    /** Default constructor. */
    public BlockImpl(String id, BlockType blockType, int purity, Block impl) {
      super(id, blockType, purity);
      this.impl = impl;
    }

    public Prim clone(BlockType bt) {
      return new BlockImpl(id, bt, purity, impl);
    }

    Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
      return new BlockCall(impl, targs);
    }
  }

  static final Type bit1 = Type.bit(1);

  static final Type bit8 = Type.bit(8);

  static final Type bit16 = Type.bit(16);

  static final Type bit32 = Type.bit(32);

  static final Type bit64 = Type.bit(64);

  static final Type addrType = Tycon.addr.asType();

  static final Type addrTuple = Type.tuple(addrType);

  static final Type unitTuple = Type.tuple(Tycon.unit.asType());

  static final BlockType load1type = new BlockType(addrTuple, Type.tuple(bit1));

  static final BlockType load8type = new BlockType(addrTuple, Type.tuple(bit8));

  static final BlockType load16type = new BlockType(addrTuple, Type.tuple(bit16));

  static final BlockType load32type = new BlockType(addrTuple, Type.tuple(bit32));

  static final BlockType load64type = new BlockType(addrTuple, Type.tuple(bit64));

  static final BlockType store1type = new BlockType(Type.tuple(addrType, bit1), unitTuple);

  static final BlockType store8type = new BlockType(Type.tuple(addrType, bit8), unitTuple);

  static final BlockType store16type = new BlockType(Type.tuple(addrType, bit16), unitTuple);

  static final BlockType store32type = new BlockType(Type.tuple(addrType, bit32), unitTuple);

  static final BlockType store64type = new BlockType(Type.tuple(addrType, bit64), unitTuple);

  public static final class load1 extends Prim {

    private load1() {
      this(load1type);
    }

    private load1(BlockType bt) {
      super("load1", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new load1(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return loadLLVM(lm, vm, s, args, llvm.Type.i1, lhs, c);
    }
  }

  public static final Prim load1 = new load1();

  public static final class load8 extends Prim {

    private load8() {
      this(load8type);
    }

    private load8(BlockType bt) {
      super("load8", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new load8(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return loadLLVM(lm, vm, s, args, llvm.Type.i8, lhs, c);
    }
  }

  public static final Prim load8 = new load8();

  public static final class load16 extends Prim {

    private load16() {
      this(load16type);
    }

    private load16(BlockType bt) {
      super("load16", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new load16(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return loadLLVM(lm, vm, s, args, llvm.Type.i16, lhs, c);
    }
  }

  public static final Prim load16 = new load16();

  public static final class load32 extends Prim {

    private load32() {
      this(load32type);
    }

    private load32(BlockType bt) {
      super("load32", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new load32(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return loadLLVM(lm, vm, s, args, llvm.Type.i32, lhs, c);
    }
  }

  public static final Prim load32 = new load32();

  public static final class load64 extends Prim {

    private load64() {
      this(load64type);
    }

    private load64(BlockType bt) {
      super("load64", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new load64(bt);
    }

    /**
     * Representation transformation for memory accesses: Generates an implementation of a 64 bit
     * memory access by using a pair of 32 bit memory accesses, if Word.size==32. Assumes little
     * endian memory layout.
     */
    Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
      final int wordsize = Word.size();
      if (wordsize == 64) {
        return super.repTransformPrim(set, targs);
      } else if (wordsize == 32) {
        if (impl == null) {
          Temp[] vs = Temp.makeTemps(1);
          Temp a = new Temp();
          Temp lsw = new Temp();
          Temp msw = new Temp();
          Prim p = Prim.load32.canonPrim(set);
          impl =
              new Block(
                  BuiltinPosition.pos,
                  vs,
                  new Bind(
                      lsw,
                      p.withArgs(vs[0]),
                      new Bind(
                          a,
                          Prim.add.withArgs(vs[0], 4),
                          new Bind(
                              msw, p.withArgs(a), new Done(new Return(new Atom[] {lsw, msw}))))));
        }
        return new BlockCall(impl, targs);
      } else {
        debug.Internal.error(
            "Unrecognized wordsize " + wordsize + " in repTransformPrim for load64");
        return null; /* not reached */
      }
    }

    private static Block impl = null;

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is expected to return a result (that should be captured in the specified lhs), and
     * then execution is expected to continue on to the specified code, c.
     */
    llvm.Code toLLVMPrimCont(
        LLVMMap lm,
        VarMap vm,
        TempSubst s,
        Atom[] args,
        boolean isTail,
        llvm.Local lhs,
        llvm.Code c) {
      return loadLLVM(lm, vm, s, args, llvm.Type.i64, lhs, c);
    }
  }

  public static final Prim load64 = new load64();

  public static final class store1 extends Prim {

    private store1() {
      this(store1type);
    }

    private store1(BlockType bt) {
      super("store1", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new store1(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      return storeLLVM(lm, vm, s, args, llvm.Type.i1, c);
    }
  }

  public static final Prim store1 = new store1();

  public static final class store8 extends Prim {

    private store8() {
      this(store8type);
    }

    private store8(BlockType bt) {
      super("store8", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new store8(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      return storeLLVM(lm, vm, s, args, llvm.Type.i8, c);
    }
  }

  public static final Prim store8 = new store8();

  public static final class store16 extends Prim {

    private store16() {
      this(store16type);
    }

    private store16(BlockType bt) {
      super("store16", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new store16(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      return storeLLVM(lm, vm, s, args, llvm.Type.i16, c);
    }
  }

  public static final Prim store16 = new store16();

  public static final class store32 extends Prim {

    private store32() {
      this(store32type);
    }

    private store32(BlockType bt) {
      super("store32", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new store32(bt);
    }

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      return storeLLVM(lm, vm, s, args, llvm.Type.i32, c);
    }
  }

  public static final Prim store32 = new store32();

  public static final class store64 extends Prim {

    private store64() {
      this(store64type);
    }

    private store64(BlockType bt) {
      super("store64", bt, IMPURE);
    }

    Prim clone(BlockType bt) {
      return new store64(bt);
    }

    /**
     * Representation transformation for memory accesses: Generates an implementation of a 64 bit
     * memory access by using a pair of 32 bit memory accesses, if Word.size==32. Assumes little
     * endian memory layout.
     */
    Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
      final int wordsize = Word.size();
      if (wordsize == 64) {
        return super.repTransformPrim(set, targs);
      } else if (wordsize == 32) {
        if (impl == null) {
          // Build implementation of the form:
          //    store64[addr, lsw, msw]
          //      = _  <- store32(addr, lsw)
          //        a  <- add((addr, 4))
          //        store32((a, msw))
          Temp[] vs = Temp.makeTemps(3);
          Temp a = new Temp();
          Prim p = Prim.store32.canonPrim(set);
          impl =
              new Block(
                  BuiltinPosition.pos,
                  vs,
                  new Bind(
                      new Temp(),
                      p.withArgs(vs[0], vs[1]),
                      new Bind(a, Prim.add.withArgs(vs[0], 4), new Done(p.withArgs(a, vs[2])))));
        }
        return new BlockCall(impl, targs);
      } else {
        debug.Internal.error(
            "Unrecognized wordsize " + wordsize + " in repTransformPrim for store64");
        return null; /* not reached */
      }
    }

    private static Block impl = null;

    /**
     * Generate code for a MIL PrimCall with the specified arguments in a context where the
     * primitive is not expected to produce any results, but execution is expected to continue with
     * the given code.
     */
    llvm.Code toLLVMPrimVoid(
        LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
      return storeLLVM(lm, vm, s, args, llvm.Type.i64, c);
    }
  }

  public static final Prim store64 = new store64();

  static final Type init0Type = Type.init(Type.gen(0));

  static final BlockType initSeqType =
      new PolyBlockType(Type.tuple(init0Type, init0Type), Type.tuple(init0Type), Prefix.area);

  public static final class initSeq extends Prim {

    private initSeq() {
      this(initSeqType);
    }

    private initSeq(BlockType bt) {
      super("primInitSeq", bt, PURE);
    }

    Prim clone(BlockType bt) {
      return new initSeq(bt);
    }

    /** Records the closure structure that is used to implement this primitive. */
    private ClosureDefn impl = null;

    Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
      if (impl == null) {
        Temp[] ijr = Temp.makeTemps(3);
        Block b =
            new Block(
                BuiltinPosition.pos,
                ijr, //  b[i, j, r]
                new Bind(
                    new Temp(),
                    new Enter(ijr[0], ijr[2]), //    =  _ <- i @ r
                    new Done(new Enter(ijr[1], ijr[2])))); //       j @ r
        Temp[] ij = Temp.makeTemps(2);
        Temp[] r = Temp.makeTemps(1);
        impl =
            new ClosureDefn(
                BuiltinPosition.pos,
                ij,
                r, // impl{i, j} r
                new BlockCall(b, Temp.append(ij, r))); //   = b[i, j, r]
      }
      return new ClosAlloc(impl, targs);
    }
  }

  public static final Prim initSeq = new initSeq();

  /**
   * Representation for structure field initializers that map initializers for single fields into
   * initializers for full structures. Used individually, these primitives would not guarantee full
   * initialization of a structure; instead, they should be used in combination when compiling a
   * structure with one initializer for every field.
   */
  static class InitStructField extends Prim {

    private int offset;

    /** Default constructor. */
    InitStructField(String id, BlockType blockType, int purity, int offset) {
      super(id, blockType, purity);
      this.offset = offset;
    }

    Prim clone(BlockType bt) {
      return new InitStructField(id, bt, purity, offset);
    }

    /**
     * A closure for representing structure field initializer functions with types of the form Init
     * T -> Init S, where T is the type of a field within structure S at offset O. The
     * initStructFieldClos has two stored fields, one for the Init T initializer and one for the
     * offset O; when entered with a reference to a structure, it calculates a reference to the
     * field (by adding O to the incoming reference) and then runs the initializer using the
     * resulting address.
     */
    private static ClosureDefn initStructFieldClos = null;

    static {

      // Initialize initStructFieldClos:
      Temp[] ior = Temp.makeTemps(3);
      Temp a = new Temp();
      Block b =
          new Block(
              BuiltinPosition.pos,
              ior, // b[i, o, r]
              new Bind(
                  a,
                  Prim.add.withArgs(ior[2], ior[1]), //   = a <- add((r, o))
                  new Done(new Enter(ior[0], a)))); //     i @ a
      Temp[] io = Temp.makeTemps(2);
      Temp[] r = Temp.makeTemps(1);
      initStructFieldClos =
          new ClosureDefn(
              BuiltinPosition.pos,
              io,
              r, // initStructFieldClos{i, o} r
              new BlockCall(b, Temp.append(io, r))); //   = b[i, o, r]
    }

    /**
     * Rewrite this call init_x((i)) with a closure allocation (function value) of the form
     * initStructFieldClos(i, o), for the associated field offset o.
     */
    Tail repTransformPrim(RepTypeSet set, Atom[] targs) {
      return new ClosAlloc(initStructFieldClos).withArgs(targs[0], offset);
    }
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
  llvm.Code toLLVMPrimVoid(
      LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, boolean isTail, llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.CallVoid(isTail, lm.globalFor(this), Atom.toLLVMValues(lm, vm, s, args), c);
  }

  /**
   * Generate code for a MIL PrimCall with the specified arguments in a context where the primitive
   * is expected to return a result (that should be captured in the specified lhs), and then
   * execution is expected to continue on to the specified code, c.
   */
  llvm.Code toLLVMPrimCont(
      LLVMMap lm,
      VarMap vm,
      TempSubst s,
      Atom[] args,
      boolean isTail,
      llvm.Local lhs,
      llvm.Code c) {
    // Default approach is to call a function:
    return new llvm.Op(
        lhs,
        new llvm.Call(
            isTail, lhs.getType(), lm.globalFor(this), Atom.toLLVMValues(lm, vm, s, args)),
        c);
  }

  /**
   * Generate an LLVM code sequence to store (a portion of) a given Word value at a specified
   * address.
   */
  static llvm.Code storeLLVM(
      LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Type ty, llvm.Code c) {
    llvm.Type pt = llvm.Type.ptr; // pointer type
    llvm.Local p = vm.reg(pt); // register to hold pointer
    llvm.Value v = args[1].toLLVMAtom(lm, vm, s); // value to store
    if (ty == v.getType()) { // store v directly if types match
      c = new llvm.Store(v, p, c);
    } else { // truncate and store if types do not match
      llvm.Local r = vm.reg(ty); // register to hold truncated value
      c = new llvm.Op(r, new llvm.Trunc(v, ty), new llvm.Store(r, p, c));
    }
    return new llvm.Op(p, new llvm.IntToPtr(args[0].toLLVMAtom(lm, vm, s), pt), c);
  }

  /**
   * Generate an LLVM code sequence to load a value of the given type from a specified address. We
   * assume that the data that is being loaded will be either the same size or else smaller than a
   * single machine word: for example, we may load an i8, i16, or i32 into an i32, but we should not
   * attempt to load an i64 into an i32.
   */
  static llvm.Code loadLLVM(
      LLVMMap lm, VarMap vm, TempSubst s, Atom[] args, llvm.Type ty, llvm.Local lhs, llvm.Code c) {
    llvm.Type pt = llvm.Type.ptr; // pointer type
    llvm.Local p = vm.reg(pt); // register to hold pointer
    if (ty == lhs.getType()) {
      // load directly into lhs if types match
      c = new llvm.Op(lhs, new llvm.Load(ty, p), c);
    } else {
      // zero extend loaded value if types do not match (assumes lhs
      // type is wider than ty)
      llvm.Local v = vm.reg(ty); // register to hold value
      c =
          new llvm.Op(
              v, new llvm.Load(ty, p), new llvm.Op(lhs, new llvm.Zext(v, lhs.getType()), c));
    }
    return new llvm.Op(p, new llvm.IntToPtr(args[0].toLLVMAtom(lm, vm, s), pt), c);
  }
}
