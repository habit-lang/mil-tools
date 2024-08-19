/*
    Copyright 2018-19 Mark P Jones, Portland State University

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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.TreeSet;
import obdd.Pat;

/**
 * Requests the generation of an implementation for an external using a given tag and list of type
 * parameters.
 */
public class GenImp extends ExtImp {

  private String ref;

  private Type[] ts;

  /** Default constructor. */
  public GenImp(String ref, Type[] ts) {
    this.ref = ref;
    this.ts = ts;
  }

  void dump(PrintWriter out, StringTypeWriter tw) {
    out.print(" {" + ref);
    for (int i = 0; i < ts.length; i++) {
      out.print(" ");
      out.print(ts[i].toString(TypeWriter.ALWAYS, tw));
      tw.reset();
    }
    out.print("}");
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    for (int i = 0; i < ts.length; i++) {
      ts[i] = ts[i].canonType(set);
    }
  }

  /** Generate a specialized version of this external implementation strategy. */
  ExtImp specialize(MILSpec spec, Type[] tenv) {
    Type[] nts = new Type[ts.length];
    for (int i = 0; i < ts.length; i++) {
      nts[i] = ts[i].canonType(tenv, spec);
    }
    GenImp newImp = new GenImp(ref, nts);
    newImp.gen = gen;
    return newImp;
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    for (int i = 0; i < ts.length; i++) {
      ts[i] = ts[i].canonType(spec);
    }
  }

  abstract static class Generator {

    /** Prefix describing parameters needed to use this generator. */
    protected Prefix prefix;

    /** Type skeleton for generated value (or close approximation). */
    protected Type type;

    /** Default constructor. */
    Generator(Prefix prefix, Type type) {
      this.prefix = prefix;
      this.type = type;
    }

    /** Check that a given list of arguments is valid for this generator. */
    void checkArguments(Position pos, String ref, Type[] ts, Scheme declared) throws Failure {
      int n = prefix.numGenerics();
      if (ts.length < n) { // Check that there are enough arguments
        throw new Failure(
            pos, "Generator " + ref + " requires " + n + " argument" + ((n == 1) ? "" : "s"));
      }
      Type[] declEnv = declared.getPrefix().instantiate();
      Type[] instEnv = new Type[n];
      for (int i = 0; i < n; i++) { // Check that the arguments have the expected kinds
        Kind ekind = prefix.getGen(i).getKind(); // expected kind
        Kind tkind = ts[i].calcKind(declEnv); // actual kind of parameter
        if (tkind == null || !ekind.same(tkind)) {
          throw new Failure(
              pos,
              "Generator argument "
                  + (i + 1)
                  + " ("
                  + ts[i]
                  + ") does not have expected kind ("
                  + ekind
                  + ")");
        }
        instEnv[i] = ts[i].with(declEnv);
      }
      //    TODO: uncomment this and replace with more appropriate diagnostic when ready to check
      // declared types
      //    if (!declared.getType().same(declEnv, type, instEnv)) {
      //      System.out.println("Declared type (" + declared.getType().skeleton(declEnv) +
      //                         ") does not match inst type (" + type.skeleton(instEnv) + ")");
      //    }
    }

    /**
     * Generate a tail as the implementation of an external described by a reference and list of
     * types.
     */
    abstract Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException;

    void dump(PrintWriter out, String ref, int width) {
      out.print(ref);
      for (int i = ref.length(); i <= width; i++) {
        out.print(' ');
      }
      out.println(prefix.forall(type).toString());
    }
  }

  /**
   * Stores a mapping from String references to generators for external function implementations.
   */
  static HashMap<String, Generator> generators = new HashMap();

  /** Stores a pointer to the generator for this GenImp, if there is one. */
  private Generator gen;

  /**
   * Validate a generator implementation in context and construct abstract syntax for full external
   * definition.
   */
  public External validate(Position pos, String id, Scheme declared) throws Failure {
    // Called when the this GenImp is created to check for a valid call and initialize the
    // associated Generator:
    gen = generators.get(ref); // look for a generator ...
    if (gen != null) { // If found ...
      gen.checkArguments(pos, ref, ts, declared); // ... then check arguments
    } else if (ts.length > 0) { // No generator, but arguments supplied
      throw new Failure(pos, "No generator for " + ref);
    }
    return new External(pos, id, declared, this);
  }

  /** Write the current list of external generators to the given PrintWriter. */
  public static void dumpGenerators(PrintWriter out) {
    TreeSet<String> refs = new TreeSet<String>();
    // Find the number of external generators:
    int width = 0;
    for (String ref : generators.keySet()) {
      int len = ref.length();
      if (len > width) {
        width = len;
      }
      refs.add(ref);
    }
    out.println("External generators: --------------------");
    for (String ref : refs) {
      generators.get(ref).dump(out, ref, width);
    }
    out.println(refs.size() + " external generators listed");
    out.println("-----------------------------------------");
  }

  /**
   * Attempt to generate an implementation, post representation transformation, for an external
   * primitive.
   */
  TopDefn repImplement(Handler handler, External ext, Type[] reps, RepTypeSet set) throws Failure {
    Position pos = ext.getPos();
    if (gen != null) {
      try {
        // Use generator to make a new top level definition as replacement for ext
        return repImplement(pos, ext, reps, gen.generate(pos, ts, set));
      } catch (GeneratorException e) {
        throw new Failure(pos, "No generated implementation: " + e.getReason());
      }
    }
    if (ts.length > 0) {
      throw new Failure(pos, "No generator for " + ref);
    }
    return ext.generatePrim(ref, reps);
  }

  /** Flag to indicate whether bitdata representations (e.g., for Maybe (Ix 15)) are in use. */
  private static boolean bitdataRepresentations = false;

  /** Set the bitdataRepresentations flag; intended to be called in the driver as appropriate. */
  public static void setBitdataRepresentations() {
    bitdataRepresentations = true;
  }

  private static void validBitdataRepresentations() throws GeneratorException {
    if (!bitdataRepresentations) {
      throw new GeneratorException("Bitdata representations (\"b\" pass) required");
    }
  }

  /** Check that the specified type corresponds to a bitdata type. */
  private static BitdataType validBitdataType(Type t) throws GeneratorException {
    BitdataType bt = t.bitdataType();
    if (bt == null) {
      throw new GeneratorException(t + " is not a bitdata type");
    }
    return bt;
  }

  /**
   * Check that the type t is a natural number whose value matches the width of the given bitdata
   * type.
   */
  private static void checkBitdataWidth(BitdataType bt, Type t) throws GeneratorException {
    int width = t.validWidth(); // make sure that t is a natural number
    Pat pat = bt.getPat();
    int btwidth = pat.getWidth();
    if (btwidth != width) { // ensure that t matches width of bitdata Type
      throw new GeneratorException(
          "Bitdata " + bt + " has width " + btwidth + " which does not match " + width);
    }
  }

  /** Check that bitdata bt is not restricted (not restricted, abstract or contains junk) */
  static void checkBitdataNotRestricted(BitdataType bt) throws GeneratorException {
    Pat pat = bt.getPat();
    if (pat.isRestricted()) {
      throw new GeneratorException(
          "Bitdata type " + bt + " has restricted (reference or abstract) fields");
    } else if (!pat.isAll()) {
      throw new GeneratorException("Bitdata type " + bt + " contains junk");
    }
  }

  static Tail unaryUnit(Position pos) { // Tail for \x -> Unit, i.e., k{} where k{} x = Unit()
    return new DataAlloc(Cfun.Unit).withArgs().constClosure(pos, 1);
  }

  static Tail binaryUnit(Position pos) { // Tail for \y -> \x -> Unit
    return unaryUnit(pos).constClosure(pos, 1);
  }

  static final TGen gA = Type.gen(0);

  static final TGen gB = Type.gen(1);

  static final TGen gC = Type.gen(2);

  static final Type word = Tycon.word.asType();

  static final Type flag = Tycon.flag.asType();

  static final Type unit = Tycon.unit.asType();

  static final Type todo = new PrimTycon("TODO", KAtom.STAR, 0).asType();

  static final Type bool = todo;

  static final Type maybeIx = todo;

  static final Type fun(Type d, Type r) {
    return Type.fun(d, r);
  }

  static final Type fun(Type d1, Type d2, Type r) {
    return fun(d1, fun(d2, r));
  }

  static final Type fun(Type d1, Type d2, Type d3, Type r) {
    return fun(d1, fun(d2, d3, r));
  }

  static final Type fun(Type d1, Type d2, Type d3, Type d4, Type r) {
    return fun(d1, fun(d2, d3, d4, r));
  }

  static Type bitA = Type.bit(gA);

  static Type bitB = Type.bit(gB);

  static Type bitC = Type.bit(gC);

  static Type natA = Type.nat(gA);

  static Type bitAbitA = fun(bitA, bitA);

  static Type bitAbitB = fun(bitA, bitB);

  static Type bitAbitAbitA = fun(bitA, fun(bitA, bitA));

  static Type bitAbitABool = fun(bitA, bitA, bool);

  static Type gAgA = fun(gA, gA);

  static Type gAgAgA = fun(gA, gAgA);

  static Type gAgAbool = fun(gA, fun(gA, bool));

  static Type ixA = Type.ix(gA);

  static Type ixB = Type.ix(gB);

  static Type ixC = Type.ix(gC);

  static Type inxA = Type.inx(gA);

  static Type bitAixAbitA = fun(bitA, ixA, bitA);

  static Type ixAixBixA = fun(ixA, ixB, ixA);

  static Type arrayAB = Type.array(gA, gB);

  static Type padAB = Type.pad(gA, gB);

  static Type refA = Type.ref(gA);

  static Type refB = Type.ref(gB);

  static Type refC = Type.ref(gC);

  static Type initA = Type.init(gA);

  static {

    // primBitFromLiteral v w :: ProxyNat v -> Bit w
    generators.put(
        "primBitFromLiteral",
        new Generator(Prefix.nat_nat, fun(natA, bitB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal
            int w = ts[1].validWidth(); // Width of bit vector
            Type.validBelow(v, BigInteger.ONE.shiftLeft(w)); // v < 2 ^ w
            return new Return(Const.atoms(v, w)).constClosure(pos, Tycon.unitRep);
          }
        });

    // primBitdataToBit :: bitdataType -> Bit n  where n==width of the bitdata type
    generators.put(
        "primBitdataToBit",
        new Generator(Prefix.star_nat, fun(gA, bitB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BitdataType bt = validBitdataType(ts[0]);
            checkBitdataWidth(bt, ts[1]);
            return new Return().makeUnaryFuncClosure(pos, ts[0].repLen());
          }
        });

    // primBitToBitdata :: Bit n -> bitdataType   where n==width of the bitdata type
    generators.put(
        "primBitToBitdata",
        new Generator(Prefix.nat_star, fun(bitA, gB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BitdataType bt = validBitdataType(ts[1]);
            checkBitdataWidth(bt, ts[0]);
            checkBitdataNotRestricted(bt);
            return new Return().makeUnaryFuncClosure(pos, ts[1].repLen());
          }
        });

    // primBitToWord w :: Bit w -> Word
    generators.put(
        "primBitToWord",
        new Generator(Prefix.nat, fun(bitA, word)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth();
            switch (width) {
              case 0:
                return new Return(Word.Zero).constClosure(pos, Tycon.unitRep);

              case 1:
                return new PrimCall(Prim.flagToWord).makeUnaryFuncClosure(pos, 1);

              default:
                if (width < 0 || width > Word.size()) {
                  throw new GeneratorException(
                      "Argument value "
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
        new Generator(Prefix.nat, fun(word, bitA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth();
            switch (width) {
              case 0:
                return unaryUnit(pos);

              case 1:
                return new PrimCall(Prim.wordToFlag).makeUnaryFuncClosure(pos, 1);

              default:
                if (width < 0 || width > Word.size()) {
                  throw new GeneratorException(
                      "Argument value "
                          + width
                          + " not accepted; value must be in the range 0 to "
                          + Word.size());
                } else if (width != Word.size()) {
                  Temp[] vs = Temp.makeTemps(Tycon.wordRep);
                  Tail t = Prim.and.withArgs(vs[0], (1L << width) - 1);
                  return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, vs, t)).withArgs();
                }
                return new Return().makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primBitsConcat m n p :: Bit m -> Bit n -> Bit p,  where m+n = p
    generators.put(
        "primBitsConcat",
        new Generator(Prefix.nat_nat_nat, fun(bitA, bitB, bitC)) {
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

    // primBitSelect m o n :: Bit m -> ProxyNat o -> Bit n, where o is the offset, o+n<=m
    generators.put(
        "primBitSelect",
        new Generator(Prefix.nat_nat_nat, fun(bitA, fun(Type.nat(gB), bitC))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int m = ts[0].validWidth(); // Width of input bit vector
            int n = ts[2].validWidth(); // Width of output bit vector
            int o = ts[1].validWidth(); // Offset to output bits within the input vector
            if ((o + n) > m) {
              throw new GeneratorException(
                  "A field of width "
                      + n
                      + " at offset "
                      + o
                      + " will not fit in a bit vector of width "
                      + m);
            }
            int w = Word.numWords(m); // How many words do we need for Bit m argument?
            Temp[] vs = Temp.makeTemps(Type.repBits(m)); // Make corresponding parameters
            Tail tail =
                new BlockCall(BitdataField.generateBitSelector(pos, true, n < m, o, n, m), vs);
            ClosureDefn k =
                new ClosureDefn(
                    pos, vs, Temp.makeTemps(Tycon.unitRep), tail); // ignore unit argument
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, w);
          }
        });

    // primBitsHi m n :: Bit m -> Bit n, where n<=m
    generators.put(
        "primBitsHi",
        new Generator(Prefix.nat_nat, bitAbitB) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int m = ts[0].validWidth(); // Width of input bit vector
            int n = ts[1].validWidth(); // Width of output bit vector
            if (n > m) {
              throw new GeneratorException("Width of output exceeds width of input");
            }
            return new BlockCall(BitdataField.generateBitSelector(pos, true, n < m, m - n, n, m))
                .makeUnaryFuncClosure(pos, Word.numWords(m));
          }
        });

    // primBitsLo m n :: Bit m -> Bit n, where n<=m
    generators.put(
        "primBitsLo",
        new Generator(Prefix.nat_nat, bitAbitB) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int m = ts[0].validWidth(); // Width of input bit vector
            int n = ts[1].validWidth(); // Width of output bit vector
            if (n > m) {
              throw new GeneratorException("Width of output exceeds width of input");
            }
            return new BlockCall(BitdataField.generateBitSelector(pos, true, n < m, 0, n, m))
                .makeUnaryFuncClosure(pos, Word.numWords(m));
          }
        });
  }

  static {

    // primIxFromLiteral v m :: ProxyNat v -> Ix m
    generators.put(
        "primIxFromLiteral",
        new Generator(Prefix.nat_nat, fun(natA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal
            BigInteger m = ts[1].validIndex(); // Modulus for index type
            Type.validBelow(v, m); // v < m
            long n = m.longValue();
            return (n == 1)
                ? unaryUnit(pos)
                : new Return((n == 2) ? Flag.fromBool(v.signum() > 0) : new Word(v.longValue()))
                    .constClosure(pos, Tycon.unitRep);
          }
        });

    // primIxMaxBound m :: Ix m
    generators.put(
        "primIxMaxBound",
        new Generator(Prefix.nat, ixA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long n = ts[0].validIndex().longValue(); // Modulus for index type
            return (n == 1)
                ? new DataAlloc(Cfun.Unit).withArgs()
                : new Return((n == 2) ? Flag.True : new Word(n - 1));
          }
        });

    // primInxMaxBound m :: Inx m
    generators.put(
        "primInxMaxBound",
        new Generator(Prefix.nat, inxA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long n = ts[0].validIndex().longValue(); // Modulus for index type
            return new Return((n == 1) ? Flag.True : new Word(n));
          }
        });

    // primIxToBit m w :: Ix m -> Bit w
    generators.put(
        "primIxToBit",
        new Generator(Prefix.nat_nat, fun(ixA, bitB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger m = ts[0].validIndex(); // Modulus for index type
            int w = ts[1].validWidth(); // Width of bitdata type
            if (BigInteger.ONE.shiftLeft(w).compareTo(m) < 0) {
              throw new GeneratorException(
                  "Width " + w + " is not large enough for index value " + m);
            }
            long l = m.longValue(); // Modulus as a long
            Temp[] vs = Temp.makeTemps(Tycon.wordRep); // Argument holds incoming index
            Tail t;
            if (w == 0) {
              t = Cfun.Unit.withArgs(); // :: Unit -> Unit
            } else if (w == 1) {
              if (l == 1) { // :: Unit -> Flag
                t = new Return(Flag.False);
              } else { // :: Flag -> Flag
                t = new Return(vs[0]);
              }
            } else {
              int n = Word.numWords(w);
              Atom[] as = new Atom[n]; // Array of atoms
              t = new Return(as);
              for (int i = 1; i < n; i++) { // all but least sig word will be zero
                as[i] = Word.Zero;
              }
              if (l == 1) { // :: Unit -> Bit w ... return all zeros
                as[0] = Word.Zero;
              } else if (l == 2) { // :: Flag -> Bit w
                Temp[] ws = Temp.makeTemps(Tycon.flagRep);
                Temp u = new Temp();
                as[0] = u;
                Block b =
                    new Block(pos, ws, new Bind(u, Prim.flagToWord.withArgs(ws[0]), new Done(t)));
                t = new BlockCall(b).withArgs(vs[0]);
              } else { // :: Word -> Bit w ... insert arg as lsw, any other words zero
                as[0] = vs[0];
              }
            }
            return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, vs, t)).withArgs();
          }
        });
  }

  static {

    // primIxShiftL n p :: Ix n -> Ix p -> Ix n,  where n=2^p
    generators.put(
        "primIxShiftL",
        new Generator(Prefix.nat_nat, ixAixBixA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validIndex(); // Modulus for index type
            int p = validIxShift(ts[1], n); // Modulus for shift amount such that n = 2^p
            Temp[] vs = Temp.makeTemps(2); // One parameter for each Ix argument
            Code code;
            if (p > 2) { // :: Word -> Word -> Word
              code = maskTail(Prim.shl.withArgs(vs[0], vs[1]), p);
            } else if (p == 2) { // :: Word -> Flag -> Word
              Temp t = new Temp();
              code =
                  new Bind(
                      t, Prim.flagToWord.withArgs(vs[1]), maskTail(Prim.shl.withArgs(vs[0], t), p));
            } else /* (p==1) */ { // :: Flag -> Unit -> Flag
              code = new Done(new Return(vs[0]));
            }
            return new BlockCall(new Block(pos, vs, code)).makeBinaryFuncClosure(pos, 1, 1);
          }
        });

    // primIxShiftR n p :: Ix n -> Ix p -> Ix n,  where n=2^p
    generators.put(
        "primIxShiftR",
        new Generator(Prefix.nat_nat, ixAixBixA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger n = ts[0].validIndex(); // Modulus for index type
            int p = validIxShift(ts[1], n); // Modulus for shift amount such that n = 2^p
            if (p > 2) { // :: Word -> Word -> Word
              return new PrimCall(Prim.lshr).makeBinaryFuncClosure(pos, 1, 1);
            }
            Temp[] vs = Temp.makeTemps(2); // One parameter for each Ix argument
            Code code;
            if (p == 2) { // :: Word -> Flag -> Word
              Temp t = new Temp();
              code =
                  new Bind(
                      t, Prim.flagToWord.withArgs(vs[1]), new Done(Prim.lshr.withArgs(vs[0], t)));
            } else /* (p==1) */ { // :: Flag -> Unit -> Flag
              code = new Done(new Return(vs[0]));
            }
            return new BlockCall(new Block(pos, vs, code)).makeBinaryFuncClosure(pos, 1, 1);
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
        new Generator(Prefix.nat_nat, fun(bitA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int w = ts[0].validWidth(); // Width of bitdata type
            int n = Word.numWords(w);
            BigInteger bm = ts[1].validIndex(); // Modulus for index type
            long m = bm.longValue();
            if (m == 1) {
              return unaryUnit(pos); // :: a -> Unit
            } else if (m == 2) {
              return (w == 0)
                  ? new Return(Flag.False).constClosure(pos, Tycon.unitRep) // :: Unit -> Flag
                  : (w == 1)
                      ? new Return().makeUnaryFuncClosure(pos, 1) // :: Flag -> Flag
                      : new PrimCall(Prim.wordToFlag)
                          .makeUnaryFuncClosure(pos, n); // :: Word.. -> Flag
            } else {
              if (w == 0) { // :: Unit -> Word
                return new Return(Word.Zero).constClosure(pos, Tycon.unitRep);
              } else if (w == 1) { // :: Flag -> Word
                return new PrimCall(Prim.flagToWord).makeUnaryFuncClosure(pos, 1);
              } else if (BigInteger.ONE.shiftLeft(w).compareTo(bm)
                  <= 0) { // 2^w <= m: can use identity
                return new Return().makeUnaryFuncClosure(pos, 1);
              } else if ((m & (m - 1)) == 0) { // special case for power of two
                Temp[] args = Temp.makeTemps(n);
                Tail t = Prim.and.withArgs(args[0], m - 1);
                return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
              } else if (n == 1) {
                Temp[] args = Temp.makeTemps(n);
                Tail t = Prim.nzrem.canonPrim(set).withArgs(args[0], m);
                return new ClosAlloc(new ClosureDefn(pos, Temp.noTemps, args, t)).withArgs();
              }
              // TODO: add support for n>1, mod not a power of two ...
              throw new GeneratorException(
                  "Modulus must be a power of two, or bit vector must fit in one word.");
            }
          }
        });

    // primRelaxIx n m :: Ix n -> Ix m    TODO: should be replaced by primIxInLo below ...
    generators.put(
        "primRelaxIx",
        new Generator(Prefix.nat_nat, fun(ixA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long n = ts[0].validIndex().longValue(); // Smaller index modulus
            long m = ts[1].validIndex().longValue(); // Larger index modulus
            if (n > m) {
              throw new GeneratorException("First argument must not be larger than the second");
            } else if (n == m || n > 2) {
              // TODO: the type checker will infer a *polymorphic* type for the identity function
              // used here, which could
              // trip up the LLVM code generator if it is expecting a monomorphic type (although
              // that may not happen
              // often in practice because this definition is likely to be inlined by the
              // optimizer).  (We could break
              // out separate cases here for Word -> Word, Flag-> Flag, and Unit->Unit ...)
              return new Return().makeUnaryFuncClosure(pos, 1);
            } else if (n == 2) { // :: Flag -> Word
              return new PrimCall(Prim.flagToWord).makeUnaryFuncClosure(pos, 1);
            } else if (m > 2) { // :: Unit -> Word
              return new Return(Word.Zero).constClosure(pos, Tycon.unitRep);
            } else /* n=1,m=2 */ { // :: Unit -> Flag
              return new Return(Flag.False).constClosure(pos, Tycon.unitRep);
            }
          }
        });

    // primIxInLo m s :: Ix m -> Ix (m + n), where s = m + n
    // primIxInLo i    = i
    generators.put(
        "primIxInLo",
        new Generator(Prefix.nat_nat, fun(ixA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Left hand index summand
            long s = ts[1].validIndex().longValue(); // Sum index
            if (m > s) {
              throw new GeneratorException("First index must not be larger than the second");
            } else if (m > 2
                || m == s) { // \i -> i (could be for Unit (m==1), Flag (m==2), or Word (m>2))
              return new Return().makeUnaryFuncClosure(pos, 1);
            } else if (m == 2) { // \i -> flagToWord i
              return new PrimCall(Prim.flagToWord).makeUnaryFuncClosure(pos, 1);
            } else if (s == 2) { // \i -> False :: Unit -> Flag  (s==1)
              return new Return(Flag.False).constClosure(pos, Tycon.unitRep);
            } else { // \i -> 0 :: Unit -> Word (m==1, s>2)
              return new Return(Word.Zero).constClosure(pos, Tycon.unitRep);
            }
          }
        });

    // primIxInHi m s :: Ix m -> Ix (n + m), where s = n + m
    // primIxInHi i    = i + n  (i.e., i + (s-m))
    generators.put(
        "primIxInHi",
        new Generator(Prefix.nat_nat, fun(ixA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Right hand index summand
            long s = ts[1].validIndex().longValue(); // Sum index
            if (m > s) {
              throw new GeneratorException("First index must not be larger than the second");
            }
            if (m <= 2 && m == s) { // \i -> i (could be for Unit (m==1) or Flag (m==2))
              return new Return().makeUnaryFuncClosure(pos, 1);
            } else if (m == 1) {
              if (s == 2) { // \i -> True :: Unit -> Flag
                return new Return(Flag.True).constClosure(pos, Tycon.unitRep);
              } else { // \i -> (s - m) :: Unit -> Word
                return new Return(new Word(s - m)).constClosure(pos, Tycon.unitRep);
              }
            } else { // \i -> i + (s-m)   (m>2 or (m==2 and s>2))
              Temp[] vs = Temp.makeTemps(1);
              Temp i = (m == 2) ? new Temp() : vs[0];
              Code code = new Done(Prim.add.withArgs(i, new Word(s - m)));
              if (m == 2) { // Special case for Ix 2 / Flag
                code = new Bind(i, Prim.flagToWord.withArgs(vs[0]), code);
              }
              return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primIxGenIsLo a m s :: a -> (Ix m -> a) -> Ix s -> a, where s >= m
    // primIxGenIsLo n j i  = if i < m then j i else n
    generators.put(
        "primIxGenIsLo",
        new Generator(Prefix.star_nat_nat, fun(gA, fun(ixB, gA), ixC, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Left hand summand index, will be > 0
            long s = ts[2].validIndex().longValue(); // Sum index, will be > 0, must be >=m
            if (m > s) {
              throw new GeneratorException("First index must not be larger than the second");
            }
            Temp[] ns = Temp.makeTemps(nl); // Parameters for n
            Temp[] ji = Temp.makeTemps(2); // Parameters for j and i
            Code code;
            if (s == 1) { // \n j i -> j Unit :: a -> (Unit -> a) -> Unit -> a
              Temp u = new Temp();
              code =
                  new Bind(
                      u,
                      Cfun.Unit.withArgs(), // u <- Unit()
                      new Done(new Enter(ji[0], u))); // j @ u

            } else if (m == 2 && s == 2) { // \n j i -> j i    :: a -> (Flag -> a) -> Flag -> a
              code = new Done(new Enter(ji[0], ji[1]));

            } else {
              // \n j i -> if bnot i then j Unit           else n :: a -> (Unit -> a) -> Flag -> a
              // (m=1, s=2)
              // \n j i -> if i<m    then j Unit           else n :: a -> (Unit -> a) -> Word -> a
              // (m=1, s>2)
              // \n j i -> if i<m    then j (wordToFlag i) else n :: a -> (Flag -> a) -> Word -> a
              // (m=2, s>2)
              // \n j i -> if i<m    then j i              else n :: a -> (Word -> a) -> Word -> a
              // (m>2, s>2)
              BlockCall no = new BlockCall(returnBlock(pos, nl), ns);

              BlockCall yes;
              if (m == 1) {
                Temp[] vs = Temp.makeTemps(1);
                Temp u = new Temp();
                yes =
                    new BlockCall(
                        new Block(
                            pos,
                            vs, // yes[j]
                            new Bind(
                                u,
                                Cfun.Unit.withArgs(), //   = u <- Unit()
                                new Done(new Enter(vs[0], u)))), //     j @ u
                        new Atom[] {ji[0]});
              } else if (m == 2) {
                Temp[] vs = Temp.makeTemps(2);
                Temp t = new Temp();
                yes =
                    new BlockCall(
                        new Block(
                            pos,
                            vs, // yes[j,i]
                            new Bind(
                                t,
                                Prim.wordToFlag.withArgs(vs[1]), //   = t <- wordToFlag((i))
                                new Done(new Enter(vs[0], t)))), //     j @ t
                        new Atom[] {ji[0], ji[1]});
              } else {
                yes = new BlockCall(enterBlock(pos), new Atom[] {ji[0], ji[1]});
              }

              Tail test = (s == 2) ? Prim.bnot.withArgs(ji[1]) : Prim.ult.withArgs(ji[1], m);
              Temp v = new Temp();
              code = new Bind(v, test, new If(v, yes, no));
            }
            return new BlockCall(new Block(pos, Temp.append(ns, ji), code))
                .makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primIxGenIsHi a m s :: a -> (Ix m -> a) -> Ix s -> a, where s >= m
    // primIxGenIsHi n j i  = if i >= (s-m) then j (i - (s-m)) else n
    generators.put(
        "primIxGenIsHi",
        new Generator(Prefix.star_nat_nat, fun(gA, fun(ixB, gA), ixC, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Left hand summand index, will be > 0
            long s = ts[2].validIndex().longValue(); // Sum index, will be > 0, must be >=m
            if (m > s) {
              throw new GeneratorException("First index must not be larger than the second");
            }
            Temp[] ns = Temp.makeTemps(nl); // Parameters for n
            Temp[] ji = Temp.makeTemps(2); // Parameters for j and i
            Code code;

            if (s == 1) { // \n j i -> j Unit :: a -> (Unit -> a) -> Unit -> a
              Temp u = new Temp();
              code =
                  new Bind(
                      u,
                      Cfun.Unit.withArgs(), // u <- Unit()
                      new Done(new Enter(ji[0], u))); // j @ u

            } else if (m == 2 && s == 2) { // \n j i -> j i    :: a -> (Flag -> a) -> Flag -> a
              code = new Done(new Enter(ji[0], ji[1]));

            } else {
              // \n j i -> if i        then j Unit                   else n :: a -> (Unit -> a) ->
              // Flag -> a (m=1, s=2)
              // \n j i -> if i>=(s-m) then j Unit                   else n :: a -> (Unit -> a) ->
              // Word -> a (m=1, s>2)
              // \n j i -> if i>=(s-m) then j (wordToFlag (i-(s-m))) else n :: a -> (Flag -> a) ->
              // Word -> a (m=2, s>2)
              // \n j i -> if i>=(s-m) then j (i-(s-m))              else n :: a -> (Word -> a) ->
              // Word -> a (m>2, s>2)
              BlockCall no = new BlockCall(returnBlock(pos, nl), ns);

              BlockCall yes;
              if (m == 1) {
                Temp[] vs = Temp.makeTemps(1);
                Temp u = new Temp();
                yes =
                    new BlockCall(
                        new Block(
                            pos,
                            vs, // yes[j]
                            new Bind(
                                u,
                                Cfun.Unit.withArgs(), //   = u <- Unit()
                                new Done(new Enter(vs[0], u)))), //     j @ u
                        new Atom[] {ji[0]});
              } else if (m == 2) {
                Temp[] vs = Temp.makeTemps(2);
                Temp t = new Temp();
                Temp u = new Temp();
                yes =
                    new BlockCall(
                        new Block(
                            pos,
                            vs, // yes[j,i]
                            new Bind(
                                u,
                                Prim.sub.withArgs(vs[1], s - m), //   = u <- sub((i, s-m))
                                new Bind(
                                    t,
                                    Prim.wordToFlag.withArgs(u), //     t <- wordToFlag((u))
                                    new Done(new Enter(vs[0], t))))), //     j @ t
                        new Atom[] {ji[0], ji[1]});
              } else {
                Temp[] vs = Temp.makeTemps(2);
                Temp t = new Temp();
                yes =
                    new BlockCall(
                        new Block(
                            pos,
                            vs, // yes[j,i]
                            new Bind(
                                t,
                                Prim.sub.withArgs(vs[1], s - m), //   = t <- sub((i, s-m))
                                new Done(new Enter(vs[0], t)))), //     j @ t
                        new Atom[] {ji[0], ji[1]});
              }

              if (s == 2) {
                code = new If(ji[1], yes, no);
              } else {
                Temp v = new Temp();
                code = new Bind(v, Prim.uge.withArgs(ji[1], s - m), new If(v, yes, no));
              }
            }
            return new BlockCall(new Block(pos, Temp.append(ns, ji), code))
                .makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primIxPair m n p :: Ix m -> Ix n -> Ix p, where p = m * n
    // primIxPair i j    = (i*n) + j
    generators.put(
        "primIxPair",
        new Generator(Prefix.nat_nat_nat, fun(ixA, ixB, ixC)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Left hand index factor
            long n = ts[1].validIndex().longValue(); // Right hand index factor
            long p = ts[2].validIndex().longValue(); // Product index factor
            if (m * n != p) {
              throw new GeneratorException(
                  "Result index must be the product of the argument index types");
            }
            Temp[] ij = Temp.makeTemps(2);
            Code code;
            if (m == 1) {
              code =
                  new Done(
                      (n == 1)
                          ? new DataAlloc(Cfun.Unit).withArgs() // \i j -> Unit (m==n==1)
                          : new Return(ij[1])); // \i j -> j    (m=1, n>1)
            } else if (n == 1) {
              code = new Done(new Return(ij[0])); // \i j -> i    (m>1, n=1)
            } else {
              Temp i = (m == 2) ? new Temp() : ij[0];
              Temp j = (n == 2) ? new Temp() : ij[1];
              Temp t = new Temp();
              code =
                  new Bind(
                      t,
                      Prim.mul.withArgs(i, new Word(n)), // \i j -> i*n + j
                      new Done(Prim.add.withArgs(t, j)));
              if (n == 2) { // Convert j from Flag to Word (n==2)
                code = new Bind(j, Prim.flagToWord.withArgs(ij[1]), code);
              }
              if (m == 2) { // Convert i from Flag to Word (m==2)
                code = new Bind(i, Prim.flagToWord.withArgs(ij[0]), code);
              }
            }
            return new BlockCall(new Block(pos, ij, code)).makeBinaryFuncClosure(pos, 1, 1);
          }
        });

    // primIxFst p m :: Ix (m * n) -> Ix m, where p = m * n
    // primIxFst i    = i `div` n
    // primIxFst (primIxPair i j) = (i*n+j) `div` n = i (because j<n)
    generators.put(
        "primIxFst",
        new Generator(Prefix.nat_nat, fun(ixA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long p = ts[0].validIndex().longValue(); // Product index
            long m = ts[1].validIndex().longValue(); // Left hand index factor
            long n = p / m; // Right hand index factor
            if (m * n != p) {
              throw new GeneratorException("First index must be a multiple of the second");
            } else if (m
                == 1) { // (\i -> Unit)  -- TODO: distinguish between types for i (Unit, Flag,
                        // Word)?
              return unaryUnit(pos);
            } else if (n
                == 1) { // (\i -> i)     -- TODO: distinguish between types for i (Flag, Word)?
              return new Return().makeUnaryFuncClosure(pos, 1);
            } else {
              Temp[] vs = Temp.makeTemps(1);
              Tail t = Prim.div.withArgs(vs[0], new Word(n));
              Code code;
              if (m == 2) {
                Temp v = new Temp();
                code = new Bind(v, t, new Done(Prim.wordToFlag.withArgs(v)));
              } else {
                code = new Done(t);
              }
              return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primIxSnd p n :: Ix (m * n) -> Ix n, where p = m * n
    // primIxSnd i    = i `mod` n
    // primIxSnd (primIxPair i j) = (i*n+j) `mod` n = j (because j<n)
    generators.put(
        "primIxSnd",
        new Generator(Prefix.nat_nat, fun(ixA, ixB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long p = ts[0].validIndex().longValue(); // Product index
            long n = ts[1].validIndex().longValue(); // Right hand index factor
            long m = p / n; // Left hand index factor
            if (m * n != p) {
              throw new GeneratorException("First index must be a multiple of the second");
            } else if (n
                == 1) { // (\i -> Unit)  -- TODO: distinguish between types for i (Unit, Flag,
                        // Word)?
              return unaryUnit(pos);
            } else if (m
                == 1) { // (\i -> i)     -- TODO: distinguish between types for i (Flag, Word)?
              return new Return().makeUnaryFuncClosure(pos, 1);
            } else {
              Temp[] vs = Temp.makeTemps(1);
              Tail t = Prim.rem.withArgs(vs[0], new Word(n));
              Code code;
              if (n == 2) {
                Temp v = new Temp();
                code = new Bind(v, t, new Done(Prim.wordToFlag.withArgs(v)));
              } else {
                code = new Done(t);
              }
              return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, 1);
            }
          }
        });

    // primGenLtInc a m :: a -> (Ix m -> a) -> Ix m -> Ix m -> Ix m -> a
    generators.put(
        "primGenLtInc",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), ixB, fun(ixB, ixB, gA))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus, will be > 0

            // The intended semantics for genLtInc is given by the following:
            //   genLtInc nothing just n i j = let k = i + n
            //                                 in if (i<=k) && (k<=j) then just k else nothing
            // with the assumption that the calculation of (i + n) does not produce any overflow.
            // The validIndex() test will ensure that m is in the range [1..maxSigned], and hence
            // that 0 <= n, i, j < maxSigned.  As a result, so long as we used an UNSIGNED
            // comparison,
            // we can be sure that (i + n) will not produce an overflow and that i <= k will be
            // trivially satisfied.  For that reason, the generated code only tests that k <= j.

            // Parameters for a block:   b[nothing,..., just, n, i, j] = ...
            Temp[] ns = Temp.makeTemps(nl); // arguments for nothing parameter
            Temp[] jnij = Temp.makeTemps(4); // arguments for just, n, i, j
            Code code;

            if (m == 1) { // :: a -> (Unit -> a) -> Unit -> Unit -> Unit -> a
              Temp u = new Temp();
              code =
                  new Bind(
                      u,
                      Cfun.Unit.withArgs(), // u <- Unit()
                      new Done(new Enter(jnij[0], u))); // just @ u

            } else if (m == 2) { // :: a -> (Flag -> a) -> Flag -> Flag -> Flag -> a
              Temp[] js = Temp.makeTemps(2);
              Temp s = new Temp(); // sum as a Flag
              Block yes =
                  new Block(
                      pos,
                      js, // yes[j, sw]
                      new Bind(
                          s,
                          Prim.wordToFlag.withArgs(js[1]), //   = s <- wordToFlag((sw))
                          new Done(new Enter(js[0], s)))); //     j @ s

              Temp nw = new Temp(); // n as a word
              Temp iw = new Temp(); // i as a word
              Temp kw = new Temp(); // sum (of n and i) as a word
              Temp jw = new Temp(); // j as a word
              Temp t = new Temp(); // result of comparing kw <= jw
              code =
                  new Bind(
                      nw,
                      Prim.flagToWord.withArgs(jnij[1]), // nw <- flagToWord((n))
                      new Bind(
                          iw,
                          Prim.flagToWord.withArgs(jnij[2]), // iw <- flagToWord((i))
                          new Bind(
                              kw,
                              Prim.add.withArgs(nw, iw), // kw <- add((nw, iw))
                              new Bind(
                                  jw,
                                  Prim.flagToWord.withArgs(jnij[3]), // jw <- flagToWord((j))
                                  new Bind(
                                      t,
                                      Prim.ule.withArgs(kw, jw), // t  <- ule((kw, jw))
                                      new If(
                                          t,
                                          new BlockCall(
                                              yes,
                                              new Atom[] {jnij[0], kw}), // if t then yes[j, kw]
                                          new BlockCall(
                                              returnBlock(pos, nl), ns))))))); //      else no[ns]

            } else { // :: a -> (Word -> a) -> Word -> Word -> Word -> a
              Block yes = enterBlock(pos); // yes[j, k] = j @ k
              Temp k = new Temp(); // sum of n + i
              Temp t = new Temp(); // result of comparing (n+i) <= j
              code =
                  new Bind(
                      k,
                      Prim.add.withArgs(jnij[2], jnij[1]), //   = k <- add((i, n))
                      new Bind(
                          t,
                          Prim.ule.withArgs(k, jnij[3]), //     t <- ule((k, j))
                          new If(
                              t,
                              new BlockCall(
                                  yes, new Atom[] {jnij[0], k}), //     if t then yes[just, k]
                              new BlockCall(
                                  returnBlock(pos, nl), ns)))); //          else no[nothing..]
            }
            return new BlockCall(new Block(pos, Temp.append(ns, jnij), code))
                .makeClosure(pos, nl + 3, 1)
                .makeClosure(pos, nl + 2, 1)
                .makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primGenLtDec a m :: a -> (Ix m -> a) -> Ix m -> Ix m -> Ix m -> a
    generators.put(
        "primGenLtDec",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), ixB, fun(ixB, ixB, gA))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus, will be > 0

            // The intended semantics for genLtDec is given by the following:
            //   genLtDec nothing just n i j = let k = j - n
            //                                 in if (i<=k) && (k<=j) then just k else nothing
            // with the assumption that the calculation of (j - n) does not produce any underflow.
            // The validIndex() test will ensure that m is in the range [1..maxSigned], and hence
            // that 0 <= n, i, j < maxSigned.  As a result, so long as we used an SIGNED comparison,
            // we can be sure that (j - n) will not produce an underflow and that k <= j will be
            // trivially satisfied.  For that reason, the generated code only tests that i <= k.

            // Parameters for a block:   b[nothing,..., just, n, i, j] = ...
            Temp[] ns = Temp.makeTemps(nl); // arguments for nothing parameter
            Temp[] jnij = Temp.makeTemps(4); // arguments for just, n, i, j
            Code code;

            if (m == 1) { // :: a -> (Unit -> a) -> Unit -> Unit -> Unit -> a
              Temp u = new Temp();
              code =
                  new Bind(
                      u,
                      Cfun.Unit.withArgs(), // u <- Unit()
                      new Done(new Enter(jnij[0], u))); // just @ u
            } else if (m == 2) { // :: a -> (Flag -> a) -> Flag -> Flag -> Flag -> a
              Temp[] jd = Temp.makeTemps(2);
              Temp d = new Temp(); // difference j-n as a Flag
              Block yes =
                  new Block(
                      pos,
                      jd, // yes[j, dw]
                      new Bind(
                          d,
                          Prim.wordToFlag.withArgs(jd[1]), //   = d <- wordToFlag((dw))
                          new Done(new Enter(jd[0], d)))); //     j @ d

              Temp jw = new Temp(); // j as a word
              Temp nw = new Temp(); // n as a word
              Temp kw = new Temp(); // difference (j - n) as a word
              Temp iw = new Temp(); // i as a word
              Temp t = new Temp(); // result of comparing iw <= kw
              code =
                  new Bind(
                      jw,
                      Prim.flagToWord.withArgs(jnij[3]), // jw <- flagToWord((j))
                      new Bind(
                          nw,
                          Prim.flagToWord.withArgs(jnij[1]), // nw <- flagToWord((n))
                          new Bind(
                              kw,
                              Prim.sub.withArgs(jw, nw), // kw <- sub((jw, nw))
                              new Bind(
                                  iw,
                                  Prim.flagToWord.withArgs(jnij[2]), // iw <- flagToWord((i))
                                  new Bind(
                                      t,
                                      Prim.sle.withArgs(iw, kw), // t  <- sle((iw, kw))
                                      new If(
                                          t,
                                          new BlockCall(
                                              yes,
                                              new Atom[] {jnij[0], kw}), // if t then yes[j, kw]
                                          new BlockCall(
                                              returnBlock(pos, nl), ns))))))); //      else no[ns]
            } else { // :: a -> (Word -> a) -> Word -> Word -> Word -> a
              Block yes = enterBlock(pos); // yes[j, k] = j @ k
              Temp k = new Temp(); // difference (j - n)
              Temp t = new Temp(); // result of comparing i <= k
              code =
                  new Bind(
                      k,
                      Prim.sub.withArgs(jnij[3], jnij[1]), // k <- sub((j, n))
                      new Bind(
                          t,
                          Prim.sle.withArgs(jnij[2], k), // t <- sle((i, k))
                          new If(
                              t,
                              new BlockCall(yes, new Atom[] {jnij[0], k}), // if t then yes[just, k]
                              new BlockCall(returnBlock(pos, nl), ns)))); //      else no[nothing..]
            }
            return new BlockCall(new Block(pos, Temp.append(ns, jnij), code))
                .makeClosure(pos, nl + 3, 1)
                .makeClosure(pos, nl + 2, 1)
                .makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primGenIncIx a m :: a -> (Ix m -> a) -> Ix m -> a
    generators.put(
        "primGenIncIx",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), ixB, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus, will be > 0
            Block b;
            if (m == 1) { // :: a -> (Unit -> a) -> Unit -> a
              Temp[] as = Temp.makeTemps(nl); // params for a
              Temp[] vs = Temp.append(as, Temp.makeTemps(2)); // add params for args 2 and 3
              b = new Block(pos, vs, new Done(new Return(as)));
            } else if (m == 2) { // :: a -> (Flag -> a) -> Flag -> a
              // inc[j] = j @ True
              Temp[] js = Temp.makeTemps(1);
              Block inc = new Block(pos, js, new Done(new Enter(js[0], Flag.True)));

              // b[n,..., j, i] = if i then no[n,...] else inc[j]
              Block no = returnBlock(pos, nl);
              Temp[] as = Temp.makeTemps(nl);
              Temp[] ji = Temp.makeTemps(2);
              b =
                  new Block(
                      pos,
                      Temp.append(as, ji),
                      new If(ji[1], new BlockCall(no, as), new BlockCall(inc, new Atom[] {ji[0]})));
            } else { // :: a -> (Word -> a) -> Word -> a
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
              b = guardBlock(pos, nji, Prim.ult.withArgs(nji[nl + 1], m - 1), inc);
            }
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primIncIx m :: Ix m -> Maybe (Ix m)
    // Special case: requires bitdataRepresentations, and m < 2^WordSize
    generators.put(
        "primIncIx",
        new Generator(Prefix.nat, fun(ixA, maybeIx)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Index modulus, must be > 0
            validBitdataRepresentations(); // ensure Maybe (Ix m) and Ix (m+1) have same
                                           // representation
            Temp[] vs = Temp.makeTemps(1);
            Code code;
            if (m == 1) {
              code = new Done(new Return(Flag.True));
            } else if (m == 2) {
              Temp t = new Temp();
              code =
                  new Bind(t, Prim.flagToWord.withArgs(vs[0]), new Done(Prim.add.withArgs(t, 1)));
            } else {
              code = new Done(Prim.add.withArgs(vs[0], 1));
            }
            return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primGenDecIx a m :: a -> (Ix m -> a) -> Ix m -> a
    generators.put(
        "primGenDecIx",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), ixB, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus, must be > 0
            Block b;
            if (m == 1) { // :: a -> (Unit -> a) -> Unit -> a
              Temp[] as = Temp.makeTemps(nl); // params for a
              Temp[] vs = Temp.append(as, Temp.makeTemps(2)); // add params for args 2 and 3
              b = new Block(pos, vs, new Done(new Return(as)));
            } else if (m == 2) { // :: a -> (Flag -> a) -> Flag -> a
              // dec[j] = j @ False
              Temp[] js = Temp.makeTemps(1);
              Block dec = new Block(pos, js, new Done(new Enter(js[0], Flag.False)));

              // b[n,..., j, i] = if i then dec[j] else no[n,...]
              Block no = returnBlock(pos, nl);
              Temp[] as = Temp.makeTemps(nl);
              Temp[] ji = Temp.makeTemps(2);
              b =
                  new Block(
                      pos,
                      Temp.append(as, ji),
                      new If(ji[1], new BlockCall(dec, new Atom[] {ji[0]}), new BlockCall(no, as)));
            } else {
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
              b = guardBlock(pos, nji, Prim.ugt.withArgs(nji[nl + 1], 0), dec);
            }
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primDecIx m :: Ix m -> Maybe (Ix m)
    // Special case: requires bitdataRepresentations, (m+1) is a power of 2, and m is a valid index
    // limit
    generators.put(
        "primDecIx",
        new Generator(Prefix.nat, fun(ixA, maybeIx)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Index modulus, must be > 0
            validBitdataRepresentations(); // ensure that Nothing is represented by all 1s
            Temp[] vs = Temp.makeTemps(1);
            Code code;
            if (m == 1) { // :: Unit -> Flag
              code = new Done(new Return(Flag.True));
            } else if (m == 2) { // :: Flag -> Word
              Temp w = new Temp();
              Temp t = new Temp();
              code =
                  new Bind(
                      w,
                      Prim.flagToWord.withArgs(vs[0]), // w <- flagToWord((v))
                      new Bind(
                          t,
                          Prim.add.withArgs(w, 3), // t <- add((w, 3)) -- 0 |-> 3, 1 |-> 4
                          new Done(
                              Prim.and.withArgs(t, 2)))); // and((t,2))       -- 3 |-> 2, 4 |-> 0
            } else if (((m + 1) & m)
                != 0) { // check for modulus with a successor that is a power of two
              throw new GeneratorException(m + " is not a power of two minus 1");
            } else {
              // In this special case, we can avoid a branching implementation by combining a
              // decrement with a mask:
              Temp w = new Temp();
              code = new Bind(w, Prim.sub.withArgs(vs[0], 1), new Done(Prim.and.withArgs(w, m)));
            }
            return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primMaybeIx a n :: a -> (Ix n -> a) -> Word -> a
    generators.put(
        "primGenMaybeIx",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), word, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Index modulus
            Block b;
            if (m == 1) { // :: a -> (Unit -> a) -> Word -> a
              Temp[] j = Temp.makeTemps(1);
              Temp t = new Temp();
              Block yes =
                  new Block(
                      pos,
                      j, // yes[j]
                      new Bind(
                          t,
                          Cfun.Unit.withArgs(), //   = t <- Unit()
                          new Done(new Enter(j[0], t)))); //     j @ t
              Temp[] as = Temp.makeTemps(nl);
              Temp[] jv = Temp.makeTemps(2);
              Temp w = new Temp();
              // b[as, j, v] = w <- eq((v, 0)); if w then yes[j] else no[as]
              b =
                  new Block(
                      pos,
                      Temp.append(as, jv),
                      new Bind(
                          w,
                          Prim.ule.withArgs(jv[1], 1),
                          new If(
                              w,
                              new BlockCall(yes, new Atom[] {jv[0]}),
                              new BlockCall(returnBlock(pos, nl), as))));
            } else if (m == 2) { // :: a -> (Flag -> a) -> Word -> a
              Temp[] jw = Temp.makeTemps(2);
              Temp t = new Temp();
              Block yes =
                  new Block(
                      pos,
                      jw, // yes[j, w]
                      new Bind(
                          t,
                          Prim.wordToFlag.withArgs(jw[1]), //   = t <- wordToFlag w
                          new Done(new Enter(jw[0], t)))); //     j @ t
              Temp[] as = Temp.makeTemps(nl);
              Temp[] jv = Temp.makeTemps(2);
              Temp w = new Temp();
              b =
                  new Block(
                      pos,
                      Temp.append(as, jv), // b[as, j, v]
                      new Bind(
                          w,
                          Prim.ult.withArgs(jv[1], 2), //   = w <- ult((v, 2))
                          new If(
                              w,
                              new BlockCall(yes, jv), //     if w then yes[j, v] else no[as]
                              new BlockCall(returnBlock(pos, nl), as))));
            } else { // :: a -> (Word -> a) -> Word -> a
              Block yes = enterBlock(pos); // yes[j, i] = j @ i
              // b[n,..., j, i] = w <- ule((i, m-1)); if w then yes[j, i] else return [n,...]
              // NOTE: using (v <= m-1) rather than (v < m) is important for the case where
              // m=(2^WordSize)
              Temp[] njv = Temp.makeTemps(nl + 2);
              b = guardBlock(pos, njv, Prim.ule.withArgs(njv[nl + 1], m - 1), yes);
            }
            return new BlockCall(b).makeTernaryFuncClosure(pos, nl, 1, 1);
          }
        });

    // primGenLeqIx a n :: a -> (Ix n -> a) -> Word -> Ix n -> a
    generators.put(
        "primGenLeqIx",
        new Generator(Prefix.star_nat, fun(gA, fun(ixB, gA), word, ixB, gA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int nl = ts[0].repLen(); // Find number of words to represent values of type a
            long m = ts[1].validIndex().longValue(); // Modulus of index
            Temp[] as = Temp.makeTemps(nl);
            Temp[] jvi = Temp.makeTemps(3);
            Code code;
            if (m == 1) { // :: a -> (Unit -> a) -> Word -> Unit -> a
              Temp[] j = Temp.makeTemps(1);
              Temp t = new Temp();
              Block yes =
                  new Block(
                      pos,
                      j, // yes[j]
                      new Bind(
                          t,
                          Cfun.Unit.withArgs(), //   = t <- Unit()
                          new Done(new Enter(j[0], t)))); //     j @ t
              Temp w = new Temp();
              code =
                  new Bind(
                      w,
                      Prim.ule.withArgs(jvi[1], 0), // w <- ule((v, 0))
                      new If(
                          w,
                          new BlockCall(yes, new Atom[] {jvi[0]}), // if w then yes[j]
                          new BlockCall(returnBlock(pos, nl), as))); //      else no[as]
            } else if (m == 2) { // :: a -> (Flag -> a) -> Word -> Flag -> a
              Temp[] jv = Temp.makeTemps(2);
              Temp t = new Temp();
              Block yes =
                  new Block(
                      pos,
                      jv, // yes[j, v]
                      new Bind(
                          t,
                          Prim.wordToFlag.withArgs(jv[1]), //   = t <- wordToFlag((v))
                          new Done(new Enter(jv[0], t)))); //     j @ t
              Temp u = new Temp();
              Temp w = new Temp();
              code =
                  new Bind(
                      u,
                      Prim.flagToWord.withArgs(jvi[2]), // u <- flagToWord((i))
                      new Bind(
                          w,
                          Prim.ule.withArgs(jvi[1], u), // w <- ule((v, u))
                          new If(
                              w,
                              new BlockCall(
                                  yes, new Atom[] {jvi[0], jvi[1]}), // if w then yes[j, v]
                              new BlockCall(returnBlock(pos, nl), as)))); //      else no[as]
            } else { // :: a -> (Word -> a) -> Word -> Word -> a
              Block yes = enterBlock(pos); // yes[j, i] = j @ i
              Temp w = new Temp();
              // b[n,..., j, v, i] = w <- ule((v, i)); if w then yes[j, v] else return [n,...]
              code =
                  new Bind(
                      w,
                      Prim.ule.withArgs(jvi[1], jvi[2]), // w <- ule((v, i))
                      new If(
                          w,
                          new BlockCall(yes, new Atom[] {jvi[0], jvi[1]}), // if w then yes[j, v]
                          new BlockCall(returnBlock(pos, nl), as))); //      else no[as]
            }
            Block b = new Block(pos, Temp.append(as, jvi), code);
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
        new Generator(Prefix.nat, fun(ixA, ixA, bool)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long m = ts[0].validIndex().longValue(); // Index upper bound
            if (m == 1) {
              return new Return(Flag.fromBool(cmp.op(0, 0)))
                  .constClosure(pos, 1)
                  .constClosure(pos, 1);
            } else if (m == 2) {
              Temp[] vs = Temp.makeTemps(2);
              Temp t = new Temp();
              Temp s = new Temp();
              Block b =
                  new Block(
                      pos,
                      vs,
                      new Bind(
                          t,
                          Prim.flagToWord.withArgs(vs[0]),
                          new Bind(
                              s, Prim.flagToWord.withArgs(vs[1]), new Done(cmp.withArgs(t, s)))));
              return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
            } else {
              return new PrimCall(cmp).makeBinaryFuncClosure(pos, 1, 1);
            }
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
        new Generator(Prefix.nat, bitAbitA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            return genBitwiseNotTail(pos, ts[0].validWidth());
          }
        });

    // primBitdataNot w :: bitdataType -> bitdataType
    generators.put(
        "primBitdataNot",
        new Generator(Prefix.star, gAgA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BitdataType bt = validBitdataType(ts[0]);
            checkBitdataNotRestricted(bt);
            return genBitwiseNotTail(pos, bt.getPat().getWidth());
          }
        });
  }

  /**
   * Generate an implementation for a bitwise not operation for a bitdata type of the given width
   * (assuming that such an operation makes sense for the bitdata type in question; that requires
   * additional checks).
   */
  static Tail genBitwiseNotTail(Position pos, int width) {
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
            code = new Bind(v, Prim.xor.withArgs(vs[n] = new Temp(), (1L << rem) - 1), code);
          }

          // Use Prim.not on any remaining words:
          while (n > 0) {
            Temp v = vs[--n];
            code = new Bind(v, Prim.not.withArgs(vs[n] = new Temp()), code);
          }
          return new BlockCall(new Block(pos, vs, code)).makeUnaryFuncClosure(pos, vs.length);
        }
    }
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
        new Generator(Prefix.nat, bitAbitAbitA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            return genBitwiseBinOpTail(pos, ts[0].validWidth(), p, pf);
          }
        });
  }

  static void genBitwiseBitdataBinOp(String ref, final PrimBinOp p, final PrimBinFOp pf) {
    // primBitRef w :: bitdataType -> bitdataType -> bitdataType
    generators.put(
        ref,
        new Generator(Prefix.star, gAgAgA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BitdataType bt = validBitdataType(ts[0]);
            checkBitdataNotRestricted(bt);
            return genBitwiseBinOpTail(pos, bt.getPat().getWidth(), p, pf);
          }
        });
  }

  /**
   * Generate an implementation for a bitwise binary operation for a bitdata type of the given width
   * (assuming that such an operation makes sense for the bitdata type in question; that requires
   * additional checks).
   */
  static Tail genBitwiseBinOpTail(Position pos, int width, final PrimBinOp p, final PrimBinFOp pf) {
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

  static {
    genBitwiseBinOp("primBitAnd", Prim.and, Prim.band);
    genBitwiseBinOp("primBitOr", Prim.or, Prim.bor);
    genBitwiseBinOp("primBitXor", Prim.xor, Prim.bxor);
    genBitwiseBitdataBinOp("primBitdataAnd", Prim.and, Prim.band);
    genBitwiseBitdataBinOp("primBitdataOr", Prim.or, Prim.bor);
    genBitwiseBitdataBinOp("primBitdataXor", Prim.xor, Prim.bxor);
  }

  static {

    // primBitNegate w :: Bit w -> Bit w
    generators.put(
        "primBitNegate",
        new Generator(Prefix.nat, bitAbitA) {
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
                  Temp[] args = Temp.makeTemps(Type.repBits(width));
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
          "Bit vector of width " + width + " does not fit in a single word");
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
        new Generator(Prefix.nat, bitAbitAbitA) {
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
    // primBitRef w :: Bit w -> Bit w -> Bool
    generators.put(
        ref,
        new Generator(Prefix.nat, bitAbitABool) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            return genBitwiseEqBinOpTail(pos, ts[0].validWidth(), test, bearly, pf, bz);
          }
        });
  }

  static void genEqBitdataBinOp(
      String ref, final PrimRelOp test, final Block bearly, final PrimBinFOp pf, final Block bz) {
    // primBitRef w :: bitdataType -> bitdataType -> Bool
    generators.put(
        ref,
        new Generator(Prefix.star, fun(gA, fun(gA, bool))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BitdataType bt = validBitdataType(ts[0]);
            return genBitwiseEqBinOpTail(pos, bt.getPat().getWidth(), test, bearly, pf, bz);
          }
        });
  }

  /**
   * Generate an implementation for an equality comparison on a bitdata type of the given width
   * (assuming that such an operation makes sense for the bitdata type in question; that requires
   * additional checks).
   */
  static Tail genBitwiseEqBinOpTail(
      Position pos,
      int width,
      final PrimRelOp test,
      final Block bearly,
      final PrimBinFOp pf,
      final Block bz) {
    switch (width) {
      case 0:
        return new BlockCall(bz).withArgs().constClosure(pos, 1).constClosure(pos, 1);

      case 1:
        return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

      default:
        {
          int n = Word.numWords(width);
          return new BlockCall(bitEqBlock(pos, n, test, bearly)).makeBinaryFuncClosure(pos, n, n);
        }
    }
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
    genEqBitdataBinOp("primBitdataEq", Prim.eq, Block.returnFalse, Prim.beq, Block.returnTrue);
    genEqBitdataBinOp("primBitdataNe", Prim.neq, Block.returnTrue, Prim.bxor, Block.returnFalse);
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
        new Generator(Prefix.nat, bitAbitABool) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return new BlockCall(bz)
                    .withArgs()
                    .constClosure(pos, Tycon.unitRep)
                    .constClosure(pos, Tycon.unitRep);

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
   * A general method for generating implementations for lexicographic orderings on SIGNED Bit
   * vector values. The The primitive pf is used in the special case for bit vectors of width 1, and
   * the bz block is used to generate code for width==0. TODO: Extend to work on widths outside the
   * range 0 <= width <= WordSize.
   */
  static void genSignedRelBinOp(
      String ref, final PrimRelOp comp, final PrimBinFOp pf, final Block bz) {
    // primBit... w ... :: Bit w -> Bit w -> Flag
    generators.put(
        ref,
        new Generator(Prefix.nat, bitAbitABool) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(); // Width of bit vector
            switch (width) {
              case 0:
                return new BlockCall(bz)
                    .withArgs()
                    .constClosure(pos, Tycon.unitRep)
                    .constClosure(pos, Tycon.unitRep);

              case 1:
                return new PrimCall(pf).makeBinaryFuncClosure(pos, 1, 1);

              default:
                {
                  validSingleWord(width);
                  int shift = Word.size() - width;
                  Call c;
                  if (shift > 0) {
                    Temp[] args = Temp.makeTemps(2);
                    Temp l = new Temp();
                    Temp r = new Temp();
                    return new BlockCall(
                            new Block(
                                pos,
                                args,
                                new Bind(
                                    l,
                                    Prim.shl.withArgs(args[0], shift),
                                    new Bind(
                                        r,
                                        Prim.shl.withArgs(args[1], shift),
                                        new Done(comp.withArgs(l, r))))))
                        .makeBinaryFuncClosure(pos, 1, 1);
                  }
                  return new PrimCall(comp).makeBinaryFuncClosure(pos, 1, 1);
                }
            }
          }
        });
  }

  static {
    genSignedRelBinOp("primBitSGt", Prim.sgt, Prim.bgt, Block.returnFalse);
    genSignedRelBinOp("primBitSGe", Prim.sge, Prim.bge, Block.returnTrue);
    genSignedRelBinOp("primBitSLt", Prim.slt, Prim.blt, Block.returnFalse);
    genSignedRelBinOp("primBitSLe", Prim.sle, Prim.ble, Block.returnTrue);
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
    protected BitPosGenerator(Prefix prefix, Type type) {
      super(prefix, type);
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
    protected BitmanipGenerator(Prefix prefix, Type type) {
      super(prefix, type);
    }

    /**
     * Generator for a Bitmanip operation, distinguishing between cases for width==1, width==2, and
     * width>2.
     */
    Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
      int width = ts[0].validWidth(1); // Width of bit vector
      return (width == 1)
          ? generate1(pos)
          : (width == 2) ? generate2(pos) : generateN(pos, width, Word.numWords(width));
    }

    /** Generate special case code for a bit vector of width 1. */
    abstract Tail generate1(Position pos);

    /** Generate special case code for a bit vector of width 2. */
    abstract Tail generate2(Position pos);

    /** Generate special case code for a bit vector of width N>2. */
    abstract Tail generateN(Position pos, int width, int n);

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
     * arguments to the enclosing block; m is a previously calculated mask for the relevant bit
     * within the selected word; n is the number of words in the representation for the Bit vector;
     * and lo is the index of the word containing the relevant bit.
     */
    abstract Code makeResult(Temp[] vs, Atom mask, int n, int lo);
  }

  protected abstract static class ConstructBitmanipGenerator extends BitmanipGenerator {

    /** Default constructor. */
    protected ConstructBitmanipGenerator(Prefix prefix, Type type) {
      super(prefix, type);
    }

    Tail generateN(Position pos, int width, int n) {
      return new BlockCall(decisionTree(pos, width, n, 0, n - 1, 0)).makeUnaryFuncClosure(pos, 1);
    }
  }

  protected abstract static class ConsumeBitmanipGenerator extends BitmanipGenerator {

    /** Default constructor. */
    protected ConsumeBitmanipGenerator(Prefix prefix, Type type) {
      super(prefix, type);
    }

    Tail generateN(Position pos, int width, int n) {
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
        new ConstructBitmanipGenerator(Prefix.nat, fun(ixA, bitA)) {
          Tail generate1(Position pos) { // \i -> True
            return new Return(Flag.True).constClosure(pos, Tycon.unitRep);
          }

          Tail generate2(Position pos) { // \i -> (1 << flagToWord i)
            Temp[] i = Temp.makeTemps(Tycon.flagRep);
            Temp t = new Temp();
            Block b =
                new Block(
                    pos,
                    i,
                    new Bind(t, Prim.flagToWord.withArgs(i[0]), new Done(Prim.shl.withArgs(1, t))));
            return new BlockCall(b).makeUnaryFuncClosure(pos, 1);
          }

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
        new ConsumeBitmanipGenerator(Prefix.nat, bitAixAbitA) {
          Tail generate1(Position pos) { // \v i -> True
            return new Return(Flag.True)
                .constClosure(pos, Tycon.unitRep)
                .constClosure(pos, Tycon.flagRep);
          }

          Tail generate2(Position pos) { // \v i -> v `or` (1 << flagToWord i)
            Temp[] vi = Temp.makeTemps(2);
            Temp t = new Temp();
            Temp m = new Temp();
            Block b =
                new Block(
                    pos,
                    vi, // b[v, i]
                    new Bind(
                        t,
                        Prim.flagToWord.withArgs(vi[1]), //    = t <- flagToWord((i))
                        new Bind(
                            m,
                            Prim.shl.withArgs(1, t), //      m <- shl((1, t))
                            new Done(Prim.or.withArgs(vi[0], m))))); //      or((v, m))
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

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
        new ConsumeBitmanipGenerator(Prefix.nat, bitAixAbitA) {
          Tail generate1(Position pos) { // \v i -> False
            return new Return(Flag.False)
                .constClosure(pos, Tycon.unitRep)
                .constClosure(pos, Tycon.flagRep);
          }

          Tail generate2(Position pos) { // \v i -> v & not (1 << flagToWord i)
            Temp[] vi = Temp.makeTemps(2);
            Temp t = new Temp();
            Temp m = new Temp();
            Temp r = new Temp();
            Block b =
                new Block(
                    pos,
                    vi, // b[v, i]
                    new Bind(
                        t,
                        Prim.flagToWord.withArgs(vi[1]), //    = t <- flagToWord((i))
                        new Bind(
                            m,
                            Prim.shl.withArgs(1, t), //      m <- shl((1, t))
                            new Bind(
                                r,
                                Prim.not.withArgs(m), //      r <- not((m))
                                new Done(Prim.and.withArgs(vi[0], r)))))); //      and((v, r))
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

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
        new ConsumeBitmanipGenerator(Prefix.nat, bitAixAbitA) {
          Tail generate1(Position pos) { // \v i -> bnot v
            Temp[] vi = Temp.makeTemps(2);
            Block b = new Block(pos, vi, new Done(Prim.bnot.withArgs(vi[0])));
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

          Tail generate2(Position pos) { // \v i -> v `xor` (1 << flagToWord i)
            Temp[] vi = Temp.makeTemps(2);
            Temp t = new Temp();
            Temp m = new Temp();
            Block b =
                new Block(
                    pos,
                    vi, // b[v, i]
                    new Bind(
                        t,
                        Prim.flagToWord.withArgs(vi[1]), //    = t <- flagToWord((i))
                        new Bind(
                            m,
                            Prim.shl.withArgs(1, t), //      m <- shl((1, t))
                            new Done(Prim.xor.withArgs(vi[0], m))))); //      xor((v, m))
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

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
        new ConsumeBitmanipGenerator(Prefix.nat, fun(bitA, ixA, bool)) {
          Tail generate1(Position pos) { // \v i -> v
            Temp[] vi = Temp.makeTemps(2);
            Block b = new Block(pos, vi, new Done(new Return(vi[0])));
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

          Tail generate2(Position pos) { // \v i -> (v `and` (1 << flagToWord i)) `neq` 0
            Temp[] vi = Temp.makeTemps(2);
            Temp t = new Temp();
            Temp m = new Temp();
            Temp r = new Temp();
            Block b =
                new Block(
                    pos,
                    vi, // b[v, i]
                    new Bind(
                        t,
                        Prim.flagToWord.withArgs(vi[1]), //    = t <- flagToWord((i))
                        new Bind(
                            m,
                            Prim.shl.withArgs(1, t), //      m <- shl((1, t))
                            new Bind(
                                r,
                                Prim.and.withArgs(vi[0], m), //      r <- and((v, m))
                                new Done(Prim.neq.withArgs(r, 0)))))); //      neq((r, 0))
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }

          Code makeResult(Temp[] vs, Atom mask, int n, int lo) {
            Temp w = new Temp();
            return new Bind(
                w, Prim.and.withArgs(mask, vs[lo]), new Done(Prim.neq.withArgs(w, Word.Zero)));
          }
        });

    generators.put(
        "primBitBitSize",
        new Generator(Prefix.nat, fun(bitA, ixA)) { // :: Bit w -> Ix w
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(1); // Bit vector width
            Tail t =
                (width == 1)
                    ? Cfun.Unit.withArgs()
                    : (width == 2) ? new Return(Flag.True) : new Return(new Word(width - 1));
            // TODO: The Temp.makeTemps(n) call in the following creates the proxy argument of type
            // Bit w that is
            // required as an input for this function (to avoid ambiguity).  Because it is not
            // actually used,
            // however, it will result in a polymorphic definition in post-specialization code,
            // which may break
            // subsequent attempts to generate code from monomorphic MIL code ... unless this
            // definition is
            // optimized away (which, it should be ... assuming that the optimizer is invoked ...)
            ClosureDefn k =
                new ClosureDefn(pos, Temp.noTemps, Temp.makeTemps(Type.repBits(width)), t);
            return new ClosAlloc(k).withArgs();
          }
        });
  }

  static {

    // primBitShiftL w :: Bit w -> Ix w -> Bit w,  for w>=1
    generators.put(
        "primBitShiftL",
        new BitPosGenerator(Prefix.nat, bitAixAbitA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(1); // Width of bit vector
            int n = Word.numWords(width);
            Call call;
            if (width == 1) { // :: Flag -> Unit -> Flag
              Temp[] vi = Temp.makeTemps(2);
              call = new BlockCall(new Block(pos, vi, new Done(new Return(vi[0]))));
            } else if (width == 2) { // :: Bit 2 -> Flag -> Bit 2
              Temp[] vi = Temp.makeTemps(2);
              Temp s = new Temp();
              Temp r = new Temp();
              call =
                  new BlockCall(
                      new Block(
                          pos,
                          vi, // b[v, i]
                          new Bind(
                              s,
                              Prim.flagToWord.withArgs(vi[1]), //   = s <- flagToWord((i))
                              new Bind(
                                  r,
                                  Prim.shl.withArgs(vi[0], s), //     r <- shl((v, s))
                                  new Done(Prim.and.withArgs(r, 3)))))); //     and((r, 3))
            } else { // :: Bit w -> Word -> Bit w
              call = new BlockCall(decisionTree(pos, width, n, 0, n - 1, n));
            }
            return call.makeBinaryFuncClosure(pos, n, 1);
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

    // primBitShiftRu w :: Bit w -> Ix w -> Bit w,  for w>=1
    generators.put(
        "primBitShiftRu",
        new BitPosGenerator(Prefix.nat, bitAixAbitA) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validWidth(1); // Width of bit vector
            int n = Word.numWords(width);
            Call call;
            if (width == 1) { // :: Flag -> Unit -> Flag
              Temp[] vi = Temp.makeTemps(2);
              call = new BlockCall(new Block(pos, vi, new Done(new Return(vi[0]))));
            } else if (width == 2) { // :: Bit 2 -> Flag -> Bit 2
              Temp[] vi = Temp.makeTemps(2);
              Temp s = new Temp();
              call =
                  new BlockCall(
                      new Block(
                          pos,
                          vi, // b[v, i]
                          new Bind(
                              s,
                              Prim.flagToWord.withArgs(vi[1]), //   = s <- flagToWord((i))
                              new Done(Prim.lshr.withArgs(vi[0], s))))); //     lshr((v, s))
            } else { // :: Bit w -> Word -> Bit w
              call = new BlockCall(decisionTree(pos, width, n, 0, n - 1, n));
            }
            return call.makeBinaryFuncClosure(pos, n, 1);
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

    // primNZBitFromLiteral v w ... :: ProxyNat v -> NZBit w
    generators.put(
        "primNZBitFromLiteral",
        new Generator(Prefix.nat_nat, fun(natA, Type.nzbit(gB))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            BigInteger v = ts[0].validNat(); // Value of literal (must be nonzero!)
            int w = ts[1].validWidth(); // Width of bit vector
            if (v.signum() <= 0) {
              throw new GeneratorException("A positive value is required");
            }
            validSingleWord(w);
            Type.validBelow(v, BigInteger.ONE.shiftLeft(w)); // v < 2 ^ w
            ClosureDefn k =
                new ClosureDefn(
                    pos,
                    Temp.noTemps,
                    Temp.makeTemps(Tycon.unitRep),
                    new Return(new Word(v.longValue()))); //  k{} _ = return [v]
            return new ClosAlloc(k).withArgs();
          }
        });

    // primNZBitForget w :: NZBit w -> Bit w
    generators.put(
        "primNZBitForget",
        new Generator(Prefix.nat, fun(Type.nzbit(gA), bitA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            validSingleWord(
                ts[0].validWidth(2)); // bit vector width (must fit within a single word)
            return new Return().makeUnaryFuncClosure(pos, 1);
          }
        });

    // primNZBitNonZero w :: Bit w -> Maybe (NZBit w)
    generators.put(
        "primNZBitNonZero",
        new Generator(Prefix.nat, fun(bitA, maybeIx)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            validSingleWord(
                ts[0].validWidth(2)); // bit vector width (must fit within a single word)
            validBitdataRepresentations(); // ensures same rep for Maybe (NZBit w), Bit w.
            return new Return().makeUnaryFuncClosure(pos, 1);
          }
        });

    // primNZBitDiv w :: Bit w -> NZBit w -> Bit w
    genNZDivOp("primNZBitDiv", Prim.nzdiv);

    // primNZBitRem w :: Bit w -> NZBit w -> Bit w
    genNZDivOp("primNZBitRem", Prim.nzrem);
  }

  static void genNZDivOp(String ref, final Prim p) {
    generators.put(
        ref,
        new Generator(Prefix.nat, fun(bitA, Type.nzbit(gA), bitA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            validSingleWord(
                ts[0].validWidth(2)); // bit vector width (must fit within a single word)
            return new PrimCall(p.canonPrim(set)).makeBinaryFuncClosure(pos, 1, 1);
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
        new Generator(Prefix.star, fun(Type.ref(Type.stored(gA)), Type.proc(gA))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            int width = ts[0].validMemBitSize();
            if (width == 0) {
              return new DataAlloc(Cfun.Unit).withArgs().constClosure(pos, 0).constClosure(pos, 1);
            }
            Prim load = findStoredAccess(width).load; // select appropriate load primitive
            Temp[] vs = Temp.makeTemps(Tycon.wordRep);
            ClosureDefn k = new ClosureDefn(pos, vs, Temp.noTemps, load.repTransformPrim(set, vs));
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, 1);
          }
        });

    // primWriteRefStored t :: Ref (Stored t) -> t -> Proc Unit
    generators.put(
        "primWriteRefStored",
        new Generator(Prefix.star, fun(Type.ref(Type.stored(gA)), gA, Type.proc(unit))) {
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
        new Generator(Prefix.star, fun(gA, Type.init(Type.stored(gA)))) {
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

    // primInitSelf t :: (Ref a -> Init a) -> Init a
    generators.put(
        "primInitSelf",
        new Generator(Prefix.area, fun(fun(refA, initA), initA)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            Temp[] fr = Temp.makeTemps(2);
            Temp g = new Temp();
            Block b =
                new Block(
                    pos, fr, new Bind(g, new Enter(fr[0], fr[1]), new Done(new Enter(g, fr[1]))));
            return new BlockCall(b).makeBinaryFuncClosure(pos, 1, 1);
          }
        });

    // primReInit t :: Ref a -> Init a -> Proc Unit
    generators.put(
        "primReInit",
        new Generator(Prefix.area, fun(refA, fun(initA, Type.proc(unit)))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            Temp[] ri = Temp.makeTemps(2);
            ClosureDefn k = new ClosureDefn(pos, ri, Temp.noTemps, new Enter(ri[1], ri[0]));
            return new ClosAlloc(k).makeBinaryFuncClosure(pos, 1, 1);
          }
        });
  }

  static {

    // primAt n a :: Ref (Array n a) -> Ix n -> Ref a
    generators.put(
        "primAt",
        new Generator(Prefix.nat_area, fun(Type.ref(arrayAB), ixA, refB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long n = ts[0].validIndex().longValue(); // Array length/Modulus for index type
            long size = ts[1].validArrayArea(); // Size of array elements
            Temp[] ri = Temp.makeTemps(2);
            Code code;
            if (n == 1) { // :: Ref 1 a -> Unit -> Ref a
              code = new Done(new Return(ri[0])); // return r
            } else {
              Temp v = new Temp();
              code = new Done(Prim.add.withArgs(ri[0], v)); // add((r, v))
              if (n == 2) { // :: Ref 2 a -> Flag -> Ref a
                Temp t = new Temp();
                code =
                    new Bind(
                        t,
                        Prim.flagToWord.withArgs(ri[1]), // t <- flagToWord((i))
                        new Bind(v, Prim.mul.withArgs(t, size), code)); // v <- mul((t, size))
              } else { // :: Ref n a -> Word -> Ref a
                code = new Bind(v, Prim.mul.withArgs(ri[1], size), code); // v <- mul((i, size))
              }
            }
            return new BlockCall(new Block(pos, ri, code)).makeBinaryFuncClosure(pos, 1, 1);
          }
        });
  }

  static {

    // primInitArray n a :: (Ix n -> Init a) -> Init (Array n a)
    generators.put(
        "primInitArray",
        new Generator(Prefix.nat_area, fun(fun(ixA, Type.init(gB)), Type.init(arrayAB))) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long len = ts[0].validIndex().longValue(); // Array length/Modulus for index type
            long size = ts[1].validArrayArea(); // Size of array elements
            return (len <= 4)
                ? initArrayUnroll(pos, len, size) // TODO: 4 is an arbitrary choice here ...
                : initArrayLoop(pos, len, size);
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
        new ClosureDefn(pos, f, r, new BlockCall(loop, new Atom[] {f[0], r[0], Word.Zero}));
    return new ClosAlloc(k1).makeUnaryFuncClosure(pos, 1);
  }

  /**
   * Generate code for an implementation of initArray that uses a separate section of code for every
   * array element, essentially unrolling the loop that is produced by initArrayLoop. This will
   * likely produce a lot of code unless the array length is small. The structure of the generated
   * code is as follows:
   *
   * <p>initArray <- k0{} k0{} f = k1{f} k1{f} r = work[f, r] work[f, r] = u <- Unit() g0 <- f @ 0
   * x0 <- f0 @ r //... r1 <- add((r0, size)) g1 <- f @ 1 x1 <- g1 @ r1 //... return u
   */
  private static Tail initArrayUnroll(Position pos, long len, long size) {
    Temp unit = new Temp();
    Code code = new Done(new Return(unit));
    Temp f = new Temp();
    Temp r = new Temp();
    if (len > 0) {
      long i = len - 1;
      for (; ; ) {
        Temp g = new Temp();
        Atom a = (len == 1) ? unit : (len == 2) ? Flag.fromBool(i > 0) : new Word(i);
        code = new Bind(g, new Enter(f, a), new Bind(new Temp(), new Enter(g, r), code));
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
    return new BlockCall(
            new Block(pos, new Temp[] {f, r}, new Bind(unit, Cfun.Unit.withArgs(), code)))
        .makeBinaryFuncClosure(pos, 1, 1);
  }

  static {

    // primInitPad n a :: Init (Pad n a)
    generators.put(
        "primInitPad",
        new Generator(Prefix.nat_area, Type.init(padAB)) {
          Tail generate(Position pos, Type[] ts, RepTypeSet set) throws GeneratorException {
            long len = ts[0].validIndex().longValue(); // Array length/Modulus for index type
            long size = ts[1].validArrayArea(); // Size of array elements
            return unaryUnit(pos); // A pad doesn't need initialization
          }
        });
  }

  static {

    // primStructSelect st lab t :: Ref st -> ProxyLab lab -> Ref t
    generators.put(
        "primStructSelect",
        new Generator(Prefix.area_lab_area, fun(refA, Type.lab(gB), refC)) {
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
            // TODO: should check type in ts[2] ...
            Temp[] vs = Temp.makeTemps(Tycon.wordRep);
            Tail tail = fields[i].getSelectPrim().repTransformPrim(set, vs);
            ClosureDefn k = new ClosureDefn(pos, vs, Temp.makeTemps(Tycon.unitRep), tail);
            return new ClosAlloc(k).makeUnaryFuncClosure(pos, 1);
          }
        });
  }
}
