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
import compiler.Position;
import core.*;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;

public class External extends TopDefn {

  private String id;

  private Scheme declared;

  private String ref;

  private Type[] ts;

  /** Default constructor. */
  public External(Position pos, String id, Scheme declared, String ref, Type[] ts) {
    super(pos);
    this.id = id;
    this.declared = declared;
    this.ref = ref;
    this.ts = ts;
  }

  private static int count = 0;

  public External(Position pos, Scheme declared, String ref, Type[] ts) {
    this(pos, "e" + count++, declared, ref, ts);
  }

  /**
   * Return references to all components of this top level definition in an array of
   * atoms/arguments.
   */
  Atom[] tops() {
    return new TopExt[] {new TopExt(this)};
  }

  /** Get the declared type, or null if no type has been set. */
  public Scheme getDeclared() {
    return declared;
  }

  /** Set the declared type. */
  public void setDeclared(Scheme declared) {
    this.declared = declared;
  }

  /** Return the identifier that is associated with this definition. */
  public String getId() {
    return id;
  }

  public String toString() {
    return id;
  }

  /** Find the list of Defns that this Defn depends on. */
  public Defns dependencies() {
    return null;
  }

  boolean dotInclude() {
    return false;
  }

  void displayDefn(PrintWriter out) {
    out.print("external " + id);
    if (ref != null) {
      out.print(" {" + ref);
      for (int i = 0; i < ts.length; i++) {
        out.print(" ");
        out.print(ts[i].toString(TypeWriter.ALWAYS));
      }
      out.print("}");
    }
    out.println(" :: " + declared);
  }

  /** Return a type for an instantiated version of this item when used as Atom (input operand). */
  public Type instantiate() {
    return declared.instantiate();
  }

  /**
   * Set the initial type for this definition by instantiating the declared type, if present, or
   * using type variables to create a suitable skeleton. Also sets the types of bound variables.
   */
  void setInitialType() throws Failure {
    /* Nothing to do here */
  }

  /**
   * Type check the body of this definition, but reporting rather than throwing' an exception error
   * if the given handler is not null.
   */
  void checkBody(Handler handler) throws Failure {
    /* Nothing to do here */
  }

  /** Type check the body of this definition, throwing an exception if there is an error. */
  void checkBody(Position pos) throws Failure {
    /* Nothing to do here */
  }

  /**
   * Calculate a generalized type for this binding, adding universal quantifiers for any unbound
   * type variable in the inferred type. (There are no "fixed" type variables here because all mil
   * definitions are at the top level.)
   */
  void generalizeType(Handler handler) throws Failure {
    /* nothing to do here */
  }

  void findAmbigTVars(Handler handler, TVars gens) {
    /* Nothing to do here */
  }

  /** First pass code generation: produce code for top-level definitions. */
  void generateMain(Handler handler, MachineBuilder builder) {
    handler.report(new Failure(pos, "Cannot access external symbol \"" + id + "\" from bytecode"));
  }

  /** Apply inlining. */
  public void inlining() {
    /* Nothing to do here */
  }

  /**
   * Count the number of unused arguments for this definition using the current unusedArgs
   * information for any other items that it references.
   */
  int countUnusedArgs() {
    return 0;
  }

  /** Rewrite this program to remove unused arguments in block calls. */
  void removeUnusedArgs() {
    /* Nothing to do here */
  }

  public void flow() {
    /* Nothing to do here */
  }

  /**
   * Compute a summary for this definition (if it is a block or top-level) and then look for a
   * previously encountered item with the same code in the given table. Return true if a duplicate
   * was found.
   */
  boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels) {
    return false;
  }

  void eliminateDuplicates() {
    /* Nothing to do here */
  }

  void collect() {
    /* Nothing to do here */
  }

  void collect(TypeSet set) {
    declared = declared.canonScheme(set);
    if (ts != null) {
      for (int i = 0; i < ts.length; i++) {
        ts[i] = ts[i].canonType(set);
      }
    }
  }

  /** Apply constructor function simplifications to this program. */
  void cfunSimplify() {
    /* Nothing to do here */
  }

  void printlnSig(PrintWriter out) {
    out.println("external " + id + " :: " + declared);
  }

  /** Test to determine if this is an appropriate definition to match the given type. */
  External isExternalOfType(Scheme inst) {
    return declared.alphaEquiv(inst) ? this : null;
  }

  External(External e) {
    this(e.pos, e.declared, e.ref, e.ts);
  }

  /** Handle specialization of Externals */
  void specialize(MILSpec spec, External eorig) {
    debug.Log.println(
        "External specialize: "
            + eorig
            + " :: "
            + eorig.declared
            + "  ~~>  "
            + this
            + " :: "
            + this.declared);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original.
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    throw new ExternalAsEntrypoint(this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
  }

  void topLevelrepTransform(Handler handler, RepTypeSet set) {
    declared = declared.canonType(set);
    debug.Log.println("Determining representation for external " + id + " :: " + declared);
    Type[] r = declared.repCalc();
    Tail t = generateTail(handler);
    if (t == null) { // Program will continue to use an external definition
      if (r != null) { // Check for a change in representation
        if (r.length != 1) {
          // TODO: do something to avoid the following error
          debug.Internal.error(
              "Cannot handle change of representation for external " + id + " :: " + declared);
        }
        impl = new External(pos, id, r[0], null, null); // do not copy ref or ts
        debug.Log.println("Replaced external definition with " + id + " :: " + r[0]);
      }
    } else { // Generator has produced an implementation for this external
      TopLhs[] lhs; // Create a left hand side for the new top level definition
      if (r == null) { // no change in type representation:
        lhs = new TopLhs[] {new TopLhs()};
        lhs[0].setDeclared(declared);
      } else {
        lhs = new TopLhs[r.length];
        for (int i = 0; i < r.length; i++) {
          lhs[i] = new TopLhs();
          lhs[i].setDeclared(r[i]);
        }
      }
      // TODO: it seems inconsistent to use a HashMap for topLevelRepMap, while using a field here
      // ...
      impl =
          new TopLevel(
              pos, lhs, t); // Make new top level to use as the replacement for this External
      debug.Log.println("Generated new top level definition for " + impl);
    }
  }

  private TopDefn impl = null;

  Atom[] repExt() {
    return (impl == null) ? null : impl.tops();
  }

  protected abstract static class Generator {

    /** Minimum number of type arguments needed to use this generator. */
    int needs;

    /** Default constructor. */
    protected Generator(int needs) {
      this.needs = needs;
    }

    /**
     * Generate a tail as the implementation of an external described by a reference and list of
     * types.
     */
    abstract Tail generate(Position pos, Type[] ts);
  }

  /**
   * Stores a mapping from String references to generators for external function implementations.
   */
  private static HashMap<String, Generator> generators = new HashMap();

  /**
   * Use the ref and ts fields to determine if we can generate an implementation, post
   * representation transformation, for an external primitive.
   */
  Tail generateTail(Handler handler) {
    if (ref != null && ts != null) { // Do not generate code if ref or ts is missing
      Generator gen = generators.get(ref);
      if (gen != null && ts.length >= gen.needs) {
        return gen.generate(pos, ts);
      }
      if (ts.length == 0) {
        Tail t = declared.generatePrim(pos, ref);
        if (t != null) {
          return t;
        }
      }
      handler.report(new Failure(pos, "No generated implementation"));
    }
    return null;
  }

  static {

    // primBitFromLiteral v w ... :: Proxy -> Bit w
    generators.put(
        "primBitFromLiteral",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            //  TODO: values returned by these getNat() calls should be representable with a single
            // int (no overflow)
            BigInteger v = ts[0].getNat(); // Value of literal
            BigInteger w = ts[1].getBitArg(); // Width of bit vector
            if (v != null
                && w != null
                && v.compareTo(BigInteger.ZERO) >= 0
                && BigInteger.ONE.shiftLeft(w.intValue()).compareTo(v) > 0) {
              Tail t = new Return(Const.atoms(v, w.intValue()));
              // TODO: Temp.makeTemps(1) in the next line is used for the proxy argument; can we
              // eliminate this?
              ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(1), t);
              return new ClosAlloc(k).withArgs();
            }
            return null;
          }
        });

    // primBitConcat m n p :: Bit m -> Bit n -> Bit p,  where m+n = p
    generators.put(
        ":#",
        new Generator(3) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger m = ts[0].getBitArg(); // Width of first input (most significant bits)
            BigInteger n = ts[1].getBitArg(); // Width of second input (least significant bits)
            BigInteger p = ts[2].getBitArg(); // width of result
            if (m != null && n != null && p != null && m.add(n).compareTo(p) == 0) {
              int mw = m.intValue();
              int nw = n.intValue();
              return new BlockCall(BitdataLayout.generateBitConcat(pos, mw, nw))
                  .makeBinaryFuncClosure(pos, Type.numWords(mw), Type.numWords(nw));
            }
            return null;
          }
        });
  }

  static {

    // primIxFromLiteral v m :: Proxy -> Ix m
    generators.put(
        "primIxFromLiteral",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger v = ts[0].getNat(); // Value of literal
            BigInteger m = ts[1].getIxArg(); // Modulus for index type
            if (v != null && m != null && v.compareTo(BigInteger.ZERO) >= 0 && v.compareTo(m) < 0) {
              Tail t = new Return(new IntConst(v.intValue()));
              // TODO: Temp.makeTemps(1) in the next line is used for the proxy argument; can we
              // eliminate this?
              ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(1), t);
              return new ClosAlloc(k).withArgs();
            }
            return null;
          }
        });

    // primIxMaxBound w :: Ix w
    generators.put(
        "primIxMaxBound",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getIxArg(); // Modulus for index type
            if (w != null) {
              return new Return(new IntConst(w.subtract(BigInteger.ONE).intValue()));
            }
            return null;
          }
        });

    // primIxToBits m w :: Ix m -> Bit w
    generators.put(
        "primIxToBits",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger m = ts[0].getIxArg(); // Modulus for index type
            BigInteger w = ts[1].getBitArg(); // Width of bitdata type
            if (m != null
                && w != null
                && BigInteger.ONE.shiftLeft(w.intValue()).compareTo(m) >= 0) {
              Temp[] vs = Temp.makeTemps(1); // Argument holds incoming index
              int n = Type.numWords(w.intValue());
              Atom[] as = new Atom[n];
              as[0] = vs[0];
              for (int i = 1; i < n; i++) { // In general, could return multiple words
                as[i] = IntConst.Zero;
              }
              ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, vs, new Return(as));
              return new ClosAlloc(k).withArgs();
            }
            return null;
          }
        });

    // primIxShiftL w p :: Ix n -> Ix p -> Ix n,  where w=2^p
    generators.put(
        "primIxShiftL",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getIxArg(); // Modulus for index type
            BigInteger p = ts[1].getIxArg(); // Modulus for shift amount
            if (w != null
                && p != null
                && BigInteger.ONE.shiftLeft(p.intValue()).compareTo(w) == 0) {
              Temp[] vs = Temp.makeTemps(2); // One word for each Ix argument
              Block b = new Block(pos, vs, maskTail(Prim.shl.withArgs(vs[0], vs[1]), w.intValue()));
              return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
            }
            return null;
          }
        });

    // primIxShiftR w p :: Ix n -> Ix p -> Ix n,  where w=2^p
    generators.put(
        "primIxShiftR",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getIxArg(); // Modulus for index type
            BigInteger p = ts[1].getIxArg(); // Modulus for shift amount
            if (w != null
                && p != null
                && BigInteger.ONE.shiftLeft(p.intValue()).compareTo(w) == 0) {
              return new PrimCall(Prim.lshr).makeBinaryFuncClosure(pos, 1, 1);
            }
            return null;
          }
        });

    // primModIx m w :: Bit w -> Ix m
    generators.put(
        "primModIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger m = ts[0].getIxArg(); // Modulus for index type
            BigInteger w = ts[1].getBitArg(); // Width of bitdata type
            if (m != null && w != null) {
              int mod = m.intValue();
              int width = w.intValue();
              int n = Type.numWords(width);
              if ((mod & (mod - 1)) == 0) { // Test for power of two
                Temp[] args = Temp.makeTemps(n);
                Tail t = Prim.and.withArgs(args[0], mod - 1);
                return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
              }
              if (n == 1) {
                Temp[] args = Temp.makeTemps(n);
                Tail t = Prim.rem.withArgs(args[0], mod);
                return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
              }
              // TODO: add support for n>1, mod not a power of two ...
            }
            return null;
          }
        });

    // primRelaxIx n m :: Ix n -> Ix m
    generators.put(
        "primRelaxIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger n = ts[0].getIxArg(); // Smaller index modulus
            BigInteger m = ts[1].getIxArg(); // Larger index modulus
            if (n != null && m != null && n.compareTo(m) <= 0) {
              // We implement relaxIx as the identity function (Ix n and Ix m values are both
              // represented as Word values).
              // TODO: the type checker will infer a *polymorphic* type for this definition, which
              // will trip up the LLVM
              // code generator (although that will likely not happen in practice because this
              // definition should be inlined
              // by the optimizer).
              return new Return().makeUnaryFuncClosure(pos, 1);
            }
            return null;
          }
        });
  }

  /**
   * A general method for generating comparisons on Ix values. Because Ix values are represented by
   * a single Word, we can implement each of these using the corresponding (unsigned) comparison on
   * Word.
   */
  static void genIxCompare(String ref, final PrimRelOp cmp) {
    // primIx... m :: Ix m -> Ix m -> Bool
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger m = ts[0].getIxArg(); // Index upper bound
            if (m != null) {
              return new PrimCall(cmp).makeBinaryFuncClosure(pos, 1, 1);
            }
            return null;
          }
        });
  }

  static {
    genIxCompare("primIxEq", Prim.eq);
    genIxCompare("primIxNe", Prim.neq);
    genIxCompare("primIxLt", Prim.ult);
    genIxCompare("primIxLe", Prim.ule);
    genIxCompare("primIxGt", Prim.ugt);
    genIxCompare("primIxGe", Prim.uge);
  }

  static {

    // primBitNot w :: Bit w -> Bit w
    generators.put(
        "primBitNot",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new Return().makeUnaryFuncClosure(pos, 0);

                case 1:
                  return new PrimCall(Prim.bnot).makeUnaryFuncClosure(pos, 1);

                default:
                  {
                    int n = Type.numWords(width);
                    Temp[] vs = Temp.makeTemps(n); // variables returned from block
                    Temp[] ws = Temp.makeTemps(n); // arguments to closure
                    Code code = new Done(new Return(Temp.clone(vs)));
                    int rem = width % Type.WORDSIZE; // nonzero => unused bits in most sig word

                    // Use Prim.xor on the most significant word if not all bits are used:
                    if (rem != 0) {
                      Temp v = vs[--n];
                      code =
                          new Bind(v, Prim.xor.withArgs(vs[n] = new Temp(), (1 << rem) - 1), code);
                    }

                    // Use Prim.not on any remaining words:
                    while (n > 0) {
                      Temp v = vs[--n];
                      code = new Bind(v, Prim.not.withArgs(vs[n] = new Temp()), code);
                    }

                    return new BlockCall(new Block(pos, vs, code))
                        .makeUnaryFuncClosure(pos, vs.length);
                  }
              }
            }
            return null;
          }
        });
  }

  /**
   * A general method for generating implementations for BITWISE binary operations (and, or, xor)
   * where no special masking is required on the most significant word. The primitive p is used for
   * the general case of bit vectors with width>1, while the primitive pf is used for the special
   * case width==1 that uses a Flag representation rather than Words.
   */
  static void genBitwiseBinOp(String ref, final PrimBinOp p, final PrimBinFOp pf) {
    // primBitRef w :: Bit w -> Bit w -> Bit w
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new Return().makeBinaryFuncClosure(pos, 0, 0);

                case 1:
                  return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

                default:
                  {
                    // Block: b[a0,...b0,...] = c0 <- p((a0,b0)); ...; return [c0,...]
                    int n = Type.numWords(width);
                    Temp[] as = Temp.makeTemps(n); // inputs
                    Temp[] bs = Temp.makeTemps(n);
                    Temp[] cs = Temp.makeTemps(n); // output
                    Code code = new Done(new Return(cs));
                    for (int i = n; 0 < i--; ) {
                      code = new Bind(cs[i], p.withArgs(as[i], bs[i]), code);
                    }
                    return new BlockCall(new Block(pos, Temp.append(as, bs), code))
                        .makeBinaryFuncClosure(pos, n, n);
                  }
              }
            }
            return null;
          }
        });
  }

  static {
    genBitwiseBinOp("primBitAnd", Prim.and, Prim.band);
    genBitwiseBinOp("primBitOr", Prim.or, Prim.bor);
    genBitwiseBinOp("primBitXor", Prim.xor, Prim.bxor);
  }

  static {

    // primBitNegate w :: Bit w -> Bit w
    generators.put(
        "primBitNegate",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new Return().makeUnaryFuncClosure(pos, 0);

                case 1: // Negate is the identity function on Bit 1!
                  return new Return().makeUnaryFuncClosure(pos, 1);

                default:
                  {
                    int n = Type.numWords(width);
                    if (n == 1) {
                      Temp[] args = Temp.makeTemps(1);
                      Code code = maskTail(Prim.neg.withArgs(args), width);
                      return new BlockCall(new Block(pos, args, code)).makeUnaryFuncClosure(pos, 1);
                    }
                  }
              }
            }
            return null;
          }
        });
  }

  private static Code maskTail(Tail t, int width) {
    int rem = width % Type.WORDSIZE; // Determine whether masking is required
    if (rem == 0) {
      return new Done(t);
    } else {
      Temp c = new Temp();
      return new Bind(c, t, new Done(Prim.and.withArgs(c, (1 << rem) - 1)));
    }
  }

  /**
   * A general method for generating implementations for ARITHMETIC binary operations (add, sub,
   * mul), where masking of the most significant word may be required to match the requested length.
   * TODO: For the time being, these implementations only work for 0 <= width <= Type.WORDSIZE. The
   * algorithms for these operations on multi-word values are more complex and more varied, so they
   * will require a more sophisticated approach.
   */
  static void genArithBinOp(String ref, final PrimBinOp p, final PrimBinFOp pf) {
    // primBitRef w :: Bit w -> Bit w -> Bit w
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new Return().makeBinaryFuncClosure(pos, 0, 0);

                case 1:
                  return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

                default:
                  {
                    int n = Type.numWords(width);
                    if (n == 1) {
                      Temp[] args = Temp.makeTemps(2);
                      Code code = maskTail(p.withArgs(args), width);
                      return new BlockCall(new Block(pos, args, code))
                          .makeBinaryFuncClosure(pos, 1, 1);
                    }
                  }
              }
            }
            return null;
          }
        });
  }

  static {
    genArithBinOp("primBitPlus", Prim.add, Prim.bxor);
    genArithBinOp("primBitMinus", Prim.sub, Prim.bxor);
    genArithBinOp("primBitTimes", Prim.mul, Prim.band);
  }

  /**
   * A general method for generating implementations for EQUALITY comparisons on Bit vector values.
   * The primitive pf is used for the single bit case and the block bz provides the result for the 0
   * width case. For the general case, all but the least significant words are compared using
   * Prim.eq, branching to Block bearly if the equality test fails (so bearly should be returnFalse
   * for ==, or returnTrue for /=). The comparison on the least significant word, and the result of
   * the whole computation if all other parts were equal, is determined using the specified test
   * primitive (Prim.eq for == or Prim.neq for /=).
   */
  static void genEqBinOp(
      String ref, final PrimRelOp test, final Block bearly, final PrimBinFOp pf, final Block bz) {
    // primBitRef w :: Bit w -> Bit w -> Flag
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new BlockCall(bz).makeBinaryFuncClosure(pos, 0, 0);

                case 1:
                  return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

                default:
                  {
                    int n = Type.numWords(width);
                    return new BlockCall(bitEqBlock(pos, n, test, bearly))
                        .makeBinaryFuncClosure(pos, n, n);
                  }
              }
            }
            return null;
          }
        });
  }

  /**
   * Generate code for an equality test on two bit vectors, each represented by n words. Assumes
   * n>=1. See genEqBinOp for description of test and bearly primitives.
   */
  private static Block bitEqBlock(Position pos, int n, PrimRelOp test, Block bearly) {
    Temp[] args = Temp.makeTemps(2 * n); // Arguments for this block (two bit vectors of length n)
    Code code;
    if (n == 1) {
      code = new Done(test.withArgs(args));
    } else {
      Temp v = new Temp();
      code =
          new Bind(
              v,
              Prim.eq.withArgs(args[n - 1], args[2 * n - 1]),
              new If(
                  v,
                  new BlockCall(bitEqBlock(pos, n - 1, test, bearly), dropMSWords(n, args)),
                  new BlockCall(bearly, Atom.noAtoms)));
    }
    return new Block(pos, args, code);
  }

  /**
   * Given the arguments for a function with two n-word bit vector inputs, return a new list of
   * arguments that omits the most significant word from each of the two inputs.
   */
  private static Temp[] dropMSWords(int n, Temp[] args) {
    Temp[] rargs = new Temp[2 * (n - 1)]; // Arguments for recursive call: least significant words
    for (int i = 0; i < n - 1; i++) { // from each of the two inputs
      rargs[i] = args[i];
      rargs[i + n - 1] = args[i + n];
    }
    return rargs;
  }

  static {
    genEqBinOp("primBitEq", Prim.eq, Block.returnFalse, Prim.beq, Block.returnTrue);
    genEqBinOp("primBitNe", Prim.neq, Block.returnTrue, Prim.bxor, Block.returnFalse);
  }

  /**
   * A general method for generating implementations for lexicographic orderings on Bit vector
   * values. See bitLexCompBlock() for explanation of lsw and slsw arguments. The primitive pf is
   * used in the special case for bit vectors of width 1, and the bz block is used to generate code
   * for width==0.
   */
  static void genRelBinOp(
      String ref, final PrimRelOp lsw, final PrimRelOp slsw, final PrimBinFOp pf, final Block bz) {
    // primBit... w ... :: Bit w -> Bit w -> Flag
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              switch (width) {
                case 0:
                  return new BlockCall(bz).makeBinaryFuncClosure(pos, 0, 0);

                case 1:
                  return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

                default:
                  {
                    int n = Type.numWords(width);
                    return new BlockCall(bitLexCompBlock(pos, n, lsw, slsw))
                        .makeBinaryFuncClosure(pos, n, n);
                  }
              }
            }
            return null;
          }
        });
  }

  /**
   * Generate code for a lexicographic comparison on two bit vectors, each represented by n words.
   * Assumes n>=1. The lsw comparison (which may or may not be strict; i.e., including equality) is
   * used on the least significant word, while the slsw comparison (which should be the strict
   * version of lsw, not including equality) is used on all other words.
   */
  private static Block bitLexCompBlock(Position pos, int n, PrimRelOp lsw, PrimRelOp slsw) {
    Temp[] args = Temp.makeTemps(2 * n);
    Code code;
    if (n == 1) { // For least significant word:
      code = new Done(lsw.withArgs(args));
    } else {
      Temp v = new Temp(); // For multiple words, values, start by comparing most significant words:
      code =
          new Bind(
              v,
              slsw.withArgs(args[n - 1], args[2 * n - 1]),
              new If(
                  v,
                  new BlockCall(Block.returnTrue, Atom.noAtoms),
                  new BlockCall(bitLexCompBlock1(pos, n, lsw, slsw), args)));
    }
    return new Block(pos, args, code);
  }

  /**
   * Worker function for bitLexCompBlock: builds a block to be executed when the slsw test for the
   * most significant words has failed. In this case, we may still continue to less significant
   * words if the most significant words are equal.
   */
  private static Block bitLexCompBlock1(Position pos, int n, PrimRelOp lsw, PrimRelOp slsw) {
    Temp[] args = Temp.makeTemps(2 * n);
    Temp v = new Temp();
    return new Block(
        pos,
        args,
        new Bind(
            v,
            Prim.eq.withArgs(args[n - 1], args[2 * n - 1]),
            new If(
                v,
                new BlockCall(bitLexCompBlock(pos, n - 1, lsw, slsw), dropMSWords(n, args)),
                new BlockCall(Block.returnFalse, Atom.noAtoms))));
  }

  static {
    genRelBinOp("primBitGt", Prim.ugt, Prim.ugt, Prim.bgt, Block.returnFalse);
    genRelBinOp("primBitGe", Prim.uge, Prim.ugt, Prim.bge, Block.returnTrue);
    genRelBinOp("primBitLt", Prim.ult, Prim.ult, Prim.blt, Block.returnFalse);
    genRelBinOp("primBitLe", Prim.ule, Prim.ult, Prim.ble, Block.returnTrue);
  }

  /**
   * Provides a framework for generating implementations of functions that take (at least) an Ix w
   * argument and produce a Bit w result; this includes BitManip operations like bitBit, setBit, and
   * testBit, as well as shift operations like bitShiftL. In each case, the implementation consists
   * of a set of blocks that perform a (runtime) binary search on the index value to identify a
   * specific word position within the representation of a Bit w value. Operator specific logic is
   * then added to the leaves of the resulting decision trees.
   */
  protected abstract static class BitPosGenerator extends Generator {

    /** Default constructor. */
    protected BitPosGenerator(int needs) {
      super(needs);
    }

    /**
     * Construct a decision tree to find the appropriate position within the representation of a Bit
     * vector. In the generated code: n is the number of words needed to represent a Bit vector of
     * the appropriate width; lo and hi specify the range of words that the generated block should
     * cover, using a binary search if necessary to reduce the range to a single word; numArgs is
     * the number of arguments that must be passed at each step, not including the bit index
     * argument, which will be added on the end. (In practice, numArgs will either be 0 if we are
     * constructing a new Bit vector (bitBit), or n if we are modifying or testing an existing Bit
     * vector (bitSet, bitClear, bitFlip, bitTest)).
     */
    protected Block decisionTree(Position pos, int width, int n, int lo, int hi, int numArgs) {
      // invariant:  0 <= lo <= hi < n
      Temp[] vs = Temp.makeTemps(numArgs + 1); // arguments for words, plus bit index argument
      if (hi > lo) {
        int mid = 1 + ((lo + hi) / 2);
        Temp v = new Temp();
        return new Block(
            pos,
            vs,
            new Bind(
                v,
                Prim.ult.withArgs(vs[numArgs], mid * Type.WORDSIZE),
                new If(
                    v,
                    new BlockCall(decisionTree(pos, width, n, lo, mid - 1, numArgs), vs),
                    new BlockCall(decisionTree(pos, width, n, mid, hi, numArgs), vs))));
      } else {
        return decisionLeaf(pos, vs, width, n, lo, numArgs);
      }
    }

    /**
     * This method generates the code to be executed when we have identified which of the n words in
     * the relevant Bit vector representation (i.e., the one at offset lo) contains the bit
     * specified by the index parameter (the last argument of vs).
     */
    abstract Block decisionLeaf(Position pos, Temp[] vs, int width, int n, int lo, int numArgs);
  }

  protected abstract static class BitmanipGenerator extends BitPosGenerator {

    /** Default constructor. */
    protected BitmanipGenerator(int needs) {
      super(needs);
    }

    /**
     * For each of the BitManip operators, once we have found the word (lo) that the index is
     * pointing to, then we can calculate a single bit mask that determines which specific bit will
     * be accessed/modified. Once this mask is calculated (in m below), a different method is
     * required to construct the final result for each of the BitManip operators.
     */
    Block decisionLeaf(Position pos, Temp[] vs, int width, int n, int lo, int numArgs) {
      Temp p = new Temp();
      Temp m = new Temp();
      return new Block(
          pos,
          vs, // b[v0,...,i]
          new Bind(
              p,
              Prim.sub.withArgs(vs[numArgs], lo * Type.WORDSIZE), //  = p <- sub((i, offset))
              new Bind(
                  m,
                  Prim.shl.withArgs(1, p), //    m <- shl((1, p))
                  makeResult(vs, m, n, lo)))); //    ... calc result ...
    }

    /**
     * Construct the final result of the computation in a code sequence where: vs is the list of
     * arguments to the the enclosing block; m is a previously calculated mask for the relevant bit
     * within the selected word; n is the number of words in the representation for the Bit vector;
     * and lo is the index of the word containing the relevant bit.
     */
    abstract Code makeResult(Temp[] vs, Atom mask, int n, int lo);
  }

  protected abstract static class ConstructBitmanipGenerator extends BitmanipGenerator {

    /** Default constructor. */
    protected ConstructBitmanipGenerator(int needs) {
      super(needs);
    }

    Tail generate(Position pos, Type[] ts) {
      BigInteger w = ts[0].getBitArg(); // Width of bit vector
      if (w != null) {
        int width = w.intValue();
        int n = Type.numWords(width);
        return new BlockCall(decisionTree(pos, width, n, 0, n - 1, 0)).makeUnaryFuncClosure(pos, 1);
      }
      return null;
    }
  }

  protected abstract static class ConsumeBitmanipGenerator extends BitmanipGenerator {

    /** Default constructor. */
    protected ConsumeBitmanipGenerator(int needs) {
      super(needs);
    }

    Tail generate(Position pos, Type[] ts) {
      BigInteger w = ts[0].getBitArg(); // Width of bit vector
      if (w != null) {
        int width = w.intValue();
        int n = Type.numWords(width);
        return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
            .makeBinaryFuncClosure(pos, n, 1);
      }
      return null;
    }

    protected static Atom[] reuseOtherWords(Temp[] vs, Atom w, int n, int lo) {
      Atom[] as = new Atom[n];
      for (int i = 0; i < n; i++) {
        as[i] = (i == lo) ? w : vs[i];
      }
      return as;
    }
  }

  static {
    generators.put(
        "primBitBit",
        new ConstructBitmanipGenerator(1) {
          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Atom[] as = new Atom[n];
            for (int i = 0; i < n; i++) {
              as[i] = (i == lo) ? mask : IntConst.Zero;
            }
            return new Done(new Return(as));
          }
        });

    generators.put(
        "primBitSetBit",
        new ConsumeBitmanipGenerator(1) {
          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Temp w = new Temp();
            return new Bind(
                w,
                Prim.or.withArgs(mask, vs[lo]),
                new Done(new Return(reuseOtherWords(vs, w, n, lo))));
          }
        });

    generators.put(
        "primBitClearBit",
        new ConsumeBitmanipGenerator(1) {
          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Temp w = new Temp();
            Temp x = new Temp();
            return new Bind(
                x,
                Prim.not.withArgs(mask),
                new Bind(
                    w,
                    Prim.and.withArgs(x, vs[lo]),
                    new Done(new Return(reuseOtherWords(vs, w, n, lo)))));
          }
        });

    generators.put(
        "primBitFlipBit",
        new ConsumeBitmanipGenerator(1) {
          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Temp w = new Temp();
            return new Bind(
                w,
                Prim.xor.withArgs(mask, vs[lo]),
                new Done(new Return(reuseOtherWords(vs, w, n, lo))));
          }
        });

    generators.put(
        "primBitTestBit",
        new ConsumeBitmanipGenerator(1) {
          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Temp w = new Temp();
            return new Bind(
                w, Prim.and.withArgs(mask, vs[lo]), new Done(Prim.neq.withArgs(w, IntConst.Zero)));
          }
        });

    generators.put(
        "primBitBitSize",
        new Generator(1) { // :: Bit w -> Ix w
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Bit vector width
            if (w != null && w.compareTo(BigInteger.ZERO) > 0) {
              int width = w.intValue();
              int n = Type.numWords(width);
              Tail t = new Return(new IntConst(width - 1));
              // TODO: The Temp.makeTemps(n) call in the following creates the proxy argument of
              // type Bit w that is required as an input
              // for this function (to avoid ambiguity).  Because it is not actually used, however,
              // it will result in a polymorphic
              // definition in post-specialization code, which may break subsequent attempts to
              // generate code from monomorphic MIL code
              // ... unless this definition is optimized away (which, it should be ... assuming that
              // the optimizer is invoked ...)
              ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(n), t);
              return new ClosAlloc(k).withArgs();
            }
            return null;
          }
        });
  }

  static {

    // primBitShiftL w :: Bit w -> Ix w -> Bit w,  for w>1
    generators.put(
        "primBitShiftL",
        new BitPosGenerator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              if (width > 1) { // TODO: add support for width 0 and width 1?
                int n = Type.numWords(width);
                return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
                    .makeBinaryFuncClosure(pos, n, 1);
              }
            }
            return null;
          }

          Block decisionLeaf(Position pos, Temp[] vs, int width, int n, int lo, int numArgs) {
            Temp v = new Temp();
            if (lo == 0) {
              return shiftLeftOffsetBlock(pos, vs, width, n, lo);
            } else {
              return new Block(
                  pos,
                  vs,
                  new Bind(
                      v,
                      Prim.eq.withArgs(vs[n], lo * Type.WORDSIZE),
                      new If(
                          v,
                          new BlockCall(shiftLeftMultipleBlock(pos, width, n, lo), vs),
                          new BlockCall(
                              shiftLeftOffsetBlock(pos, Temp.makeTemps(n + 1), width, n, lo),
                              vs))));
            }
          }

          /**
           * Build a block to handle the case in a shift left where the shift is a multiple of the
           * WORDSIZE.
           */
          private Block shiftLeftMultipleBlock(Position pos, int width, int n, int lo) {
            Temp[] vs = Temp.makeTemps(n + 1); // [v0,...,shift]
            Atom[] as = new Atom[n];
            for (int i = 0; i < lo; i++) { // words below lo in the result are zero
              as[i] = IntConst.Zero;
            }
            for (int i = lo; i < n; i++) { // words at or above lo are shifted up from lower indices
              as[i] = vs[i - lo];
            }
            Code code = new Done(new Return(as)); // Mask most significant word, if necessary
            int rem = width % Type.WORDSIZE;
            if (rem != 0) {
              Temp t = new Temp();
              code = new Bind(t, Prim.and.withArgs(as[n - 1], (1 << rem) - 1), code);
              as[n - 1] = t;
            }
            return new Block(pos, vs, code);
          }

          /**
           * Build a block to handle the case in a shift left where the shift is offset, NOT a
           * multiple of the WORDSIZE. Also provides code for the case of a large shift that reaches
           * in to the most significant word.
           */
          private Block shiftLeftOffsetBlock(Position pos, Temp[] vs, int width, int n, int lo) {
            Atom[] as = new Atom[n];
            Temp offs = new Temp(); // holds offset within word
            Temp comp = new Temp(); // holds complement of offset (comp + offs == WORDSIZE)
            Code code = new Done(new Return(as)); // Build up code in reverse ...

            // Zero out least significant words of result:
            for (int i = 0; i < lo; i++) {
              as[i] = IntConst.Zero;
            }

            // Create new temporaries for the remaining words:
            Temp[] ts = (n > lo) ? Temp.makeTemps(n - lo) : null;
            for (int i = lo; i < n; i++) {
              as[i] = ts[i - lo];
            }

            // Add code to mask most significant word, if necessary:
            // TODO: The implementation of this method is somewhat contorted by the need to have
            // initialized the ts
            // and as arrays by the time we get to this point; is there a cleaner way to do this?
            int rem = width % Type.WORDSIZE;
            if (rem != 0) {
              Temp t = new Temp();
              code = new Bind(t, Prim.and.withArgs(ts[n - lo - 1], (1 << rem) - 1), code);
              as[n - 1] = t;
            }

            // Set outputs that combine data from adjacent words in the input:
            for (int i = lo + 1; i < n; i++) {
              Temp p = new Temp();
              Temp q = new Temp();
              code =
                  new Bind(
                      p,
                      Prim.shl.withArgs(vs[i - lo], offs),
                      new Bind(
                          q,
                          Prim.lshr.withArgs(vs[i - lo - 1], comp),
                          new Bind(ts[i - lo], Prim.or.withArgs(p, q), code)));
            }

            // Construct block, including code to calculate offs, comp, and lsw of output:
            return new Block(
                pos,
                vs,
                new Bind(
                    offs,
                    Prim.sub.withArgs(vs[n], lo * Type.WORDSIZE),
                    new Bind(
                        comp,
                        Prim.sub.withArgs(Type.WORDSIZE, offs),
                        new Bind(ts[0], Prim.shl.withArgs(vs[0], offs), code))));
          }
        });

    // primBitShiftRu w :: Bit w -> Ix w -> Bit w,  for w>1
    generators.put(
        "primBitShiftRu",
        new BitPosGenerator(1) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger w = ts[0].getBitArg(); // Width of bit vector
            if (w != null) {
              int width = w.intValue();
              if (width > 1) { // TODO: add support for width 0 and width 1?
                int n = Type.numWords(width);
                return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
                    .makeBinaryFuncClosure(pos, n, 1);
              }
            }
            return null;
          }

          Block decisionLeaf(Position pos, Temp[] vs, int width, int n, int lo, int numArgs) {
            if (lo + 1 == n) {
              return shiftRightOffsetBlock(pos, vs, n, lo);
            } else {
              Temp v = new Temp();
              return new Block(
                  pos,
                  vs,
                  new Bind(
                      v,
                      Prim.eq.withArgs(vs[n], lo * Type.WORDSIZE),
                      new If(
                          v,
                          new BlockCall(shiftRightMultipleBlock(pos, n, lo), vs),
                          new BlockCall(
                              shiftRightOffsetBlock(pos, Temp.makeTemps(n + 1), n, lo), vs))));
            }
          }

          /**
           * Build a block to handle the case in a shift right where the shift is a multiple of the
           * WORDSIZE.
           */
          private Block shiftRightMultipleBlock(Position pos, int n, int lo) {
            Temp[] vs = Temp.makeTemps(n + 1); // [v0,...,shift]
            Atom[] as = new Atom[n];
            int i = 0;
            for (; i + lo < n; i++) {
              as[i] = vs[i + lo];
            }
            for (; i < n; i++) {
              as[i] = IntConst.Zero;
            }
            return new Block(pos, vs, new Done(new Return(as)));
          }

          /**
           * Build a block to handle the case in a shift right where the shift is offset, NOT a
           * multiple of the WORDSIZE. Also provides code for the case of a large shift that reaches
           * in to the most significant word.
           */
          private Block shiftRightOffsetBlock(Position pos, Temp[] vs, int n, int lo) {
            Atom[] as = new Atom[n];
            Temp offs = new Temp(); // holds offset within word
            Temp comp = new Temp(); // holds complement of offset (comp + offs == WORDSIZE)
            Code code = new Done(new Return(as));
            int i = 0;
            // Set words that blend data from two sources:
            for (; i + lo < n - 1; i++) {
              Temp p = new Temp();
              Temp q = new Temp();
              Temp r = new Temp();
              code =
                  new Bind(
                      p,
                      Prim.shl.withArgs(vs[i + lo + 1], comp),
                      new Bind(
                          q,
                          Prim.lshr.withArgs(vs[i + lo], offs),
                          new Bind(r, Prim.or.withArgs(p, q), code)));
              as[i] = r;
            }
            // Set most significant non-zero word:
            Temp r = new Temp();
            code = new Bind(r, Prim.lshr.withArgs(vs[n - 1], offs), code);
            as[i++] = r;

            // Zero any remaining parts of result:
            for (; i < n; i++) {
              as[i] = IntConst.Zero;
            }
            return new Block(
                pos,
                vs,
                new Bind(
                    offs,
                    Prim.sub.withArgs(vs[n], lo * Type.WORDSIZE),
                    new Bind(comp, Prim.sub.withArgs(Type.WORDSIZE, offs), code)));
          }
        });
  }

  static {

    // (@) n a :: Ref (Array n a) -> Ix n -> Ref a
    generators.put(
        "@",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger n = ts[0].getNat(); // Alignment of structure
            Type bs = ts[1].byteSize(null); // Size of array elements
            BigInteger s;
            if (n != null && bs != null && (s = bs.getNat()) != null) {
              int size = s.intValue(); // TODO: check that size is in a reasonable range?
              Temp[] ri = Temp.makeTemps(2);
              Temp v = new Temp();
              Block b =
                  new Block(
                      pos,
                      ri, // b[r,i]
                      new Bind(
                          v,
                          Prim.mul.withArgs(ri[1], size), //  = v <- mul((i, size))
                          new Done(Prim.add.withArgs(ri[0], v)))); //    add((r, v))
              return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
            }
            return null;
          }
        });
  }

  static {

    // primInitArray n a :: (Ix n -> Init a) -> Init (Array n a)
    generators.put(
        "primInitArray",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger n = ts[0].getIxArg(); // Array length
            Type bs = ts[1].byteSize(null); // Size of array elements
            BigInteger s;
            if (n != null && bs != null && (s = bs.getNat()) != null) {
              int len =
                  n.intValue(); // TODO: calls to intValue produce wrong results for large n, s
              int size = s.intValue();
              return (len <= 4) ? initArrayUnroll(pos, len, size) : initArrayLoop(pos, len, size);
            }
            return null;
          }
        });
  }

  /**
   * Generate code for a loop-based implementation of initArray. The structure of the generated code
   * is as follows: (len=array length, size=element size)
   *
   * <p>initArray <- k0{} k0{} f = k1{f} k1{f} r = loop[f, r, 0] loop[f, r, j] = v <- ult((j, len))
   * if v then step[f, r, j] else done[] step[f, r, i] = g <- f @ i x <- g @ r j <- add((i, i)) s <-
   * add((r, size)) loop[f, s, j] done[] = Unit()
   *
   * <p>(Assumes implementation: Init a = [Ref a] ->> [Unit])
   */
  private static Tail initArrayLoop(Position pos, int len, int size) {
    Block done = new Block(pos, Temp.noTemps, new Done(Cfun.Unit.withArgs())); // bdone[] = Unit()
    Temp[] fsj = Temp.makeTemps(3);
    Block loop = new Block(pos, fsj, null); // bloop[f, s, j] = ...
    Temp[] fri = Temp.makeTemps(3);
    Block step = new Block(pos, fri, null); // bstep[f, r, j] = ...

    Temp v = new Temp();
    loop.setCode(
        new Bind(
            v,
            Prim.ult.withArgs(fsj[2], len),
            new If(v, new BlockCall(step, Temp.clone(fsj)), new BlockCall(done, Atom.noAtoms))));

    Temp g = new Temp();
    Temp x = new Temp();
    Temp j = new Temp();
    Temp s = new Temp();
    step.setCode(
        new Bind(
            g,
            new Enter(fri[0], fri[2]),
            new Bind(
                x,
                new Enter(g, fri[1]),
                new Bind(
                    j,
                    Prim.add.withArgs(fri[2], 1),
                    new Bind(
                        s,
                        Prim.add.withArgs(fri[1], size),
                        new Done(new BlockCall(loop, new Atom[] {fri[0], s, j})))))));

    Temp[] f = Temp.makeTemps(1);
    Temp[] r = Temp.makeTemps(1);
    ClosureDefn k1 =
        new ClosureDefn(pos, f, r, new BlockCall(loop, new Atom[] {f[0], r[0], IntConst.Zero}));
    return new ClosAlloc(k1).makeUnaryFuncClosure(pos, 1);
  }

  /**
   * Generate code for an implementation of initArray that uses a separate section of code for every
   * array element, essentially unrolling the loop that is produced by initArrayLoop. This will
   * likely produce a lot of code unless the array length is small. The structure of the generated
   * code is as follows:
   *
   * <p>initArray <- k0{} k0{} f = k1{f} k1{f} r = work[f, r] work[f, r] = g0 <- f @ 0 x0 <- f0 @ r
   * //... r1 <- add((r0, size)) g1 <- f @ 1 x1 <- g1 @ r1 //... Unit()
   */
  private static Tail initArrayUnroll(Position pos, int len, int size) {
    Code code = new Done(Cfun.Unit.withArgs());
    Temp f = new Temp();
    Temp r = new Temp();
    if (len > 0) {
      int i = len - 1;
      for (; ; ) {
        Temp g = new Temp();
        code =
            new Bind(g, new Enter(f, new IntConst(i)), new Bind(new Temp(), new Enter(g, r), code));
        if (i > 0) {
          Temp r1 = new Temp();
          code = new Bind(r, Prim.add.withArgs(r1, size), code);
          r = r1;
        } else {
          break;
        }
      }
    }
    Block work = new Block(pos, new Temp[] {f, r}, code);
    return new BlockCall(work).makeBinaryFuncClosure(pos, 1, 1);
  }

  static {

    // structSelect m s f n :: ARef m s -> #f -> ARef n t
    generators.put(
        "structSelect",
        new Generator(4) {
          Tail generate(Position pos, Type[] ts) {
            BigInteger m = ts[0].getNat(); // Alignment of structure
            StructName sn = ts[1].structName(); // Structure
            String lab = ts[2].getLabel(); // Field label
            BigInteger n = ts[3].getNat(); // Alignment of field
            if (m != null && sn != null && lab != null && n != null) {
              // TODO: For now, we ignore the alignment values, trusting that the values passed in
              // by the front
              // end will be valid ... but it probably would not be a bad idea to check them here
              // too.
              StructField[] fields = sn.getFields();
              for (int i = 0; i < fields.length; i++) {
                if (fields[i].answersTo(lab)) {
                  Temp[] vs = Temp.makeTemps(1);
                  Tail tail = Prim.add.withArgs(vs[0], fields[i].getOffset());
                  ClosureDefn k = new ClosureDefn(pos, vs, Temp.makeTemps(1), tail);
                  return new ClosAlloc(k).makeUnaryFuncClosure(pos, 1);
                }
              }
            }
            return null;
          }
        });
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    /* Processing of External definitions was completed during the first pass. */
  }

  /** Add this exported definition to the specified MIL environment. */
  void addExport(MILEnv exports) {
    exports.addTop(id, new TopExt(this));
  }

  /**
   * Find the arguments that are needed to enter this definition. The arguments for each block or
   * closure are computed the first time that we visit the definition, but are returned directly for
   * each subsequent call.
   */
  Temp[] addArgs() throws Failure {
    return null;
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* Nothing to do here */
  }
}
