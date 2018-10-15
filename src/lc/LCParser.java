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
package lc;

import compiler.*;
import core.*;
import mil.*;

public class LCParser extends CoreParser implements LCTokens {

  private LCLoader loader;

  /** Default constructor. */
  public LCParser(Handler handler, LCLexer lexer, LCLoader loader) {
    super(handler, lexer);
    this.loader = loader;
  }

  public void parse(LCProgram prog) {
    lexer.enterSection();
    for (; ; ) {
      if (!lexer.match(SEMI)) {
        try {
          CoreDefn defn = maybeCoreDefn();
          if (defn != null) {
            prog.add(defn);
          } else if (!handleTopDefn(prog)) {
            LCDefn d = maybeParseDefn();
            if (d != null) {
              prog.add(d);
              lexer.itemEnd("definition");
            } else {
              lexer.leaveSection();
              checkForEnd();
              return;
            }
          }
        } catch (Failure f) {
          report(f);
        }
      }
    }
  }

  private boolean handleTopDefn(LCProgram prog) throws Failure {
    switch (lexer.getToken()) {
      case REQUIRE:
        {
          if (lexer.nextToken(/* REQUIRE */ ) != STRLIT) {
            throw new ParseFailure(lexer.getPos(), "Missing file name for require declaration");
          }
          String name = lexer.getLexeme();
          if (!loader.loadMIL(name)) {
            prog.requires(loader.require(name));
          }
          lexer.nextToken(/* STRLIT */ );
          lexer.itemEnd("require declaration");
          return true;
        }

      case AREA:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* AREA */ );
          AreaVar[] areaVars = areaVars(0);
          require(COCO);
          TypeExp areaType = typeExp();
          TypeExp alignExp = lexer.match(ALIGNED) ? typeExp() : null;
          prog.add(new AreaDefn(pos, areaVars, areaType, alignExp));
          lexer.itemEnd("area declaration");
          return true;
        }

        // TODO: consolidate EXPORT and ENTRYPOINT with similar code in milasm
      case EXPORT:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* EXPORT */ );
          String[] ids = parseIds1();
          if (lexer.match(COCO)) {
            prog.add(new TypeAnn(pos, ids, typeExp()));
          }
          lexer.itemEnd("export declaration");
          prog.add(new Export(pos, ids));
          return true;
        }

      case ENTRYPOINT:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* ENTRYPOINT */ );
          String[] ids = parseIds1();
          if (lexer.match(COCO)) {
            prog.add(new TypeAnn(pos, ids, typeExp()));
          }
          lexer.itemEnd("entrypoint declaration");
          prog.add(new Entrypoint(pos, ids));
          return true;
        }

      default:
        return false;
    }
  }

  /**
   * Parse a comma separated list of (one or more) area variables. Following the pattern used
   * elsewhere, the parameter i specifies how many area variables have already been read as part of
   * this definition so that we can allocate an array of the appropriate size.
   */
  private AreaVar[] areaVars(int i) throws Failure {
    AreaVar areaVar = areaVar();
    AreaVar[] areaVars = lexer.match(COMMA) ? areaVars(i + 1) : new AreaVar[i + 1];
    areaVars[i] = areaVar;
    return areaVars;
  }

  /**
   * Read an area variable specification, providing a name and an initializer for a new memory area.
   */
  private AreaVar areaVar() throws Failure {
    if (lexer.getToken() != VARID) {
      throw missing("area name");
    }
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    lexer.nextToken(/* VARID */ );
    require(FROM);
    return new AreaVar(pos, id, parseInfixExpr());
  }

  /** Parse a non-empty list of definitions. */
  private LCDefns parseDefns() throws Failure {
    LCDefns defns = null;
    lexer.enterSection();
    for (; ; ) {
      if (!lexer.match(SEMI)) {
        LCDefn d = maybeParseDefn();
        if (d == null) {
          lexer.leaveSection();
          return defns; // TODO: reverse?
        }
        defns = new LCDefns(d, defns);
      }
    }
  }

  /** Parse a single definition. */
  private LCDefn maybeParseDefn() throws Failure {
    Expr e = maybeParseAExpr(); // Look for an atomic expression
    if (e != null) {
      e = parseInfixExpr(e); // Extend to an infix expression
      switch (lexer.getToken()) {
        case EQ:
          {
            int n = e.mustBeLhs(0);
            Position pos = lexer.getPos();
            lexer.nextToken(/* = */ );
            return e.makeEquation(pos, (n > 0 ? new DefVar[n] : null), n, parseExpr());
          }
        case COMMA:
          {
            return parseTypeAnn(parseIdExprs(e.mustBeId(), 1));
          }
        default:
          return parseTypeAnn(new String[] {e.mustBeId()});
      }
    }
    return null;
  }

  /**
   * Parse a sequence of expressions, each of which must be an identifier, having already read n
   * previous items---id being the most recent---with the next token having been detected as a COMMA
   * (that has not yet been skipped).
   */
  private String[] parseIdExprs(String id, int n) throws Failure {
    lexer.nextToken(/* COMMA */ );
    Expr e = parseInfixExpr();
    String id1 = e.mustBeId();
    String[] ids;
    if (lexer.getToken() == COMMA) {
      ids = parseIdExprs(id1, n + 1);
    } else {
      ids = new String[n + 1];
      ids[n] = id1;
    }
    ids[n - 1] = id;
    return ids;
  }

  /** Parse a type annotation having already read the list of names. */
  private TypeAnn parseTypeAnn(String[] ids) throws Failure {
    if (lexer.getToken() != COCO) {
      throw missing("::");
    }
    Position pos = lexer.getPos();
    lexer.nextToken(/* :: */ );
    return new TypeAnn(pos, ids, typeExp());
  }

  /** Parse an expression. Expr ::= TExpr1 | ... | TExprN n>=1 */
  private Expr parseExpr() throws Failure {
    Expr e = parseTExpr();
    while (lexer.getToken() == BAR) {
      Position pos = lexer.getPos();
      lexer.nextToken(/* | */ );
      e = new EFatbar(pos, e, parseTExpr());
    }
    return e;
  }

  /** Report an error if the parser result passed in as an argument to this function is null. */
  private Expr failIfMissing(Expr e) throws Failure {
    if (e == null) {
      throw new ParseFailure(lexer.getPos(), "Syntax error in expression");
    }
    return e;
  }

  /** Parse an expression, possibly followed by a type signature, or throw a parser exception. */
  private Expr parseTExpr() throws Failure {
    return failIfMissing(maybeParseTExpr());
  }

  /**
   * Try to parse an expression with an optional type annotation, TExpr ::= CExpr [ :: Type ], n>=1,
   * returning null if there is no expression.
   */
  private Expr maybeParseTExpr() throws Failure {
    Expr e = maybeParseCExpr();
    if (e != null && lexer.getToken() == COCO) {
      Position pos = lexer.getPos();
      lexer.nextToken(/* :: */ );
      e = new EType(pos, e, typeExp());
    }
    return e;
  }

  /**
   * Parse a special form expression, or throw a parser exception. CExpr = LambdaExpr | LetExpr |
   * IfExpr | DoExpr | InfixExpr
   */
  private Expr parseCExpr() throws Failure {
    return failIfMissing(maybeParseCExpr());
  }

  private Expr maybeParseCExpr() throws Failure {
    switch (lexer.getToken()) {
      case LAMBDA:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* \ */ );
          LamVar[] vs = parseVars(0);
          require(TO);
          return new ELam(pos, vs, parseExpr());
        }

      case LET:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* LET */ );
          LCDefns defns = parseDefns();
          require(IN);
          return new ELet(pos, defns, parseExpr());
        }

      case DO:
        lexer.nextToken(/* DO */ );
        return parseBlock();

      case IF:
        return parseCond(false);

      case CASE:
        return parseCase(false);
    }
    return maybeParseInfixExpr();
  }

  /** Parse a block of statements in a do expression. */
  private Expr parseBlock() throws Failure {
    lexer.enterSection();
    Expr stmts = parseStmts();
    lexer.leaveSection();
    return stmts;
  }

  private Expr parseStmts() throws Failure {
    return failIfMissing(maybeParseStmts());
  }

  private Expr maybeParseStmts() throws Failure {
    while (lexer.match(SEMI)) {
      /* skip semicolons/empty statements */
    }
    switch (lexer.getToken()) {
      case LET:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* LET */ );
          LCDefns defns = parseDefns();
          if (lexer.match(IN)) {
            return parseExprStmts(new ELet(pos, defns, parseBlock()));
          } else {
            // stmts -> let defns _ ; stmts
            lexer.itemEnd("let statement");
            if (lexer.match(SEMI)) {
              return new ELet(pos, defns, parseStmts());
            } else {
              throw new ParseFailure(lexer.getPos(), "missing expression after let statement");
            }
          }
        }

      case IF:
        return parseExprStmts(parseCond(true));

      case CASE:
        return parseExprStmts(parseCase(true));

      default:
        {
          Expr e = maybeParseTExpr();
          if (e == null) {
            return null;
          } else if (lexer.match(FROM)) {
            LamVar v = e.asLamVar(null);
            Position pos = e.getPos();
            e = parseTExpr();
            // stmts -> id <- e _ ; stmts
            lexer.itemEnd("generator");
            if (lexer.match(SEMI)) {
              return new EFrom(pos, v, e, parseStmts());
            } else {
              throw new ParseFailure(lexer.getPos(), "missing expression after qualifier");
            }
          } else {
            return parseExprStmts(e);
          }
        }
    }
  }

  private Expr parseExprStmts(Expr e) throws Failure { // stmts -> exp _ [ ; stmts ]
    lexer.itemEnd("expression");
    if (lexer.match(SEMI)) {
      Expr s = maybeParseStmts();
      return (s == null) ? e : new EFrom(e.getPos(), new FreshVar(), e, s);
    }
    return e;
  }

  /**
   * Parse a conditional expression (if-then-else in an expression, or an if<- or an if statement in
   * a monadic context.
   */
  private Expr parseCond(boolean isMonadic) throws Failure {
    Position pos = lexer.getPos();
    boolean ifFrom = false;
    if (lexer.nextToken(/* IF */ ) == FROM) {
      lexer.nextToken(/* <- */ );
      isMonadic = ifFrom = true;
      // TODO: could we use parseBlock() for test in this special case?
    }
    Expr test = parseExpr();

    Position post = lexer.getPos();
    if (!lexer.match(THEN)) {
      throw new ParseFailure(lexer.getPos(), "missing \"then\" branch");
    }
    Expr ifTrue = isMonadic ? parseBlock() : parseTExpr();

    Expr ifFalse;
    Position posf = lexer.getPos();
    if (lexer.match(ELSE)) {
      ifFalse = isMonadic ? parseBlock() : parseTExpr();
    } else if (isMonadic) {
      // TODO: avoid hardwired reference to "return"
      ifFalse = new EAp(new EId(pos, "return"), new EId(pos, Cfun.Unit.getId()));
    } else {
      throw new ParseFailure(lexer.getPos(), "missing \"else\" branch");
    }

    if (ifFrom) {
      DefVar v = new FreshVar();
      return new EFrom(
          pos, v, test, Expr.ifthenelse(pos, new EVar(pos, v), post, ifTrue, posf, ifFalse));
    } else {
      return Expr.ifthenelse(pos, test, post, ifTrue, posf, ifFalse);
    }
  }

  /** Parse a case expression, or a case-from or case statement in a monadic context. */
  private Expr parseCase(boolean isMonadic) throws Failure {
    Position pos = lexer.getPos();
    boolean caseFrom = false;
    if (lexer.nextToken(/* CASE */ ) == FROM) {
      lexer.nextToken(/* <- */ );
      isMonadic = caseFrom = true;
      // TODO: could we use parseBlock() in this special case?
    }
    Expr discr = parseExpr();

    if (!lexer.match(OF)) {
      throw new ParseFailure(lexer.getPos(), "missing \"of\" part");
    }
    lexer.enterSection();
    EAlt[] alts = parseAlts(0, isMonadic);
    lexer.leaveSection();

    if (caseFrom) {
      DefVar v = new FreshVar();
      return new EFrom(pos, v, discr, new ECase(pos, new EVar(pos, v), alts));
    } else {
      return new ECase(pos, discr, alts);
    }
  }

  /**
   * Parse a non-empty sequence of alternatives, separated by semicolons. The parameter indicates
   * the number of alternatives that have already been seen.
   */
  private EAlt[] parseAlts(int i, boolean isMonadic) throws Failure {
    while (lexer.match(SEMI)) /* skip semicolons/empty alternatives */ ;
    EAlt alt = parseAlt(isMonadic);
    EAlt[] alts = lexer.match(SEMI) ? parseAlts(i + 1, isMonadic) : new EAlt[i + 1];
    alts[i] = alt;
    return alts;
  }

  /** Parse a single alternative. */
  private EAlt parseAlt(boolean isMonadic) throws Failure {
    if (lexer.getToken() == CONID) {
      Position pos = lexer.getPos();
      String c = lexer.getLexeme();
      lexer.nextToken(/* CONID */ );
      LamVar[] vs = parseVars(0);
      require(TO);
      return new EAlt(pos, c, vs, isMonadic ? parseBlock() : parseTExpr());
    } else {
      throw new ParseFailure(lexer.getPos(), "Missing CONID for alternative");
    }
  }

  /**
   * Parse a sequence of identifiers as an array of DefVars. The parameter records the number of
   * arguments that have already been seen.
   */
  private LamVar[] parseVars(int i) throws Failure {
    Expr e = maybeParseAExpr();
    if (e != null) {
      LamVar[] vs = parseVars(i + 1);
      vs[i] = e.asLamVar(null);
      return vs;
    }
    return new LamVar[i];
  }

  /**
   * Parse an infix expression without having read any initial portion, throwing an exception if no
   * suitable initial token is found.
   */
  private Expr parseInfixExpr() throws Failure {
    return failIfMissing(maybeParseInfixExpr());
  }

  /**
   * Parse an infix expression or return null if the current token is not in the appropriate FIRST
   * set.
   */
  private Expr maybeParseInfixExpr() throws Failure {
    Expr e = maybeParseAExpr();
    return (e == null) ? null : parseInfixExpr(e);
  }

  /**
   * Parse an infix expression having already read the first (leftmost) aexpr, passed in as
   * parameter e.
   */
  private Expr parseInfixExpr(Expr e) throws Failure {
    // Parse an applicative expression (a sequence of juxtaposed aexprs) with the first aexpr
    // already in e
    for (Expr arg; (arg = maybeParseAExpr()) != null; ) {
      e = new EAp(e, arg);
    }
    // Parse an infix operator followed by another infix expression, if present:
    if (lexer.getToken() == VARSYM) {
      Position pos = lexer.getPos();
      String id = lexer.getLexeme();
      lexer.nextToken(/* VARSYM */ );
      Expr f = parseInfixExpr();
      if (id.equals("&&")) {
        return Expr.ifthenelse(pos, e, pos, f, pos, Expr.falseCon);
      } else if (id.equals("||")) {
        return Expr.ifthenelse(pos, e, pos, Expr.trueCon, pos, f);
      } else {
        return new EAp(new EAp(new EId(pos, id), e), f);
      }
    }
    return e;
  }

  /** Parse an atomic expression (aexpr), or return null if no valid expression is found. */
  private Expr maybeParseAExpr() throws Failure {
    Expr e = null;

    switch (lexer.getToken()) {
      case VARID:
        {
          e = new EId(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* VARID */ );
          break;
        }

      case CONID:
        {
          Position pos = lexer.getPos();
          String id = lexer.getLexeme();
          if (lexer.nextToken(/* CONID */ ) == SOPEN) {
            e = parseConstruct(pos, id);
          } else {
            e = new EId(pos, id);
          }
          break;
        }

      case NATLIT:
        {
          try {
            e = new ENat(lexer.getPos(), lexer.getWord());
          } finally {
            lexer.nextToken(/* NATLIT */ );
          }
          return e;
        }

      case BITLIT:
        {
          e = new EBit(lexer.getPos(), lexer.getNat(), lexer.getNumBits());
          lexer.nextToken(/* BITLIT */ );
          return e;
        }

      case STRLIT:
        {
          e = new EStr(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* STRLIT */ );
          return e;
        }

      case POPEN:
        {
          if (lexer.nextToken(/* ( */ ) == VARSYM) {
            e = new EId(lexer.getPos(), lexer.getLexeme());
            lexer.nextToken(/* VARSYM */ );
          } else {
            e = parseExpr();
          }
          require(PCLOSE);
          break;
        }

      default:
        return null;
    }

    // Parse zero or more suffixes: either a selection or an update
    for (; ; ) {
      switch (lexer.getToken()) {
        case DOT:
          {
            if (lexer.nextToken(/* DOT */ ) != VARID) {
              throw missing("field name");
            }
            e = new ESelect(lexer.getPos(), e, lexer.getLexeme());
            lexer.nextToken(/* VARID */ );
            continue;
          }

        case SOPEN:
          {
            Position pos = lexer.getPos();
            e = new EUpdate(pos, e, parseFields());
            continue;
          }

        default:
          return e;
      }
    }
  }

  /**
   * Parse a bitdata constructor or a structure initializer, both of which begin with CONID [ ...
   * The two different forms can be distinguished by the symbol that is used to separate field names
   * and values: id = expr in bitdata constructors and id <- expr in initializers. However, there is
   * an ambiguity here in the case where there are no fields. For the time being, we choose to
   * interpret C [] as a bitdata constructor, but this ambiguity could be properly resolved later in
   * scope analysis. TODO: Fix this ambiguity!
   */
  private EConstruct parseConstruct(Position pos, String id) throws Failure {
    if (lexer.nextToken(/* SOPEN */ ) != VARID) {
      require(SCLOSE);
      return new EBitdata(pos, id, new EField[0]);
    }
    Position pos1 = lexer.getPos();
    String id1 = lexer.getLexeme();
    switch (lexer.nextToken(/* VARID */ )) {
      case EQ:
        { // bitdata constructor
          lexer.nextToken(/* EQ */ );
          EField f = new EField(pos1, id1, parseCExpr());
          EField[] fs = lexer.match(BAR) ? parseFields(1, EQ) : new EField[1];
          fs[0] = f;
          require(SCLOSE);
          return new EBitdata(pos, id, fs);
        }
      case FROM:
        { // structure initializer
          lexer.nextToken(/* FROM */ );
          EField f = new EField(pos1, id1, parseCExpr());
          EField[] fs = lexer.match(BAR) ? parseFields(1, FROM) : new EField[1];
          fs[0] = f;
          require(SCLOSE);
          return new EStructInit(pos, id, fs);
        }
    }
    throw new ParseFailure(lexer.getPos(), "expecting = or <- symbol");
  }

  /**
   * Parse a list of fields of the form id = expr in a bitdata update expression, having just
   * recognized the opening [ symbol at the start of the (possibly empty) list.
   */
  private EField[] parseFields() throws Failure {
    lexer.nextToken(/* SOPEN */ );
    EField[] fs = parseFields(0, EQ);
    require(SCLOSE);
    return fs;
  }

  /**
   * Parse a list of fields, separated by | symbols. The i parameter specifies the number of fields
   * that have already been read (we must make space for these in the array that is returned) and
   * sym specifies the symbol that separates the identifier from the expression in each individual
   * field.
   */
  private EField[] parseFields(int i, int sym) throws Failure {
    if (lexer.getToken() == VARID) {
      EField f = parseField(sym);
      EField[] fs = lexer.match(BAR) ? parseFields(i + 1, sym) : new EField[i + 1];
      fs[i] = f;
      return fs;
    } else {
      return new EField[i];
    }
  }

  /**
   * Parse a single field of the form VARID sym expr, having already found the initial VARID, and
   * where the symbol sym, passed in as a parameter, should be either EQ or FROM.
   */
  private EField parseField(int sym) throws Failure {
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    lexer.nextToken(/* VARID */ );
    require(sym);
    Expr e = parseCExpr();
    return new EField(pos, id, e);
  }

  protected BitdataFieldExp bitdataField(Position pos, String id) throws Failure {
    return lexer.match(EQ)
        ? new InitBitdataFieldExp(pos, id, parseInfixExpr())
        : super.bitdataField(pos, id);
  }

  protected StructFieldExp structField(Position pos, String id) throws Failure {
    return lexer.match(FROM)
        ? new InitStructFieldExp(pos, id, parseInfixExpr())
        : super.structField(pos, id);
  }
}
