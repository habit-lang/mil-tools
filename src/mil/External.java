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

  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out, boolean isEntrypoint) {
    if (isEntrypoint) {
      out.print("export ");
    }
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
   * Type check the body of this definition, but reporting rather than throwing an exception error
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
   * Compute a summary for this definition (if it is a block, top-level, or closure) and then look
   * for a previously encountered item with the same code in the given table. Return true if a
   * duplicate was found.
   */
  boolean summarizeDefns(Blocks[] blocks, TopLevels[] topLevels, ClosureDefns[] closures) {
    return false;
  }

  void eliminateDuplicates() {
    /* Nothing to do here */
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
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

  External(External e, int num) {
    this(e.pos, mkid(e.id, num), e.declared, e.ref, e.ts);
  }

  /** Handle specialization of Externals. */
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
    if (ts != null) {
      Type[] nts = new Type[ts.length];
      for (int i = 0; i < ts.length; i++) {
        nts[i] = ts[i].canonType(spec);
      }
      ts = nts;
    }
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    Type t = declared.isMonomorphic();
    if (t != null) {
      External e = spec.specializedExternal(this, t);
      e.id = this.id; // use the same name as in the original program
      return e;
    }
    throw new PolymorphicEntrypointFailure("external", this);
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
  }

  void bitdataRewrite(BitdataMap m) {
    /* Nothing to do here */
  }

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    declared = declared.canonType(set);
    debug.Log.println("Determining representation for external " + id + " :: " + declared);
    try {
      repImplement(handler, declared.repCalc(), set);
    } catch (Failure f) {
      handler.report(f);
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
    abstract Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException;
  }

  /**
   * Stores a mapping from String references to generators for external function implementations.
   */
  private static HashMap<String, Generator> generators = new HashMap();

  /**
   * Use the ref and ts fields to determine if we can generate an implementation, post
   * representation transformation, for an external primitive, or if the external should be replaced
   * with the use of an external primitive.
   */
  private void repImplement(Handler handler, Type[] reps, RepTypeSet set) throws Failure {
    if (ref == null) { // If there is no reference name given,
      generatePrim(id, reps); // then consider reimplementation using a primitive.
      return;
    } else if (ts != null) {
      Generator gen = generators.get(ref); // Otherwise, look for a generator ...
      if (gen != null) {
        if (ts.length < gen.needs) { // ... with enough arguments
          throw new Failure(
              pos, "Generator for " + ref + " needs at least " + gen.needs + " arguments");
        }
        Tail t; // ... and try to produce an implementation
        try {
          t = gen.generate(pos, ts, set);
        } catch (GeneratorException e) {
          throw new Failure(pos, "No generated implementation: " + e.getReason());
        }
        TopLhs[] lhs; // Generate a suitable left hand side
        if (reps == null) { // No change in representation
          lhs = new TopLhs[] {new TopLhs(id)}; // ==> single left hand side
          lhs[0].setDeclared(declared);
        } else { // Change in representation
          lhs = new TopLhs[reps.length]; // ==> may require multiple left hand sides
          for (int i = 0; i < reps.length; i++) {
            lhs[i] = new TopLhs(mkid(id, i));
            lhs[i].setDeclared(reps[i]);
          }
        }
        // TODO: it seems inconsistent to use a HashMap for topLevelRepMap, while using a field here
        // ...
        impl =
            new TopLevel(
                pos, lhs, t); // Make new top level to use as the replacement for this External
        impl.setIsEntrypoint(isEntrypoint);
        debug.Log.println("Generated new top level definition for " + impl);
        return;
      }
      if (ts.length > 0) {
        throw new Failure(pos, "No generator for " + ref);
      }
    }
    generatePrim(ref, reps);
  }

  private void generatePrim(String id, Type[] reps) throws Failure {
    if (reps == null) {
      generatePrim(id, declared);
    } else if (reps.length == 1) {
      generatePrim(id, reps[0]);
    } else {
      TopLhs[] lhs = new TopLhs[reps.length];
      Atom[] rhs = new Atom[reps.length];
      for (int i = 0; i < reps.length; i++) {
        External ext = new External(pos, mkid(id, i), reps[i], null, null);
        ext.setIsEntrypoint(isEntrypoint);
        rhs[i] = new TopExt(ext);
        lhs[i] = id.equals(this.id) ? new TopLhs() : new TopLhs(mkid(id, i));
      }
      impl = new TopLevel(pos, lhs, new Return(rhs));
      impl.setIsEntrypoint(isEntrypoint && !id.equals(this.id));
    }
  }

  private void generatePrim(String id, Scheme declared) throws Failure {
    Tail t = declared.generatePrim(pos, id);
    if (t != null) {
      TopLhs lhs = new TopLhs(this.id);
      lhs.setDeclared(declared);
      impl = new TopLevel(pos, new TopLhs[] {lhs}, t);
      if (isEntrypoint && id.equals(this.id)) { // test delayed until impl has been initialized
        throw new Failure(
            pos,
            "External "
                + id
                + " is implemented by a primitive of the same"
                + " name so cannot be declared as an entrypoint.");
      }
    } else {
      External ext = new External(pos, id, declared, null, null);
      if (id.equals(this.id)) {
        impl = ext;
      } else {
        TopLhs lhs = new TopLhs(this.id);
        lhs.setDeclared(declared);
        impl = new TopLevel(pos, new TopLhs[] {lhs}, new Return(new TopExt(ext)));
      }
    }
    impl.setIsEntrypoint(isEntrypoint);
  }

  /**
   * Rewrite this definition, replacing TopLevels that introduce curried function values with
   * corresponding uncurried blocks. No changes are made to other forms of definition.
   */
  Defn makeEntryBlock() {
    return (impl == null) ? this : impl.makeEntryBlock();
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    /* Processing of External definitions was completed during the first pass. */
  }

  /** Flag to indicate whether bitdata representations (e.g., for Maybe (Ix 15)) are in use. */
  private static boolean bitdataRepresentations = false;

  /** Set the bitdataRepresentations flag; intended to be called in the driver as appropriate. */
  public static void setBitdataRepresentations() {
    bitdataRepresentations = true;
  }

  private static void validBitdataRepresentations() throws GeneratorException {
    if (!bitdataRepresentations) {
      throw new GeneratorException("bitdata representations (\"b\" pass) required");
    }
  }

  static Tail unaryUnit(Position pos) { // Tail for \x -> Unit, i.e., k{} where k{} x = Unit()
    return new DataAlloc(Cfun.Unit).withArgs().constClosure(pos, 1);
  }

  static Tail binaryUnit(Position pos) { // Tail for \y -> \x -> Unit
    return unaryUnit(pos).constClosure(pos, 1);
  }

  static {

    // primBitFromLiteral v w ... :: Proxy v -> Bit w
    generators.put(
        "primBitFromLiteral",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal
            int w = ts[1].validWidth(); // Width of bit vector
            Type.validBelow(v, BigInteger.ONE.shiftLeft(w)); // v < 2 ^ w
            return new Return(Const.atoms(v, w)).constClosure(pos, 1);
          }
        });

    // primBitToWord w :: Bit w -> Word
    generators.put(
        "primBitToWord",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth();
            switch (width) {
              case 0:
                return new Return(Word.Zero).constClosure(pos, 1);

              case 1:
                return new PrimCall(Prim.flagToWord).makeUnaryFuncClosure(pos, 1);

              default:
                if (width < 0 || width > Word.size()) {
                  throw new GeneratorException(
                      "parameter "
                          + width
                          + " not accepted; value must be in the range 0 to "
                          + Word.size());
                }
                return new Return().makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primWordToBit w :: Word -> Bit w
    generators.put(
        "primWordToBit",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth();
            switch (width) {
              case 0:
                return new DataAlloc(Cfun.Unit).withArgs().constClosure(pos, 1);

              case 1:
                {
                  Temp[] vs = Temp.makeTemps(1);
                  Tail t = Prim.neq.withArgs(vs[0], Word.Zero);
                  return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, vs, t)).withArgs();
                }

              default:
                if (width < 0 || width > Word.size()) {
                  throw new GeneratorException(
                      "parameter "
                          + width
                          + " not accepted; value must be in the range 0 to "
                          + Word.size());
                } else if (width != Word.size()) {
                  Temp[] vs = Temp.makeTemps(1);
                  Tail t = Prim.and.withArgs(vs[0], (1L << width) - 1);
                  return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, vs, t)).withArgs();
                }
                return new Return().makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primBitConcat m n p :: Bit m -> Bit n -> Bit p,  where m+n = p
    generators.put(
        ":#",
        new Generator(3) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int m = ts[0].validWidth(1); // Width of first input (most significant bits)
            int n = ts[1].validWidth(1); // Width of second input (least significant bits)
            int p = ts[2].validWidth(1); // Width of result
            if (m + n != p) {
              throw new GeneratorException(p + " is not the sum of " + m + " and " + n);
            }
            return new BlockCall(BitdataLayout.generateBitConcat(pos, m, n))
                .makeBinaryFuncClosure(pos, Word.numWords(m), Word.numWords(n));
          }
        });

    // primBitSelect m n o :: Bit m -> Bit n, where o is the offset, o+n<=m
    generators.put(
        "primBitSelect",
        new Generator(3) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int m = ts[0].validWidth(); // Width of input bit vector
            int n = ts[1].validWidth(); // Width of output bit vector
            int o = ts[2].validWidth(); // Offset to output bits within the input vector
            if ((o + n) > m) {
              throw new GeneratorException(
                  "A field of width "
                      + n
                      + " at offset "
                      + o
                      + " will not fit in a bit vector of width "
                      + m);
            }
            return new BlockCall(BitdataField.generateBitSelector(pos, true, n < m, o, n, m))
                .makeUnaryFuncClosure(pos, Word.numWords(m));
          }
        });
  }

  static {

    // primIxFromLiteral v m :: Proxy v -> Ix m
    generators.put(
        "primIxFromLiteral",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal
            BigInteger m = ts[1].validIndex(); // Modulus for index type
            Type.validBelow(v, m); // v < m
            return new Return(new Word(v.longValue())).constClosure(pos, 1);
          }
        });

    // primIxMaxBound m :: Ix m
    generators.put(
        "primIxMaxBound",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger m = ts[0].validIndex(); // Modulus for index type
            return new Return(new Word(m.longValue() - 1));
          }
        });

    // primIxToBit m w :: Ix m -> Bit w
    generators.put(
        "primIxToBit",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger m = ts[0].validIndex(); // Modulus for index type
            int w = ts[1].validWidth(); // Width of bitdata type
            if (BigInteger.ONE.shiftLeft(w).compareTo(m) < 0) {
              throw new GeneratorException(
                  "width " + w + " is not large enough for index value " + m);
            }
            Temp[] vs = Temp.makeTemps(1); // Argument holds incoming index
            int n = Word.numWords(w);
            Atom[] as = new Atom[n];
            as[0] = vs[0];
            for (int i = 1; i < n; i++) { // In general, could return multiple words
              as[i] = Word.Zero;
            }
            ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, vs, new Return(as));
            return new ClosAlloc(k).withArgs();
          }
        });
  }

  static {

    // primIxShiftL n p :: Ix n -> Ix p -> Ix n,  where n=2^p
    generators.put(
        "primIxShiftL",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validIndex(); // Modulus for index type
            int p = validIxShift(ts[1], n); // Modulus for shift amount such that n = 2^p
            Temp[] vs = Temp.makeTemps(2); // One word for each Ix argument
            Block b = new Block(pos, vs, maskTail(Prim.shl.withArgs(vs[0], vs[1]), p));
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }
        });

    // primIxShiftR n p :: Ix n -> Ix p -> Ix n,  where n=2^p
    generators.put(
        "primIxShiftR",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validIndex(); // Modulus for index type
            int p = validIxShift(ts[1], n); // Modulus for shift amount such that n = 2^p
            return new PrimCall(Prim.lshr).makeBinaryFuncClosure(pos, 1, 1);
          }
        });
  }

  /**
   * Determine if p is a natural number type such that n = 2^p for the given n. Both p and n must be
   * valid index types, so n must be in the range 1 <= n < Word.size() if the condition is
   * satisfied. The return result is the value of p, as an int.
   */
  private static int validIxShift(Type p, BigInteger n) throws GeneratorException {
    BigInteger pb = p.validNat(); // Find BigInteger value for p
    Type.validBelow(pb, Word.sizeBig());
    int pi = pb.intValue(); // Find int value for p
    Type.validNotBelow(pi, 1);
    BigInteger q = BigInteger.ONE.shiftLeft(pi);
    if (q.compareTo(n) != 0) {
      throw new GeneratorException(n + " is not 2^" + pi + " (i.e., " + q + ")");
    }
    return pi;
  }

  static {

    // primModIx w m :: Bit w -> Ix m
    generators.put(
        "primModIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int w = ts[0].validWidth(2); // Width of bitdata type
            long m = ts[1].validIndex().longValue(); // Modulus for index type
            int n = Word.numWords(w);
            if ((m & (m - 1)) == 0) { // Special case for power of two
              Temp[] args = Temp.makeTemps(n);
              Tail t = Prim.and.withArgs(args[0], m - 1);
              return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
            } else if (n != 1) {
              throw new GeneratorException(
                  "modulus must be a power of two, or bit vector must fit in one word.");
            }
            Temp[] args =
                Temp.makeTemps(n); // TODO: add support for n>1, mod not a power of two ...
            Tail t = Prim.rem.withArgs(args[0], m);
            return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
          }
        });

    // primRelaxIx n m :: Ix n -> Ix m
    generators.put(
        "primRelaxIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validIndex(); // Smaller index modulus
            BigInteger m = ts[1].validIndex(); // Larger index modulus
            if (n.compareTo(m) > 0) {
              throw new GeneratorException("first parameter must not be larger than the second");
            }
            // We implement relaxIx as the identity function (Ix n and Ix m values are both
            // represented as Word values).
            // TODO: the type checker will infer a *polymorphic* type for this definition, which
            // will trip up the LLVM
            // code generator (although that will likely not happen in practice because this
            // definition should be inlined
            // by the optimizer).
            return new Return().makeUnaryFuncClosure(pos, 1);
          }
        });

    // primGenIncIx a m :: a -> (Ix m -> a) -> Ix m -> a
    generators.put(
        "primGenIncIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            BigInteger m = ts[1].validIndex(); // Index modulus, will be > 0

            // inc[j, i] = v <- add((i, 1)); j @ v
            Temp[] ji = Temp.makeTemps(2);
            Temp v = new Temp();
            Block inc =
                new Block(
                    pos,
                    ji,
                    new Bind(v, Prim.add.withArgs(ji[1], 1), new Done(new Enter(ji[0], v))));

            // b[n,..., j, i] = w <- ult((i, m-1)); if w then inc[j, i] else return [n,...]
            Temp[] nji = Temp.makeTemps(nl + 2);
            Block b = guardBlock(pos, nji, Prim.ult.withArgs(nji[nl + 1], m.intValue() - 1), inc);
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primIncIx m :: Ix m -> Maybe (Ix m)
    // Special case: requires bitdataRepresentations, and m < 2^WordSize
    generators.put(
        "primIncIx",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Index modulus, must be > 0
            validBitdataRepresentations(); // ensure Maybe (Ix m) and Ix (m+1) have same
                                           // representation
            Temp[] vs = Temp.makeTemps(1);
            Tail t = (m == 1) ? new Return(Flag.True) : Prim.add.withArgs(vs[0], 1);
            Block b = new Block(pos, vs, new Done(t));
            return new BlockCall(b).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primGenDecIx n :: a -> (Ix n -> a) -> Ix n -> a
    generators.put(
        "primGenDecIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            BigInteger m = ts[1].validIndex(); // Index modulus, must be > 0

            // dec[j, i] = v <- sub((i, 1)); j @ v
            Temp[] ji = Temp.makeTemps(2);
            Temp v = new Temp();
            Block dec =
                new Block(
                    pos,
                    ji,
                    new Bind(v, Prim.sub.withArgs(ji[1], 1), new Done(new Enter(ji[0], v))));

            // b[n,..., j, i] = w <- ugt((i, 0)); if w then dec[j, i] else return [n,...]
            Temp[] nji = Temp.makeTemps(nl + 2);
            Block b = guardBlock(pos, nji, Prim.ugt.withArgs(nji[nl + 1], 0), dec);
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primDecIx m :: Ix m -> Maybe (Ix m)
    // Special case: requires bitdataRepresentations, (m+1) is a power of 2, and m is a valid index
    // limit
    generators.put(
        "primDecIx",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Index modulus, must be > 0
            if (((m + 1) & m) != 0) { // ... with a successor that is a power of two
              throw new GeneratorException(m + " is not a power of two minus 1");
            }
            validBitdataRepresentations(); // ensure that Nothing is represented by all 1s
            Temp[] vs = Temp.makeTemps(1);
            Code c;
            if (m == 1) {
              c = new Done(new Return(Flag.True));
            } else {
              // In this special case, we can avoid a branching implementation by combining a
              // decrement with a mask:
              Temp w = new Temp();
              c = new Bind(w, Prim.sub.withArgs(vs[0], 1), new Done(Prim.and.withArgs(w, m)));
            }
            return new BlockCall(new Block(pos, vs, c)).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primMaybeIx a n :: a -> (Ix n -> a) -> Word -> a
    generators.put(
        "primGenMaybeIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus
            Block yes = enterBlock(pos); // yes[j, i] = j @ i
            // b[n,..., j, i] = w <- ule((i, m-1)); if w then yes[j, i] else return [n,...]
            // NOTE: using (v <= m-1) rather than (v < m) is important for the case where
            // m=(2^WordSize)
            Temp[] njv = Temp.makeTemps(nl + 2);
            Block b = guardBlock(pos, njv, Prim.ule.withArgs(njv[nl + 1], m - 1), yes);
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primGenLeqIx a n :: a -> (Ix n -> a) -> Word -> Ix n -> a
    generators.put(
        "primGenLeqIx",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            ts[1].validIndex(); // Check for valid index modulus
            Block yes = enterBlock(pos); // yes[j, i] = j @ i
            Block no = returnBlock(pos, nl); // no[thing] = return thing
            // b[n,..., j, v, i] = w <- ule((v, i)); if w then yes[j, v] else return [n,...]
            Temp[] njvi = Temp.makeTemps(nl + 3);
            Atom[] ns = new Atom[nl];
            for (int i = 0; i < nl; i++) {
              ns[i] = njvi[i];
            }
            Temp w = new Temp();
            Block b =
                new Block(
                    pos,
                    njvi,
                    new Bind(
                        w,
                        Prim.ule.withArgs(njvi[nl + 1], njvi[nl + 2]),
                        new If(
                            w,
                            new BlockCall(yes, new Atom[] {njvi[nl], njvi[nl + 1]}),
                            new BlockCall(no, ns))));
            return new BlockCall(b)
                .makeClosure(pos, nl + 2, 1)
                .makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });
  }

  /**
   * Return a block that takes two arguments---a function and another value---and just applies the
   * former to the latter.
   */
  static Block enterBlock(Position pos) {
    Temp[] fx = Temp.makeTemps(2);
    return new Block(pos, fx, new Done(new Enter(fx[0], fx[1])));
  }

  /**
   * Return a block that takes a single argument represented by n words and immediately returns that
   * argument.
   */
  static Block returnBlock(Position pos, int n) {
    Temp[] vs = Temp.makeTemps(n);
    return new Block(pos, vs, new Done(new Return(vs)));
  }

  /**
   * Return a block of the form b[n,..., j, v] = w <- test; if w then yes[j,v] else return [n,...].
   * The njv argument is expected to provide the temporaries that will be used as arguments to the
   * block, and it is expected that the test will involve the variable v (i.e., njv[nl+1], where nl
   * is the number of words needed to represent n). The n and j parameters will typically be
   * instantiated to Nothing and Just in practical uses, which may explain the choice of names ...
   */
  static Block guardBlock(Position pos, Temp[] njv, Tail test, Block yes) {
    int nl = njv.length - 2;
    Atom[] ns = new Atom[nl];
    for (int i = 0; i < nl; i++) {
      ns[i] = njv[i];
    }
    Temp w = new Temp();
    return new Block(
        pos,
        njv,
        new Bind(
            w,
            test,
            new If(
                w,
                new BlockCall(yes, new Atom[] {njv[nl], njv[nl + 1]}),
                new BlockCall(returnBlock(pos, nl), ns))));
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            ts[0].validIndex(); // Index upper bound
            return new PrimCall(cmp).makeBinaryFuncClosure(pos, 1, 1);
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return unaryUnit(pos);

              case 1:
                return new PrimCall(Prim.bnot).makeUnaryFuncClosure(pos, 1);

              default:
                {
                  int n = Word.numWords(width);
                  Temp[] vs = Temp.makeTemps(n); // variables returned from block
                  Temp[] ws = Temp.makeTemps(n); // arguments to closure
                  Code code = new Done(new Return(Temp.clone(vs)));
                  int rem = width % Word.size(); // nonzero => unused bits in most sig word

                  // Use Prim.xor on the most significant word if not all bits are used:
                  if (rem != 0) {
                    Temp v = vs[--n];
                    code =
                        new Bind(v, Prim.xor.withArgs(vs[n] = new Temp(), (1L << rem) - 1), code);
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return binaryUnit(pos);

              case 1:
                return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

              default:
                {
                  // Block: b[a0,...b0,...] = c0 <- p((a0,b0)); ...; return [c0,...]
                  int n = Word.numWords(width);
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return unaryUnit(pos);

              case 1: // Negate is the identity function on Bit 1!
                return new Return().makeUnaryFuncClosure(pos, 1);

              default:
                {
                  validSingleWord(width);
                  Temp[] args = Temp.makeTemps(1);
                  Code code = maskTail(Prim.neg.withArgs(args), width);
                  return new BlockCall(new Block(pos, args, code)).makeUnaryFuncClosure(pos, 1);
                }
            }
          }
        });
  }

  private static void validSingleWord(int width) throws GeneratorException {
    if (Word.numWords(width) != 1) {
      throw new GeneratorException(
          "bit vector of width " + width + " does not fit in a single word");
    }
  }

  private static Code maskTail(Tail t, int width) {
    int rem = width % Word.size(); // Determine whether masking is required
    if (rem == 0) {
      return new Done(t);
    } else {
      Temp c = new Temp();
      return new Bind(c, t, new Done(Prim.and.withArgs(c, (1L << rem) - 1)));
    }
  }

  /**
   * A general method for generating implementations for ARITHMETIC binary operations (add, sub,
   * mul), where masking of the most significant word may be required to match the requested length.
   * TODO: For the time being, these implementations only work for 0 <= width <= WordSize. The
   * algorithms for these operations on multi-word values are more complex and more varied, so they
   * will require a more sophisticated approach.
   */
  static void genArithBinOp(String ref, final PrimBinOp p, final PrimBinFOp pf) {
    // primBitRef w :: Bit w -> Bit w -> Bit w
    generators.put(
        ref,
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return binaryUnit(pos);

              case 1:
                return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

              default:
                {
                  validSingleWord(width);
                  Temp[] args = Temp.makeTemps(2);
                  Code code = maskTail(p.withArgs(args), width);
                  return new BlockCall(new Block(pos, args, code)).makeBinaryFuncClosure(pos, 1, 1);
                }
            }
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return new BlockCall(bz).withArgs().constClosure(pos, 1).constClosure(pos, 1);

              case 1:
                return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

              default:
                {
                  int n = Word.numWords(width);
                  return new BlockCall(bitEqBlock(pos, n, test, bearly))
                      .makeBinaryFuncClosure(pos, n, n);
                }
            }
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
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return new BlockCall(bz).withArgs().constClosure(pos, 1).constClosure(pos, 1);

              case 1:
                return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

              default:
                {
                  int n = Word.numWords(width);
                  return new BlockCall(bitLexCompBlock(pos, n, lsw, slsw))
                      .makeBinaryFuncClosure(pos, n, n);
                }
            }
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
                Prim.ult.withArgs(vs[numArgs], mid * Word.size()),
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
              Prim.sub.withArgs(vs[numArgs], lo * Word.size()), //  = p <- sub((i, offset))
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

    Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
      int width = ts[0].validWidth(2); // Width of bit vector
      int n = Word.numWords(width);
      return new BlockCall(decisionTree(pos, width, n, 0, n - 1, 0)).makeUnaryFuncClosure(pos, 1);
    }
  }

  protected abstract static class ConsumeBitmanipGenerator extends BitmanipGenerator {

    /** Default constructor. */
    protected ConsumeBitmanipGenerator(int needs) {
      super(needs);
    }

    Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
      int width = ts[0].validWidth(2); // Width of bit vector
      int n = Word.numWords(width);
      return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
          .makeBinaryFuncClosure(pos, n, 1);
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
              as[i] = (i == lo) ? mask : Word.Zero;
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
                w, Prim.and.withArgs(mask, vs[lo]), new Done(Prim.neq.withArgs(w, Word.Zero)));
          }
        });

    generators.put(
        "primBitBitSize",
        new Generator(1) { // :: Bit w -> Ix w
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(1); // Bit vector width
            int n = Word.numWords(width);
            Tail t = new Return(new Word(width - 1));
            // TODO: The Temp.makeTemps(n) call in the following creates the proxy argument of type
            // Bit w that is required as an input
            // for this function (to avoid ambiguity).  Because it is not actually used, however, it
            // will result in a polymorphic
            // definition in post-specialization code, which may break subsequent attempts to
            // generate code from monomorphic MIL code
            // ... unless this definition is optimized away (which, it should be ... assuming that
            // the optimizer is invoked ...)
            ClosureDefn k = new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(n), t);
            return new ClosAlloc(k).withArgs();
          }
        });
  }

  static {

    // primBitShiftL w :: Bit w -> Ix w -> Bit w,  for w>1
    generators.put(
        "primBitShiftL",
        new BitPosGenerator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width =
                ts[0].validWidth(
                    2); // Width of bit vector // TODO: add support for width 0 and width 1?
            int n = Word.numWords(width);
            return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
                .makeBinaryFuncClosure(pos, n, 1);
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
                      Prim.eq.withArgs(vs[n], lo * Word.size()),
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
           * WordSize.
           */
          private Block shiftLeftMultipleBlock(Position pos, int width, int n, int lo) {
            Temp[] vs = Temp.makeTemps(n + 1); // [v0,...,shift]
            Atom[] as = new Atom[n];
            for (int i = 0; i < lo; i++) { // words below lo in the result are zero
              as[i] = Word.Zero;
            }
            for (int i = lo; i < n; i++) { // words at or above lo are shifted up from lower indices
              as[i] = vs[i - lo];
            }
            Code code = new Done(new Return(as)); // Mask most significant word, if necessary
            int rem = width % Word.size();
            if (rem != 0) {
              Temp t = new Temp();
              code = new Bind(t, Prim.and.withArgs(as[n - 1], (1L << rem) - 1), code);
              as[n - 1] = t;
            }
            return new Block(pos, vs, code);
          }

          /**
           * Build a block to handle the case in a shift left where the shift is offset, NOT a
           * multiple of the WordSize. Also provides code for the case of a large shift that reaches
           * in to the most significant word.
           */
          private Block shiftLeftOffsetBlock(Position pos, Temp[] vs, int width, int n, int lo) {
            Atom[] as = new Atom[n];
            Temp offs = new Temp(); // holds offset within word
            Temp comp = new Temp(); // holds complement of offset (comp + offs == WordSize)
            Code code = new Done(new Return(as)); // Build up code in reverse ...

            // Zero out least significant words of result:
            for (int i = 0; i < lo; i++) {
              as[i] = Word.Zero;
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
            int rem = width % Word.size();
            if (rem != 0) {
              Temp t = new Temp();
              code = new Bind(t, Prim.and.withArgs(ts[n - lo - 1], (1L << rem) - 1), code);
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
                    Prim.sub.withArgs(vs[n], lo * Word.size()),
                    new Bind(
                        comp,
                        Prim.sub.withArgs(Word.size(), offs),
                        new Bind(ts[0], Prim.shl.withArgs(vs[0], offs), code))));
          }
        });

    // primBitShiftRu w :: Bit w -> Ix w -> Bit w,  for w>1
    generators.put(
        "primBitShiftRu",
        new BitPosGenerator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width =
                ts[0].validWidth(
                    2); // Width of bit vector // TODO: add support for width 0 and width 1?
            int n = Word.numWords(width);
            return new BlockCall(decisionTree(pos, width, n, 0, n - 1, n))
                .makeBinaryFuncClosure(pos, n, 1);
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
                      Prim.eq.withArgs(vs[n], lo * Word.size()),
                      new If(
                          v,
                          new BlockCall(shiftRightMultipleBlock(pos, n, lo), vs),
                          new BlockCall(
                              shiftRightOffsetBlock(pos, Temp.makeTemps(n + 1), n, lo), vs))));
            }
          }

          /**
           * Build a block to handle the case in a shift right where the shift is a multiple of the
           * Word.size.
           */
          private Block shiftRightMultipleBlock(Position pos, int n, int lo) {
            Temp[] vs = Temp.makeTemps(n + 1); // [v0,...,shift]
            Atom[] as = new Atom[n];
            int i = 0;
            for (; i + lo < n; i++) {
              as[i] = vs[i + lo];
            }
            for (; i < n; i++) {
              as[i] = Word.Zero;
            }
            return new Block(pos, vs, new Done(new Return(as)));
          }

          /**
           * Build a block to handle the case in a shift right where the shift is offset, NOT a
           * multiple of the WordSize. Also provides code for the case of a large shift that reaches
           * in to the most significant word.
           */
          private Block shiftRightOffsetBlock(Position pos, Temp[] vs, int n, int lo) {
            Atom[] as = new Atom[n];
            Temp offs = new Temp(); // holds offset within word
            Temp comp = new Temp(); // holds complement of offset (comp + offs == WordSize)
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
              as[i] = Word.Zero;
            }
            return new Block(
                pos,
                vs,
                new Bind(
                    offs,
                    Prim.sub.withArgs(vs[n], lo * Word.size()),
                    new Bind(comp, Prim.sub.withArgs(Word.size(), offs), code)));
          }
        });
  }

  static {

    // primNZBitFromLiteral v w ... :: Proxy v -> NZBit w
    generators.put(
        "primNZBitFromLiteral",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal (must be nonzero!)
            int w = ts[1].validWidth(); // Width of bit vector
            if (v.signum() <= 0) {
              throw new GeneratorException("A nonzero value is required");
            }
            Type.validBelow(v, BigInteger.ONE.shiftLeft(w)); // v < 2 ^ w
            ClosureDefn k =
                new ClosureDefn(
                    pos,
                    Temp.noTemps,
                    Temp.makeTemps(1),
                    new Return(new Word(v.longValue()))); //  k{} _ = return [v]
            return new ClosAlloc(k).withArgs();
          }
        });

    // primNZBitNonZero w :: Bit w -> Maybe (NZBit w)
    generators.put(
        "primNZBitNonZero",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int w = ts[0].validWidth(2); // Width of bit vector
            validSingleWord(w);
            validBitdataRepresentations(); // ensures same rep for Maybe (NZBit w), Bit w.
            return new Return().makeUnaryFuncClosure(pos, 1);
          }
        });

    // primNZBitDiv w :: Bit w -> NZBit w -> Bit w
    generators.put(
        "primNZBitDiv",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int w = ts[0].validWidth(2); // Width of bit vector (must fit within a single word)
            validSingleWord(w);
            return new PrimCall(Prim.div).makeBinaryFuncClosure(pos, 1, 1);
          }
        });
  }

  private static class StoredAccess {

    int lo;

    int hi;

    Prim load;

    Prim store;

    /** Default constructor. */
    private StoredAccess(int lo, int hi, Prim load, Prim store) {
      this.lo = lo;
      this.hi = hi;
      this.load = load;
      this.store = store;
    }
  }

  private static final StoredAccess[] storedAccessTable =
      new StoredAccess[] {
        new StoredAccess(1, 1, Prim.load1, Prim.store1),
        new StoredAccess(2, 8, Prim.load8, Prim.store8),
        new StoredAccess(9, 16, Prim.load16, Prim.store16),
        new StoredAccess(17, 32, Prim.load32, Prim.store32),
        new StoredAccess(33, 64, Prim.load64, Prim.store64)
      };

  private static StoredAccess findStoredAccess(int width) throws GeneratorException {
    for (int i = 0; i < storedAccessTable.length; i++) {
      StoredAccess sa = storedAccessTable[i];
      if (sa.lo <= width && width <= sa.hi) {
        return sa;
      }
    }
    throw new GeneratorException("No memory access to stored values of width " + width);
  }

  static {

    // primReadRefStored t :: Ref (Stored t) -> Proc t
    generators.put(
        "primReadRefStored",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validMemBitSize();
            if (width == 0) {
              return new DataAlloc(Cfun.Unit).withArgs().constClosure(pos, 0).constClosure(pos, 1);
            }
            Prim load = findStoredAccess(width).load; // select appropriate load primitive
            Temp[] vs = Temp.makeTemps(1);
            ClosureDefn k = new ClosureDefn(pos, vs, Temp.noTemps, load.repTransformPrim(set, vs));
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primWriteRefStored t :: Ref (Stored t) -> t -> Proc Unit
    generators.put(
        "primWriteRefStored",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validMemBitSize();
            if (width == 0) {
              return new DataAlloc(Cfun.Unit)
                  .withArgs()
                  .constClosure(pos, 0)
                  .constClosure(pos, 1)
                  .constClosure(pos, 1);
            }
            Prim store = findStoredAccess(width).store; // select appropriate store primitive
            int n = ts[0].repLen();
            Temp[] vs = Temp.makeTemps(1 + n); // Temps to hold the address and value
            ClosureDefn k = new ClosureDefn(pos, vs, Temp.noTemps, store.repTransformPrim(set, vs));
            return new ClosAlloc(k).makeBinaryFuncClosure(pos, 1, n);
          }
        });

    // primInitStored t :: t -> Init (Stored t) (loosely equiv to: [t] ->> [[Word] ->> [Unit]]
    generators.put(
        "primInitStored",
        new Generator(1) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validMemBitSize();
            if (width == 0) {
              return binaryUnit(pos);
            }
            Prim store = findStoredAccess(width).store; // select appropriate store primitive
            int n = ts[0].repLen();
            Temp[] vs = Temp.makeTemps(n); // Temps to hold the initial value
            Temp[] a = Temp.makeTemps(1); // Temp to hold the address/reference
            ClosureDefn k =
                new ClosureDefn(pos, vs, a, store.repTransformPrim(set, Temp.append(a, vs)));
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, n);
          }
        });
  }

  static {

    // (@) n a :: Ref (Array n a) -> Ix n -> Ref a
    generators.put(
        "@",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validNat(); // Array length
            Type.validSigned(n);
            long size = ts[1].validArrayArea(); // Size of array elements
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
        });
  }

  static {

    // primInitArray n a :: (Ix n -> Init a) -> Init (Array n a)
    generators.put(
        "primInitArray",
        new Generator(2) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validNat(); // Array length
            Type.validSigned(n);
            long len = n.longValue();
            long size = ts[1].validArrayArea(); // Size of array elements
            return (len <= 4) ? initArrayUnroll(pos, len, size) : initArrayLoop(pos, len, size);
          }
        });
  }

  /**
   * Generate code for a loop-based implementation of initArray. The structure of the generated code
   * is as follows: (len=array length, size=element size)
   *
   * <p>initArray <- k0{} k0{} f = k1{f} k1{f} r = loop[f, r, 0] loop[f, r, j] = v <- ult((j, len))
   * if v then step[f, r, j] else done[] step[f, r, i] = [] <- noinline(()) g <- f @ i x <- g @ r j
   * <- add((i, i)) s <- add((r, size)) loop[f, s, j] done[] = Unit()
   *
   * <p>(Assumes implementation: Init a = [Ref a] ->> [Unit])
   */
  private static Tail initArrayLoop(Position pos, long len, long size) {
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
            Temp.noTemps,
            Prim.noinline.withArgs(),
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
                            new Done(new BlockCall(loop, new Atom[] {fri[0], s, j}))))))));

    Temp[] f = Temp.makeTemps(1);
    Temp[] r = Temp.makeTemps(1);
    ClosureDefn k1 =
        new ClosureDefn(pos, f, r, new BlockCall(loop, new Atom[] {f[0], r[0], Word.Zero}));
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
  private static Tail initArrayUnroll(Position pos, long len, long size) {
    Code code = new Done(Cfun.Unit.withArgs());
    Temp f = new Temp();
    Temp r = new Temp();
    if (len > 0) {
      long i = len - 1;
      for (; ; ) {
        Temp g = new Temp();
        code = new Bind(g, new Enter(f, new Word(i)), new Bind(new Temp(), new Enter(g, r), code));
        if (i > 0) {
          Temp r1 = new Temp();
          code = new Bind(r, Prim.add.withArgs(r1, size), code);
          r = r1;
          i--;
        } else {
          break;
        }
      }
    }
    Block work = new Block(pos, new Temp[] {f, r}, code);
    return new BlockCall(work).makeBinaryFuncClosure(pos, 1, 1);
  }

  static {

    // primStructSelect st lab t :: Ref st -> #lab -> Ref t
    generators.put(
        "primStructSelect",
        new Generator(3) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            StructType st = ts[0].structType(); // Structure type
            if (st == null) {
              throw new GeneratorException(ts[0] + " is not a structure type");
            }
            String lab = ts[1].getLabel(); // Field label
            if (lab == null) {
              throw new GeneratorException(ts[1] + " is not a field label");
            }
            StructField[] fields = st.getFields();
            int i = Name.index(lab, fields);
            if (i < 0) {
              throw new GeneratorException("There is no \"" + lab + "\" field in " + st);
            }
            Temp[] vs = Temp.makeTemps(1);
            Tail tail = Prim.add.withArgs(vs[0], fields[i].getOffset());
            ClosureDefn k = new ClosureDefn(pos, vs, Temp.makeTemps(1), tail);
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, 1);
          }
        });
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

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    Type t = declared.isMonomorphic();
    if (t == null) {
      debug.Internal.error("external " + id + " has polymorphic type " + declared);
    } else if (t.nonUnit()) {
      prog.add(new llvm.GlobalVarDecl(id, lm.toLLVM(t)));
    }
  }

  /** Count the number of non-tail calls to blocks in this abstract syntax fragment. */
  void countCalls() {
    /* Nothing to do here */
  }

  /**
   * Count the number of calls to blocks, both regular and tail calls, in this abstract syntax
   * fragment. This is suitable for counting the calls in the main function; unlike countCalls, it
   * does not skip tail calls at the end of a code sequence.
   */
  void countAllCalls() {
    /* Nothing to do here */
  }

  /**
   * Generate code (in reverse) to initialize each TopLevel (unless all of the components are
   * statically known).
   */
  llvm.Code addRevInitCode(LLVMMap lm, InitVarMap ivm, llvm.Code code) {
    // Generate code to load values of externals in case they are needed later in initialization
    // TODO: Will LLVM optimize away these loads if they are not actually needed?
    // Can we avoid generating them in the first place?
    Type t = declared.isMonomorphic(); // - find the MIL type of this external
    llvm.Type gt = lm.toLLVM(t); // - find the corresponding LLVM type
    llvm.Global g = new llvm.Global(gt.ptr(), id); // - find the global for this external
    llvm.Local l = ivm.reg(gt); // - find a local to hold its value
    ivm.mapGlobal(new TopExt(this), l); // - record the load in the var map
    return new llvm.Op(l, new llvm.Load(g), code); // - emit code to load the value
  }
}
