/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

class LCParser extends CoreParser implements LCTokens {

  private LCLoader loader;

  /** Default constructor. */
  LCParser(Handler handler, LCLexer lexer, LCLoader loader) {
    super(handler, lexer);
    this.loader = loader;
  }

  void parse(LCProgram prog) {
    lexer.enterSection();
    for (; ; ) {
      if (!lexer.match(SEMI)) {
        try {
          CoreDefn defn = maybeCoreDefn();
          if (defn != null) {
            prog.add(defn);
          } else if (!handleTopDefn(prog)) {
            LCDefn d = maybeLCDefn();
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
  private LCDefns parseLCDefns() throws Failure {
    LCDefns defns = null;
    lexer.enterSection();
    for (; ; ) {
      if (!lexer.match(SEMI)) {
        LCDefn d = maybeLCDefn();
        if (d == null) {
          lexer.leaveSection();
          return defns; // TODO: reverse?
        }
        defns = new LCDefns(d, defns);
      }
    }
  }

  /** Parse a single definition. */
  private LCDefn maybeLCDefn() throws Failure {
    Expr e = maybeAExpr(); // Look for an atomic expression
    if (e != null) {
      e = parseInfixExpr(e); // Extend to an infix expression
      switch (lexer.getToken()) {
        case EQ:
          {
            int n = e.mustBeLhs(0);
            Position pos = lexer.getPos();
            lexer.nextToken(/* = */ );
            return e.makeEquation(pos, (n > 0 ? new DefVar[n] : null), n, parseRhs());
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

  /** Read a right hand side, comprising an expression and an optional WHERE clause. */
  private Expr parseRhs() throws Failure {
    Expr e = parseExpr();
    if (lexer.getToken() == WHERE) {
      Position pos = lexer.getPos();
      lexer.nextToken(/* WHERE */ );
      LCDefns defns = parseLCDefns();
      return new ELet(pos, defns, e);
    }
    return e;
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
    return failIfMissing(maybeTExpr());
  }

  /**
   * Try to parse an expression with an optional type annotation, TExpr ::= CExpr [ :: Type ], n>=1,
   * returning null if there is no expression.
   */
  private Expr maybeTExpr() throws Failure {
    Expr e = maybeCExpr();
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
    return failIfMissing(maybeCExpr());
  }

  private Expr maybeCExpr() throws Failure {
    switch (lexer.getToken()) {
      case LAMBDA:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* \ */ );
          DefVar[] vs = parseVars(0);
          require(TO);
          return new ELam(pos, vs, parseExpr());
        }

      case LET:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* LET */ );
          LCDefns defns = parseLCDefns();
          require(IN);
          return new ELet(pos, defns, parseExpr());
        }

      case DO:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* DO */ );
          return new EDo(pos, parseBlock());
        }

      case IF:
        return parseCond(false);

      case CASE:
        return parseCase(false);
    }
    return maybeInfixExpr();
  }

  /** Parse a block of statements in a do expression. */
  private Expr parseBlock() throws Failure {
    lexer.enterSection();
    Expr stmts = parseStmts();
    lexer.leaveSection();
    return stmts;
  }

  private Expr parseStmts() throws Failure {
    return failIfMissing(maybeStmts());
  }

  private Expr maybeStmts() throws Failure {
    while (lexer.match(SEMI)) {
      /* skip semicolons/empty statements */
    }
    switch (lexer.getToken()) {
      case LET:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* LET */ );
          LCDefns defns = parseLCDefns();
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
          Expr e = maybeTExpr();
          if (e == null) {
            return null;
          } else if (lexer.match(FROM)) {
            DefVar v = e.asLamVar(null);
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
      Expr s = maybeStmts();
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
      return new EDo(
          pos,
          new EFrom(
              pos, v, test, Expr.ifthenelse(pos, new EVar(pos, v), post, ifTrue, posf, ifFalse)));
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
      return new EDo(pos, new EFrom(pos, v, discr, new ECase(pos, new EVar(pos, v), alts)));
    } else {
      return new ECase(pos, discr, alts);
    }
  }

  /**
   * Parse a non-empty sequence of alternatives, separated by semicolons. The parameter indicates
   * the number of alternatives that have already been seen.
   */
  private EAlt[] parseAlts(int i, boolean isMonadic) throws Failure {
    while (lexer.match(SEMI))
      /* skip semicolons/empty alternatives */ ;
    EAlt alt = parseAlt(isMonadic);
    EAlt[] alts = lexer.match(SEMI) ? parseAlts(i + 1, isMonadic) : new EAlt[i + 1];
    alts[i] = alt;
    return alts;
  }

  /** Parse a single alternative. */
  private EAlt parseAlt(boolean isMonadic) throws Failure {
    switch (lexer.getToken()) {
      case VARID:
        return parseVarAlt(lexer.getPos(), isMonadic, new LamVar(lexer.getLexeme(), null));

      case UNDER:
        return parseVarAlt(lexer.getPos(), isMonadic, new FreshVar());

      case CONID:
      case CONSYM:
        {
          Position pos = lexer.getPos();
          String c = lexer.getLexeme();
          lexer.nextToken(/* CONID|CONSYM */ );
          DefVar[] vs = parseVars(0);
          require(TO);
          return new EPatAlt(pos, isMonadic ? parseBlock() : parseTExpr(), c, vs);
        }
    }
    throw new ParseFailure(lexer.getPos(), "Missing CONID for alternative");
  }

  private EVarAlt parseVarAlt(Position pos, boolean isMonadic, DefVar v) throws Failure {
    lexer.nextToken(/* VARID or UNDER */ );
    require(TO);
    return new EVarAlt(pos, isMonadic ? parseBlock() : parseTExpr(), v);
  }

  /**
   * Parse a sequence of identifiers as an array of DefVars. The parameter records the number of
   * arguments that have already been seen.
   */
  private DefVar[] parseVars(int i) throws Failure {
    DefVar v;
    if (lexer.match(UNDER)) {
      v = new FreshVar();
    } else {
      Expr e = maybeAExpr();
      if (e == null) {
        return new DefVar[i];
      }
      v = e.asLamVar(null);
    }
    DefVar[] vs = parseVars(i + 1);
    vs[i] = v;
    return vs;
  }

  /**
   * Parse an infix expression without having read any initial portion, throwing an exception if no
   * suitable initial token is found.
   */
  private Expr parseInfixExpr() throws Failure {
    return failIfMissing(maybeInfixExpr());
  }

  /**
   * Parse an infix expression or return null if the current token is not in the appropriate FIRST
   * set.
   */
  private Expr maybeInfixExpr() throws Failure {
    Expr e = maybeAExpr();
    return (e == null) ? null : parseInfixExpr(e);
  }

  /**
   * Parse an infix expression having already read the first (leftmost) aexpr, passed in as
   * parameter e.
   */
  private Expr parseInfixExpr(Expr e) throws Failure {
    // Parse an applicative expression (a sequence of juxtaposed aexprs) with the first aexpr
    // already in e
    for (Expr arg; (arg = maybeAExpr()) != null; ) {
      e = new EAp(e, arg);
    }
    // Parse an infix operator followed by another infix expression, if present:
    switch (lexer.getToken()) {
      case VARSYM:
      case CONSYM:
        {
          Position pos = lexer.getPos();
          String id = lexer.getLexeme();
          lexer.nextToken(/* SYM */ );
          return new EAp(new EAp(new EId(pos, id), e), parseInfixExpr());
        }

      case BACKTICK:
        {
          lexer.nextToken(/* BACKTICK */ );
          Expr op = failIfMissing(maybeAExpr());
          require(BACKTICK);
          return new EAp(new EAp(op, e), parseInfixExpr());
        }

      case AMPAMP:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* && */ );
          return Expr.ifthenelse(pos, e, pos, parseInfixExpr(), pos, Expr.falseCon);
        }

      case BARBAR:
        {
          Position pos = lexer.getPos();
          lexer.nextToken(/* || */ );
          return Expr.ifthenelse(pos, e, pos, Expr.trueCon, pos, parseInfixExpr());
        }
    }
    return e;
  }

  /** Parse an atomic expression (aexpr), or return null if no valid expression is found. */
  private Expr maybeAExpr() throws Failure {
    Position pos = lexer.getPos();
    Expr e = null;
    Atom a = parseCoreLiteral(pos);
    if (a != null) {
      e = new ELit(pos, a);
    } else {
      switch (lexer.getToken()) {
        case VARID:
          e = new EId(pos, lexer.getLexeme());
          lexer.nextToken(/* VARID */ );
          break;

        case CONID:
          {
            String id = lexer.getLexeme();
            if (lexer.nextToken(/* CONID */ ) == SOPEN) {
              e = new EConstruct(pos, id, parseFields());
            } else {
              e = new EId(pos, id);
            }
            break;
          }

        case POPEN:
          switch (lexer.nextToken(/* ( */ )) {
            case VARSYM:
            case CONSYM:
              e = new EId(lexer.getPos(), lexer.getLexeme());
              lexer.nextToken(/* SYM */ );
              break;

            default:
              e = parseExpr();
              break;
          }
          require(PCLOSE);
          break;

        default:
          return null;
      }
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
            pos = lexer.getPos();
            e = new EUpdate(pos, e, parseFields());
            continue;
          }

        default:
          return e;
      }
    }
  }

  /**
   * Parse a list of fields having just recognized an opening [ symbol at the start of a (possibly
   * empty) list.
   */
  private EField[] parseFields() throws Failure {
    lexer.nextToken(/* SOPEN */ );
    EField[] fs = parseFields(0);
    require(SCLOSE);
    return fs;
  }

  /**
   * Parse a list of fields, separated by | symbols. The i parameter specifies the number of fields
   * that have already been read (we must make space for these in the array that is returned).
   */
  private EField[] parseFields(int i) throws Failure {
    if (lexer.getToken() == VARID) {
      EField f = parseField();
      EField[] fs = lexer.match(BAR) ? parseFields(i + 1) : new EField[i + 1];
      fs[i] = f;
      return fs;
    } else {
      return new EField[i];
    }
  }

  /**
   * Parse a field of the form VARID Opt(= expr | <- expr), having already found the initial VARID.
   */
  private EField parseField() throws Failure {
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    switch (lexer.nextToken(/* VARID */ )) {
      case EQ:
        lexer.nextToken(/* EQ */ );
        return new EqField(pos, id, parseCExpr());
      case FROM:
        lexer.nextToken(/* FROM */ );
        return new FromField(pos, id, parseCExpr());
      default:
        return new PunField(pos, id, new EId(pos, id));
    }
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
