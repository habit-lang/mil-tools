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
package core;

import compiler.*;
import mil.*;

public class CoreParser extends Phase implements CoreTokens {

  protected CoreLexer lexer;

  /** Default constructor. */
  public CoreParser(Handler handler, CoreLexer lexer) {
    super(handler);
    this.lexer = lexer;
  }

  /** Check for a required token. */
  protected void require(int token, String what) {
    if (!lexer.match(token)) {
      report(missing(what));
    }
  }

  /**
   * Variation of require where the "what" string describing the missing token can be determined
   * directly from the given token code.
   */
  protected void require(int token) {
    if (!lexer.match(token)) {
      report(missing(lexer.describeToken(token, null)));
    }
  }

  /** Generate Failure object for a missing item. */
  protected Failure missing(String what) {
    // TODO: it would be nice to detect the special case when the next token is a semicolon,
    // inserted by the layout rule, in which case the error message might suggest that the
    // error results from something missing earlier on the line or on a previous line.
    return new MissingFailure(lexer.getPos(), what, lexer.describeToken());
  }

  public void checkForEnd() {
    if (lexer.getToken() != ENDINPUT) {
      report(
          new Failure(
              lexer.getPos(), "Trailing token(s) (" + lexer.describeToken() + ") at end of input"));
    }
  }

  /** Parse a comma separated list of ONE or more variable names (VARIDs or VARSYMs). */
  protected String[] parseIds1() throws Failure {
    String[] ids = parseIds();
    if (ids.length == 0) {
      throw missing("identifier list");
    }
    return ids;
  }

  /**
   * Parse a comma separated list of ZERO or more variable names (vars or cons). TODO: redo this to
   * allow section syntax for operators?
   */
  protected String[] parseIds() throws Failure {
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String id = lexer.getLexeme();
          return lexer.nextToken(/* var|con */ ) == COMMA ? parseIds(id, 1) : new String[] {id};
        }
    }
    return new String[0];
  }

  /**
   * Parse the tail of a non-empty, comma separated list of variable names. Assumes that the current
   * token is a comma, separating the earlier (already read) portion of the list from whatever items
   * are yet to be read.
   */
  protected String[] parseIds(String id, int n) throws Failure {
    switch (lexer.nextToken(/* COMMA */ )) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String nid = lexer.getLexeme();
          String[] ids;
          if (lexer.nextToken() == COMMA) {
            ids = parseIds(nid, n + 1);
          } else {
            ids = new String[n + 1];
            ids[n] = nid;
          }
          ids[n - 1] = id;
          return ids;
        }
      default:
        throw missing("identifier");
    }
  }

  /** Parse a kind expression. */
  private KindExp kindExp() {
    KindExp k = maybeKindExp();
    if (k == null) {
      report(missing("kind expression"));
      return new MissingKindExp(lexer.getPos());
    }
    return k;
  }

  /** Try to parse a kind expression. */
  private KindExp maybeKindExp() {
    KindExp k = maybeKindAtom();
    if (k != null && lexer.getToken() == TO) {
      lexer.nextToken(/* -> */ );
      k = new KFunExp(k, kindExp());
    }
    return k;
  }

  /** Try to parse an atomic kind, either a kind name or a parenthesized kind. */
  private KindExp maybeKindAtom() {
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case TYPE:
      case AREA:
        {
          KindExp k = new KPrimExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* VARID | VARSYM | TYPE | AREA */ );
          return k;
        }

      case POPEN:
        {
          lexer.nextToken(/* ( */ );
          KindExp k = kindExp();
          require(PCLOSE, "missing ')'");
          return k;
        }
    }
    return null;
  }

  /** Hack to provide access to otherwise hidden kindExp() function. */
  public KindExp sneakKindExp() {
    return kindExp();
  }

  /**
   * Parse a type expression, generating a MissingTypeExp object and a corresponding diagnostic if
   * no valid type expression can be read.
   */
  protected TypeExp typeExp() {
    return notMissing(maybeTypeExp());
  }

  /** Produce a MissingTypeExp value if the given type expression is null. */
  private TypeExp notMissing(TypeExp t) {
    if (t == null) {
      Position pos = lexer.getPos();
      t = new MissingTypeExp(pos);
      report(missing("type expression"));
    }
    return t;
  }

  /** Try to parse a type, returning null if no valid type is found. */
  protected TypeExp maybeTypeExp() {
    TypeExp t = maybeTypeOpExp();
    if (t != null && lexer.getToken() == COCO) {
      Position pos = lexer.getPos();
      lexer.nextToken(/* COCO */ );
      return new KindAnnTypeExp(pos, t, kindExp());
    }
    return t;
  }

  /**
   * Try to parse a typeOpExp, which may include a function arrow and an associated range type.
   * Return null if no valid type found.
   */
  private TypeExp maybeTypeOpExp() {
    TypeExp t = maybeTypeApExp();
    if (t != null) {
      TypeExp arr = maybeTypeArrow(); // Look for an arrow ...
      if (arr != null) {
        TypeExp rng = notMissing(maybeTypeOpExp()); // Find the range
        return new ApTypeExp(new ApTypeExp(arr, t), rng); // Return function type
      }
    }
    return t;
  }

  /**
   * Try to parse a type application consisting of a sequence of one or more type atoms, but
   * returning null if no type atom can be found.
   */
  private TypeExp maybeTypeApExp() {
    TypeExp t = maybeTypeSelExp();
    if (t != null) {
      for (TypeExp a; (a = maybeTypeSelExp()) != null; ) {
        t = new ApTypeExp(t, a);
      }
    }
    return t;
  }

  /** Try to parse a type atom expression, optionally followed by selectors. */
  private TypeExp maybeTypeSelExp() {
    TypeExp t = maybeTypeAtomExp();
    if (t != null && lexer.getToken() == DOT) {
      do {
        if (lexer.nextToken(/* DOT */ ) != CONID) {
          report(new Failure(lexer.getPos(), "Missing selector name"));
          break;
        }
        t = new SelTypeExp(lexer.getPos(), t, lexer.getLexeme());
      } while (lexer.nextToken(/* CONID */ ) == DOT);
    }
    return t;
  }

  /** Parse an atomic type expression, triggering an error if no type expression is found. */
  protected TypeExp typeAtomExp() {
    return notMissing(maybeTypeAtomExp());
  }

  /**
   * Try to parse an atomic type expression, returning null if unsuccessful. TODO: document the idea
   * that this method can be overriden to expand the syntax for atomic types.
   */
  protected TypeExp maybeTypeAtomExp() {
    switch (lexer.getToken()) {
      case VARID:
        {
          TypeExp t = new VaridTypeExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* VARID */ );
          return t;
        }

      case CONID:
        {
          TypeExp t = new ConidTypeExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* CONID */ );
          return t;
        }

      case NATLIT:
        {
          TypeExp t = new NatTypeExp(lexer.getPos(), lexer.getNat());
          lexer.nextToken(/* NATLIT */ );
          return t;
        }

      case STRLIT:
        {
          TypeExp t = new LabTypeExp(lexer.getPos(), lexer.getLexeme());
          lexer.nextToken(/* STRLIT */ );
          return t;
        }

      case POPEN:
        { // TODO: this doesn't allow special syntax for unit or tuples; allow?
          Position pos = lexer.getPos();
          lexer.nextToken(/* ( */ );
          TypeExp t = maybeTypeArrow(); // Look for an arrow
          if (t == null) {
            t = typeExp(); // or else some other type expression
          }
          require(PCLOSE);
          return t;
        }
    }
    return null;
  }

  /**
   * Parse a square bracketed list of types, assuming that the lexer has just moved past the opening
   * token, but still checking for the corresponding closing token at the end of the list.
   */
  protected TypeExp[] parseBracketedTypes(int close) {
    TypeExp t = maybeTypeExp();
    TypeExp[] ts = (t == null) ? new TypeExp[0] : parseTypes(t, 1);
    require(close);
    return ts;
  }

  /** Parse the tail of a non-empty, comma-separated list of types. */
  private TypeExp[] parseTypes(TypeExp t, int sofar) {
    TypeExp[] ts;
    if (lexer.getToken() == COMMA) {
      lexer.nextToken(/* , */ );
      ts = parseTypes(typeExp(), sofar + 1);
    } else {
      ts = new TypeExp[sofar];
    }
    ts[sofar - 1] = t;
    return ts;
  }

  /**
   * Look for an arrow symbol, or return null if none found. This particular implementation only
   * checks for the -> arrow, which is part of core, but we can override this method in subclasses
   * to support different arrows (such as the ->> constructor for mil).
   */
  protected TypeExp maybeTypeArrow() {
    if (lexer.getToken() == TO) {
      TypeExp t = new TyconTypeExp(lexer.getPos(), DataType.arrow);
      lexer.nextToken(/* -> */ );
      return t;
    }
    return null;
  }

  /** Hack to provide access to otherwise hidden typeExp() function. */
  public TypeExp sneakTypeExp() {
    return typeExp();
  }

  /**
   * Parse a core program comprising a list of core definitions. parse: _ "{" (defn? ";")* defn? "}"
   */
  public CoreProgram coreProgram() {
    CoreProgram program = new CoreProgram();
    lexer.enterSection();
    for (; ; ) {
      if (lexer.getToken() == SEMI) {
        lexer.nextToken();
      } else {
        try {
          CoreDefn defn = maybeCoreDefn();
          if (defn == null) {
            lexer.leaveSection();
            return program;
          }
          program.add(defn);
        } catch (Failure f) {
          report(f);
          lexer.itemEnd("definition");
        }
      }
    }
  }

  /** Parse a core definition, if possible. */
  public CoreDefn maybeCoreDefn() throws Failure {
    switch (lexer.getToken()) {
      case DATA:
        {
          CoreDefn d = dataDefn();
          lexer.itemEnd("data type definition");
          return d;
        }

      case BITDATA:
        {
          CoreDefn d = bitdataDefn();
          lexer.itemEnd("bitdata definition");
          return d;
        }

      case STRUCT:
        {
          CoreDefn d = structDefn();
          lexer.itemEnd("struct definition");
          return d;
        }

      case TYPE:
        {
          CoreDefn d = typeDefn();
          lexer.itemEnd("type definition");
          return d;
        }

      case EXTERNAL:
        {
          CoreDefn d = externalDefn();
          lexer.itemEnd("external declaration");
          return d;
        }

      default:
        return null;
    }
  }

  private ExternalDefn externalDefn() throws Failure {
    Position pos = lexer.getPos();
    lexer.nextToken(/* EXTERNAL */ );
    ExternalId[] extids = parseExternalIds(0);
    require(COCO);
    return new ExternalDefn(pos, extids, typeExp());
  }

  private ExternalId[] parseExternalIds(int i) throws Failure {
    ExternalId extid = parseExternalId();
    ExternalId[] extids = lexer.match(COMMA) ? parseExternalIds(i + 1) : new ExternalId[i + 1];
    extids[i] = extid;
    return extids;
  }

  private ExternalId parseExternalId() throws Failure {
    Position pos = lexer.getPos();
    switch (lexer.getToken()) {
      case VARID:
      case VARSYM:
      case CONID:
      case CONSYM:
        {
          String id = lexer.getLexeme();
          if (lexer.nextToken(/* id */ ) == BOPEN) {
            switch (lexer.nextToken(/* BOPEN */ )) {
              case VARID:
              case VARSYM:
              case CONID:
              case CONSYM:
              case STRLIT:
              case NATLIT:
              case BITLIT:
                break;
              default:
                throw missing("primitive reference");
            }
            String ref = lexer.getLexeme();
            lexer.nextToken(/* {VAR,CON}{ID,SYM}|STRLIT|NATLIT|BITLIT */ );
            TypeExp[] spec = typeAtomExps(0);
            require(BCLOSE);
            return new ExternalId(pos, id, ref, spec);
          }
          return new ExternalId(pos, id, null, null);
        }
      default:
        throw missing("identifier");
    }
  }

  /**
   * Parse a data type definition, having just found (but not yet skipped) the initial DATA token
   * that begins the definition.
   */
  private DataDefn dataDefn() throws Failure {
    Position pos = lexer.getPos();
    if (lexer.nextToken(/* DATA */ ) != CONID) {
      throw missing("data type name");
    }
    String id = lexer.getLexeme();
    lexer.nextToken(/* CONID */ );
    TypeExp[] args = typeAtomExps(0);
    DataConDefn[] ds = (lexer.getToken() == EQ) ? dataConDefns(0) : new DataConDefn[0];
    return new DataDefn(pos, id, args, ds);
  }

  /**
   * Parse a list of data constructor definitions, the first of which begins with an EQ, while
   * subsequent alternatives are introduced using BAR. We assume that the appropriate EQ or BAR
   * character has already been read when this method is called, and we use the parameter i to
   * record the number of entries that have already been read.
   */
  private DataConDefn[] dataConDefns(int i) throws Failure {
    //    _ = constr_1 ...                           (when i=0)
    // OR
    //    constr_1 | ... constr_i _ | constr_{i+1}   (when i>0)
    lexer.nextToken(/* EQ or BAR */ );
    DataConDefn d = dataConDefn();
    DataConDefn[] ds = (lexer.getToken() == BAR) ? dataConDefns(i + 1) : new DataConDefn[i + 1];
    ds[i] = d;
    return ds;
  }

  /**
   * Parse a single data constructor definition, each of which starts with a constructor function
   * name (a CONID) and is followed by a list of zero or more type expressions.
   */
  private DataConDefn dataConDefn() throws Failure {
    if (lexer.getToken() != CONID) {
      throw missing("constructor function name");
    }
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    lexer.nextToken(/* CONID */ );
    return new DataConDefn(pos, id, typeAtomExps(0));
  }

  /**
   * Parse a list of atomic type expressions, returning the result as an array. The parameter i
   * specifies the number of type arguments that have already been read.
   */
  private TypeExp[] typeAtomExps(int i) {
    TypeExp t = maybeTypeAtomExp();
    if (t == null) {
      return new TypeExp[i];
    } else {
      TypeExp[] ts = typeAtomExps(i + 1);
      ts[i] = t;
      return ts;
    }
  }

  /**
   * Parse a bitdata type definition, having just found (but not yet skipped) the initial BITDATA
   * token that begins the definition.
   */
  private BitdataDefn bitdataDefn() throws Failure {
    Position pos = lexer.getPos();
    if (lexer.nextToken(/* BITDATA */ ) != CONID) {
      throw missing("bitdata type name");
    }
    String id = lexer.getLexeme();
    TypeExp sizeExp = null;
    if (lexer.nextToken(/* CONID */ ) == VARSYM && lexer.getLexeme().equals("/")) {
      lexer.nextToken(/* / */ );
      sizeExp = typeExp();
    }
    BitdataConDefn[] constrs =
        (lexer.getToken() == EQ) ? bitdataConDefns(0) : new BitdataConDefn[0];
    return new BitdataDefn(pos, id, sizeExp, constrs);
  }

  /**
   * Parse a list of bitdata constructor definitions, the first of which begins with an EQ, while
   * subsequent alternatives are introduced using BAR. The parameter i indicates the number of
   * entries that have already been read.
   */
  private BitdataConDefn[] bitdataConDefns(int i) throws Failure {
    //    _ = constr_1 ...                           (when i=0)
    // OR
    //    constr_1 | ... constr_i _ | constr_{i+1}   (when i>0)
    lexer.nextToken(/* EQ or BAR */ );
    BitdataConDefn constr = bitdataConDefn();
    BitdataConDefn[] constrs =
        (lexer.getToken() == BAR) ? bitdataConDefns(i + 1) : new BitdataConDefn[i + 1];
    constrs[i] = constr;
    return constrs;
  }

  /** Parse a single bitdata constructor definition. */
  private BitdataConDefn bitdataConDefn() throws Failure {
    if (lexer.getToken() != CONID) {
      throw missing("bitdata constructor name");
    }
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    lexer.nextToken(/* CONID */ );
    require(SOPEN);
    BitdataRegionExp[] regexps = bitdataRegions(0);
    require(SCLOSE);
    return new BitdataConDefn(pos, id, regexps);
  }

  /** Parse a list of bitdata region expressions separated by vertical bars. */
  private BitdataRegionExp[] bitdataRegions(int i) throws Failure {
    BitdataRegionExp regexp = bitdataRegion();
    if (regexp == null) { // no region found
      return new BitdataRegionExp[i];
    } else {
      BitdataRegionExp[] regexps =
          lexer.match(BAR) ? bitdataRegions(i + 1) : new BitdataRegionExp[i + 1];
      regexps[i] = regexp;
      return regexps;
    }
  }

  /**
   * Parse a region in a bitdata type, comprising either a list of one or more field labels with an
   * associated type, or else a literal that specifies some tag bits. A null result indicates that
   * no region was found.
   */
  private BitdataRegionExp bitdataRegion() throws Failure {
    switch (lexer.getToken()) {
      case VARID:
        {
          BitdataFieldExp[] fields = bitdataFields(0);
          require(COCO);
          return new BitdataFieldsExp(fields, typeExp());
        }

      case NATLIT:
        {
          BitdataRegionExp reg = new BitdataTagbitsExp(lexer.getPos(), lexer.getNat(), (-1));
          lexer.nextToken(/* NATLIT */ );
          return reg;
        }

      case BITLIT:
        {
          BitdataRegionExp reg =
              new BitdataTagbitsExp(lexer.getPos(), lexer.getNat(), lexer.getNumBits());
          lexer.nextToken(/* BITLIT */ );
          return reg;
        }

      default:
        return null;
    }
  }

  /**
   * Parse a comma separated list of (one or more) bitdata fields, assuming that the current token
   * has already been established as a VARID that can begin a bitdata field. The parameter i
   * documents the number of bitdata fields that have already been seen in the current list. (So the
   * first field that we parse here will be stored at index i in the array that is returned.)
   */
  private BitdataFieldExp[] bitdataFields(int i) throws Failure {
    BitdataFieldExp field = bitdataField();
    BitdataFieldExp[] fields;
    if (lexer.match(COMMA)) {
      if (lexer.getToken() != VARID) {
        throw missing("bitdata field label");
      }
      fields = bitdataFields(i + 1);
    } else {
      fields = new BitdataFieldExp[i + 1];
    }
    fields[i] = field;
    return fields;
  }

  /**
   * Read a single bitdata field specification, including a label for the field and an optional
   * initial value. Assumes that the current token is a VARID specifying the label for the field.
   */
  private BitdataFieldExp bitdataField() throws Failure {
    Position pos = lexer.getPos();
    String id = lexer.getLexeme();
    lexer.nextToken(/* VARID */ );
    return bitdataField(pos, id);
  }

  /**
   * Create a BitdataFieldExp with given position and identifier. Can be overridden in subclasses to
   * parse an additional default value expression.
   */
  protected BitdataFieldExp bitdataField(Position pos, String id) throws Failure {
    return new BitdataFieldExp(pos, id);
  }

  /**
   * Parse a struct type definition, having just found (but not yet skipped) the initial STRUCT
   * token that begins the definition.
   */
  private StructDefn structDefn() throws Failure {
    Position pos = lexer.getPos();
    if (lexer.nextToken(/* Struct */ ) != CONID) {
      throw missing("struct type name");
    }
    String id = lexer.getLexeme();
    TypeExp sizeExp = null;
    if (lexer.nextToken(/* CONID */ ) == VARSYM && lexer.getLexeme().equals("/")) {
      lexer.nextToken(/* / */ );
      sizeExp = typeExp();
    }
    require(SOPEN);
    StructRegionExp[] regexps =
        (lexer.getToken() == SCLOSE) ? new StructRegionExp[0] : structRegions(0);
    require(SCLOSE);
    TypeExp alignExp = lexer.match(ALIGNED) ? typeExp() : null;
    return new StructDefn(pos, id, sizeExp, alignExp, regexps);
  }

  /**
   * Parse a list of one or more structure region expressions with the current token being the first
   * token in the first of those regions.
   */
  private StructRegionExp[] structRegions(int i) throws Failure {
    StructRegionExp regexp = structRegion();
    StructRegionExp[] regexps =
        lexer.match(BAR) ? structRegions(i + 1) : new StructRegionExp[i + 1];
    regexps[i] = regexp;
    return regexps;
  }

  /**
   * Parse a structure region, specifying an (area) type for the region and a list of one or more
   * fields. A region with a given layout but no way to access its content can be described by
   * omitting the list of fields (a null value is used in the abstract syntax), although this does
   * create some complications for parsing because we have to distinguish between types and field
   * lists, both of which can begin with a VARID.
   */
  private StructRegionExp structRegion() throws Failure {
    if (lexer.getToken() == VARID) {
      Position pos = lexer.getPos();
      String id = lexer.getLexeme();
      switch (lexer.nextToken(/* VARID */ )) {
        case COMMA:
        case FROM:
        case COCO:
          StructFieldExp[] fields = structFields(pos, id, 0);
          require(COCO);
          return new StructRegionExp(fields, typeExp());

        default:
          return new StructRegionExp(null, typeExpBeginningWith(new VaridTypeExp(pos, id)));
      }
    }
    return new StructRegionExp(null, typeExp());
  }

  /**
   * Parse a type expression that begins with the atomic type expression t. This is useful when we
   * have been forced to use lookahead to determine how to parse a given input and have already
   * consumed tokens that make up the initial portion of a type expressions. TODO: This code is
   * positioned here because it is currently only used in structRegion(), but perhaps it should be
   * positioned next to typeExp() instead so that it can be more easily related to and maintained
   * with that code?
   */
  private TypeExp typeExpBeginningWith(TypeExp t) {
    for (TypeExp a; (a = maybeTypeAtomExp()) != null; ) { // apply to arguments
      t = new ApTypeExp(t, a);
    }
    TypeExp arr = maybeTypeArrow(); // Look for an arrow ...
    if (arr != null) {
      TypeExp rng = notMissing(maybeTypeOpExp()); // Find the range
      return new ApTypeExp(new ApTypeExp(arr, t), rng); // Return function type
    }
    return t;
  }

  /**
   * Parse a list of one or more structure fields, having just read and skipped past the VARID at
   * the start of the next field (with the given position and id) and having previously read i other
   * field specificiations.
   */
  private StructFieldExp[] structFields(Position pos, String id, int i) throws Failure {
    StructFieldExp field = structField(pos, id);
    StructFieldExp[] fields;
    if (lexer.getToken() == COMMA) {
      if (lexer.nextToken(/* COMMA */ ) != VARID) {
        throw missing("structure field name");
      }
      pos = lexer.getPos();
      id = lexer.getLexeme();
      lexer.nextToken(/* VARID */ );
      fields = structFields(pos, id, i + 1);
    } else {
      fields = new StructFieldExp[i + 1];
    }
    fields[i] = field;
    return fields;
  }

  /**
   * Create a StructFieldExp with given position and identifier. Can be overridden in subclasses to
   * parse an additional initializer expression.
   */
  protected StructFieldExp structField(Position pos, String id) throws Failure {
    return new StructFieldExp(pos, id);
  }

  /**
   * Parse a type synonym or primitive type definition, having just found (but not yet skipped) the
   * initial TYPE keyword that begins the definition. The syntax for a type synonym definition is as
   * follows:
   *
   * <p>TYPE Conid = texp
   *
   * <p>And the syntax for a primitive type definition is as follows:
   *
   * <p>TYPE Conid [arity] :: kexp
   *
   * <p>where arity is an optional integer that specifies an arity for the new type (used only to
   * provide a friendlier error if there are too many arguments) and kexp specifies the kind of the
   * primitive (uninterpreted) type.
   */
  private CoreDefn typeDefn() throws Failure {
    Position pos = lexer.getPos();
    if (lexer.nextToken(/* TYPE */ ) != CONID) {
      throw missing("type constructor name");
    }
    String id = lexer.getLexeme();
    if (lexer.nextToken(/* CONID */ ) == EQ) {
      lexer.nextToken(/* EQ */ );
      TypeExp rhs = maybeTypeExp();
      if (rhs == null) {
        throw new Failure(lexer.getPos(), "Missing right hand side in synonym definition");
      }
      return new SynonymDefn(pos, id, rhs);
    }
    int arity = Integer.MAX_VALUE; // Advisory, not required
    if (lexer.getToken() == NATLIT) {
      try {
        arity = lexer.getInt();
      } finally {
        lexer.nextToken(/* NATLIT */ );
      }
    }
    KindExp kexp;
    if (!lexer.match(COCO) || (kexp = maybeKindExp()) == null) {
      throw new Failure(lexer.getPos(), "Missing kind in primitive type definition");
    }
    return new PrimTyconDefn(pos, id, arity, kexp);
  }
}
