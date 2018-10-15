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

public class MILParser extends CoreParser implements MILTokens {

  private MILLoader loader;

  /** Default constructor. */
  public MILParser(Handler handler, MILLexer lexer, MILLoader loader) {
    super(handler, lexer);
    this.loader = loader;
  }

  public void parse(MILAST ast) throws Failure {
    lexer.enterSection();
    for (; ; ) {
      if (lexer.getToken() == SEMI) {
        lexer.nextToken(/* ; */ );
      } else {
        try {
          CoreDefn defn = maybeCoreDefn();
          if (defn != null) {
            ast.add(defn);
          } else if (lexer.getToken() == REQUIRE) {
            if (lexer.nextToken(/* REQUIRE */ ) != STRLIT) {
              throw new Failure(lexer.getPos(), "Missing file name for require declaration");
            }
            String name = lexer.getLexeme();
            if (name.endsWith(".mil") || name.endsWith(".lmil")) {
              ast.requires(loader.require(name));
            } else {
              handler.report(
                  new Failure(
                      lexer.getPos(),
                      "Filename \""
                          + name
                          + "\" does not end with \".mil\" (or \".lmil\") extension"));
            }
            lexer.nextToken(/* STRLIT */ );
            lexer.itemEnd("require declaration");
          } else {
            if (!maybeParseDefn(ast)) {
              lexer.leaveSection();
              checkForEnd();
              return;
            }
          }
        } catch (Failure f) {
          report(f);
          lexer.itemEnd("definition");
        }
      }
    }
  }

  private boolean maybeParseDefn(MILAST prog) throws Failure {
    Position pos = lexer.getPos();
    switch (lexer.getToken()) {
      case PRIMITIVE:
        {
          prog.add(primDefn(pos));
          lexer.itemEnd("primitive definition");
          return true;
        }

        // TODO: consolidate EXPORT and ENTRYPOINT with similar code in lc ...
      case EXPORT:
        {
          lexer.nextToken(/* EXPORT */ );
          String[] ids = parseIds1();
          prog.add(new Export(pos, ids));
          if (lexer.getToken() == COCO) {
            lexer.nextToken(/* COCO */ );
            prog.add(parseTypeAnn(pos, ids));
          }
          lexer.itemEnd("export declaration");
          return true;
        }

      case ENTRYPOINT:
        {
          lexer.nextToken(/* ENTRYPOINT */ );
          String[] ids = parseIds1();
          prog.add(new Entrypoint(pos, ids));
          lexer.itemEnd("entrypoint declaration");
          return true;
        }

      case SOPEN:
        { // Top-level definition, multiple vars
          lexer.nextToken(/* [ */ );
          String[] ids = parseIds();
          require(SCLOSE);
          require(FROM);
          prog.add(parseTopLevel(pos, ids));
          return true;
        }

      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String id = lexer.getLexeme();
          switch (lexer.nextToken(/* var|con */ )) {
            case COMMA:
              {
                String[] ids = parseIds(id, 1);
                require(COCO);
                prog.add(parseTypeAnn(pos, ids));
                return true;
              }

            case COCO:
              lexer.nextToken(/* COCO */ );
              prog.add(parseTypeAnn(pos, new String[] {id}));
              return true;

            case SOPEN:
              { // Block definition
                lexer.nextToken(/* [ */ );
                String[] ids = parseIds();
                require(SCLOSE);
                require(EQ);
                prog.add(new BlockDefnExp(pos, id, ids, parseCode()));
                lexer.itemEnd("block definition");
                return true;
              }

            case BOPEN:
              { // Closure definition
                lexer.nextToken(/* open brace */ );
                String[] ids = parseIds();
                require(BCLOSE);
                String[] args = parseIdsTuple();
                require(EQ);
                prog.add(new ClosureDefnExp(pos, id, ids, args, parseCode()));
                lexer.itemEnd("closure definition");
                return true;
              }

            case FROM:
              { // TopLevel definition
                lexer.nextToken(/* <- */ );
                prog.add(parseTopLevel(pos, new String[] {id}));
                return true;
              }
          }
          throw new Failure(lexer.getPos(), "Syntax error in definition");
        }
    }
    return false;
  }

  private DefnExp parseTopLevel(Position pos, String[] ids) throws Failure {
    if (lexer.match(AREA)) {
      if (ids.length != 1) {
        throw new Failure(lexer.getPos(), "An area definition can only bind a single identifier");
      }
      TypeExp typeExp = typeAtomExp(); // Read expression describing area type
      AtomExp init = parseAtom(); // Read initializer expression
      TypeExp alignExp = lexer.match(ALIGNED) ? typeExp() : null; // Read (optional) alignment
      lexer.itemEnd("area definition");
      return new AreaDefnExp(pos, ids[0], typeExp, init, alignExp);
    } else if (lexer.getToken() == STRLIT) {
      if (ids.length != 1) {
        throw new Failure(
            lexer.getPos(), "A string area definition can only bind a single identifier");
      }
      String str = lexer.getLexeme();
      lexer.nextToken(/* STRLIT */ );
      lexer.itemEnd("string area definition");
      return new StringAreaDefnExp(pos, ids[0], str);
    } else {
      String[] args;
      if (lexer.match(BOPEN)) {
        args = parseIds();
        require(BCLOSE);
      } else {
        args = new String[0];
      }
      TopLevelExp tle = new TopLevelExp(pos, ids, args, parseCode());
      lexer.itemEnd("top-level definition");
      return tle;
    }
  }

  /**
   * Parse a primitive definition, having just found (but not yet skipped) the initial PRIMITIVE
   * token that begins the definition. The general syntax is as follows:
   *
   * <p>PRIMITIVE name [purity] :: domtuple >>= rngtuple
   *
   * <p>where:
   *
   * <p>name is a var (a VARID or VARSYM).
   *
   * <p>purity is an optional purity label, which is an optional VARID whose text is one of: pure,
   * observer, volatile, impure, or doesntReturn. If missing, then a default of pure is assumed.
   * (See mil.Prim for more details.)
   *
   * <p>domtuple is a tuple type expression that specifies the types for the inputs to the
   * primitive.
   *
   * <p>rngtuple is a tuple type expression that specifies the types for the outputs of the
   * primitive.
   */
  private PrimDefnExp primDefn(Position pos) throws Failure {
    int token = lexer.nextToken(/* PRIMITIVE */ );
    if (token != VARID && token != VARSYM) {
      throw new Failure(lexer.getPos(), "Missing identifier in primitive definition");
    }
    String id = lexer.getLexeme();
    lexer.nextToken(/* var */ );
    int purity = Prim.PURE; // set default purity
    if (lexer.getToken() == VARID) { // look for a purity label
      if ((purity = Prim.purityFromLabel(lexer.getLexeme())) < 0) {
        throw new Failure(
            lexer.getPos(), "Unknown purity label " + lexer.getLexeme() + " for primitive " + id);
      }
      lexer.nextToken(/* VARID */ );
    }
    boolean makechain = false;
    boolean thunk = false;
    if (lexer.match(BOPEN)) {
      require(BCLOSE);
      makechain = true; // request closure chain generation
      if (lexer.match(SOPEN)) {
        require(SCLOSE);
        thunk = true; // add thunking layer to closure chain
      }
    }
    if (!lexer.match(COCO)) {
      throw new Failure(lexer.getPos(), "Missing :: in primitive definition");
    }
    TupleTypeExp domtuple = parseTupleTypeExp();
    if (!lexer.match(TBIND)) {
      throw new Failure(lexer.getPos(), "Missing >>= in primitive definition");
    }
    TupleTypeExp rngtuple = parseTupleTypeExp();
    return new PrimDefnExp(pos, id, purity, makechain, thunk, domtuple, rngtuple);
  }

  /**
   * Parse a type annotation for some block, closure, or top level definitions with COCO as the
   * current token. The general form is either: v1, ..., vn :: inputs >>= outputs for blocks with
   * type tuples of inputs and outputs OR: v1, ..., vn :: {s1,...} t for types of stored fields
   * given by s1 and t the type of the resulting closure (this will be a function type, although we
   * enforce that during type checking rather than parsing); OR: v1, ..., vn :: t for top-level
   * values with the specified type.
   */
  private TypeAnn parseTypeAnn(Position pos, String[] ids) throws Failure {
    if (lexer.getToken() == BOPEN) { // opening brace ==> allocator type
      lexer.nextToken(/* BOPEN */ );
      TypeExp[] stored = parseBracketedTypes(BCLOSE);
      TypeExp type = typeExp();
      lexer.itemEnd("closure type annotation");
      return new ClosureTypeAnn(pos, ids, stored, type);
    } else {
      TypeExp t1 = typeExp(); // read a type
      if (lexer.match(TBIND)) { // initial part of a block type?
        TypeExp t2 = typeExp();
        lexer.itemEnd("block type annotation");
        return new BlockTypeAnn(pos, ids, t1, t2);
      }
      lexer.itemEnd("top level type annotation");
      return new TopTypeAnn(pos, ids, t1);
    }
  }

  /**
   * Parse a Code sequence. TODO: ?allow bindings with a wildcard on the lhs, as in _ <- t (work
   * around is to use a variable that isn't used elsewhere)
   */
  private CodeExp parseCode() throws Failure {
    Position pos = lexer.getPos();
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String id = lexer.getLexeme();
          switch (lexer.nextToken(/* var */ )) {
            case FROM:
              lexer.nextToken(/* FROM */ );
              return parseBind(pos, new String[] {id});
            default:
              return new DoneExp(pos, parseTailId(pos, id));
          }
        }

      case SOPEN:
        {
          lexer.nextToken(/* SOPEN */ );
          String[] ids = parseIds();
          require(SCLOSE);
          require(FROM);
          return parseBind(pos, ids);
        }

      case RETURN:
        return new DoneExp(pos, parseTailReturn());
      case CASE:
        return parseCodeCase(pos);
      case IF:
        return parseCodeIf(pos);
      case ASSERT:
        return parseCodeAssert(pos);
      default:
        return parseCodeError(pos);
    }
  }

  /** Parse a bind expression, having already read the initial "ids <-" portion. */
  private CodeExp parseBind(Position pos, String[] ids) throws Failure {
    TailExp t = parseTail();
    if (lexer.getToken() == SEMI) { // skip optional semicolon
      lexer.nextToken(/* SEMI */ );
    }
    return new BindExp(pos, ids, t, parseCode());
  }

  /** Parse a Case construct at the end of a Code sequence. */
  private CodeExp parseCodeCase(Position pos) throws Failure {
    lexer.nextToken(/* CASE */ );
    AtomExp a = parseAtom();
    require(OF);
    AltExp[] alts = parseAlts(0);
    BlockCallExp def = null;
    if (lexer.getToken() == UNDER) {
      lexer.nextToken(/* UNDER */ );
      require(TO);
      def = parseBlockCall();
    }
    return new CaseExp(pos, a, alts, def);
  }

  /** Parse an If construct at the end of a Code sequence. */
  private CodeExp parseCodeIf(Position pos) throws Failure {
    lexer.nextToken(/* IF */ );
    AtomExp a = parseAtom();
    require(THEN);
    BlockCallExp ifTrue = parseBlockCall();
    require(ELSE);
    BlockCallExp ifFalse = parseBlockCall();
    return new IfExp(pos, a, ifTrue, ifFalse);
  }

  /** Parse a list of Case alternatives, having already read the first n items in the list. */
  private AltExp[] parseAlts(int n) throws Failure {
    AltExp alt = maybeParseAlt();
    if (alt == null) {
      return new AltExp[n];
    } else {
      AltExp[] alts = parseAlts(n + 1);
      alts[n] = alt;
      return alts;
    }
  }

  /** Try to parse a single Case alternative. */
  private AltExp maybeParseAlt() throws Failure {
    int token = lexer.getToken();
    if (token == CONID || token == CONSYM) {
      String id = lexer.getLexeme(); // constructor name
      Position pos = lexer.getPos();
      lexer.nextToken(/* con */ );
      require(TO);
      return new AltExp(pos, id, parseBlockCall());
    } else {
      return null;
    }
  }

  /** Parse an assert within a Code sequence. */
  private CodeExp parseCodeAssert(Position pos) throws Failure {
    lexer.nextToken(/* ASSERT */ );
    AtomExp a = parseAtom();
    int token = lexer.getToken();
    if (token == CONID || token == CONSYM) {
      String id = lexer.getLexeme();
      if (lexer.nextToken(/* con */ ) == SEMI) { // skip optional semicolon
        lexer.nextToken(/* SEMI */ );
      }
      return new AssertExp(pos, a, id, parseCode());
    }
    return parseCodeError(pos);
  }

  /** Parse a block call within a Case alternative. */
  private BlockCallExp parseBlockCall() throws Failure {
    Position pos = lexer.getPos();
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String id = lexer.getLexeme();
          lexer.nextToken(/* var|con */ );
          require(SOPEN);
          BlockCallExp bc = new BlockCallExp(pos, id, parseAtoms());
          require(SCLOSE);
          return bc;
        }
      default:
        throw missing("block name");
    }
  }

  /** Raise an exception after detecting a parse error in a Code sequence expression. */
  private CodeExp parseCodeError(Position pos) throws Failure {
    throw new Failure(pos, "Syntax error in code sequence");
  }

  /** Parse a Tail expression. */
  private TailExp parseTail() throws Failure {
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        return parseTailId();
      case RETURN:
        return parseTailReturn();
      default:
        return parseTailError();
    }
  }

  /** Parse a Tail expression that begins with a var. */
  private TailExp parseTailId() throws Failure {
    String id = lexer.getLexeme();
    Position pos = lexer.getPos();
    lexer.nextToken(/* var */ );
    return parseTailId(pos, id);
  }

  /** Parse a Tail expression having already read an initial identifier. */
  private TailExp parseTailId(Position pos, String id) throws Failure {
    switch (lexer.getToken()) {
      case POPEN:
        {
          TailExp t;
          if (lexer.nextToken(/* POPEN */ ) == POPEN) {
            lexer.nextToken(/* POPEN */ );
            t = new PrimCallExp(pos, id, parseAtoms());
            require(PCLOSE);
          } else {
            t = new DataAllocExp(pos, id, parseAtoms());
          }
          require(PCLOSE);
          return t;
        }

      case SOPEN:
        {
          lexer.nextToken(/* SOPEN */ );
          TailExp t = new BlockCallExp(pos, id, parseAtoms());
          require(SCLOSE);
          return t;
        }

      case BOPEN:
        {
          lexer.nextToken(/* BOPEN */ );
          TailExp t = new ClosAllocExp(pos, id, parseAtoms());
          require(BCLOSE);
          return t;
        }

      case APPLY:
        lexer.nextToken(/* @ */ );
        return new EnterExp(new VarExp(pos, id), parseAtomsTuple());

      case NATLIT:
        {
          int n;
          try {
            n = lexer.getInt();
          } finally {
            lexer.nextToken(/* NATLIT */ );
          }
          return new SelExp(pos, id, n, parseAtom());
        }

      default:
        return parseTailError();
    }
  }

  /** Parse a return Tail, with return as the current token. */
  private TailExp parseTailReturn() throws Failure {
    lexer.nextToken(/* RETURN */ );
    return new ReturnExp(parseAtomsTuple());
  }

  /** Raise an exception after detecting a parse error in a Tail expression. */
  private TailExp parseTailError() throws Failure {
    throw new Failure(lexer.getPos(), "Syntax error in tail expression");
  }

  /** Parse an Atom. */
  private AtomExp parseAtom() throws Failure {
    AtomExp a = maybeParseAtom();
    if (a == null) {
      throw new Failure(lexer.getPos(), "Missing identifier or constant");
    }
    return a;
  }

  /** Parse an Atom, or return null if there is no atom. */
  private AtomExp maybeParseAtom() throws Failure {
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          VarExp e = new VarExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* var|con */ );
          return e;
        }

      case NATLIT:
        {
          WordExp e;
          try {
            e = new WordExp(lexer.getWord());
          } finally {
            lexer.nextToken(/* NATLIT */ );
          }
          return e;
        }

      case BITLIT:
        {
          BitsExp e = new BitsExp(lexer.getNat(), lexer.getNumBits());
          lexer.nextToken(/* BITLIT */ );
          return e;
        }

      case STRLIT:
        {
          StringExp e = new StringExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* STRLIT */ );
          return e;
        }
    }
    return null;
  }

  /** Parse a list of zero or more atoms. */
  private AtomExp[] parseAtoms() throws Failure {
    AtomExp a = maybeParseAtom();
    return (a == null) ? new AtomExp[0] : parseAtoms(a, 1);
  }

  private AtomExp[] parseAtomsTuple() throws Failure {
    if (lexer.getToken() == SOPEN) {
      lexer.nextToken(/* [ */ );
      AtomExp[] as = parseAtoms();
      require(SCLOSE);
      return as;
    }
    return new AtomExp[] {parseAtom()};
  }

  /** Parse the tail of a non-empty, comma-separated list of atoms. */
  private AtomExp[] parseAtoms(AtomExp a, int n) throws Failure {
    AtomExp[] as;
    if (lexer.getToken() == COMMA) {
      lexer.nextToken(/* , */ );
      as = parseAtoms(parseAtom(), n + 1);
    } else {
      as = new AtomExp[n];
    }
    as[n - 1] = a;
    return as;
  }

  /** Parse a list of zero or more identifier/formal names, optionally enclosed in brackets. */
  private String[] parseIdsTuple() throws Failure {
    switch (lexer.getToken()) {
      case SOPEN:
        {
          lexer.nextToken(/* [ */ );
          String[] ids = parseIds();
          require(SCLOSE);
          return ids;
        }
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String[] ids = new String[] {lexer.getLexeme()};
          lexer.nextToken(/* var|con */ );
          return ids;
        }
      default:
        throw missing("identifier");
    }
  }

  /** Parse a tuple type expression. To be used in places where only a tuple type can be used. */
  private TupleTypeExp parseTupleTypeExp() {
    Position pos = lexer.getPos();
    require(SOPEN);
    TypeExp[] ts = parseBracketedTypes(SCLOSE);
    return new TupleTypeExp(pos, ts);
  }

  protected TypeExp maybeTypeAtomExp() {
    if (lexer.getToken() == SOPEN) {
      Position pos = lexer.getPos();
      lexer.nextToken(/* SOPEN */ );
      TypeExp[] ts = parseBracketedTypes(SCLOSE);
      return new TupleTypeExp(pos, ts);
    }
    return super.maybeTypeAtomExp();
  }

  /**
   * Look for an arrow symbol in a type. We extend the core implementation to allow the use of ->>.
   */
  protected TypeExp maybeTypeArrow() {
    if (lexer.getToken() == MILTO) {
      TypeExp t = new TyconTypeExp(lexer.getPos(), Tycon.milArrow);
      lexer.nextToken(/* ->> */ );
      return t;
    }
    return super.maybeTypeArrow();
  }
}
