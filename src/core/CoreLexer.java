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
import java.math.BigInteger;
import java.util.HashMap;
import mil.*;

public class CoreLexer extends SourceLexer implements CoreTokens {

  protected HashMap<String, Integer> reserved;

  /** Construct a lexical analyzer for the core language. */
  public CoreLexer(Handler handler, Source source) {
    super(handler, source);
    reserved = new HashMap<String, Integer>();
    reserved.put("=", new Integer(EQ));
    reserved.put("::", new Integer(COCO));
    reserved.put("|", new Integer(BAR));
    reserved.put(".", new Integer(DOT));
    reserved.put("<-", new Integer(FROM));
    reserved.put("->", new Integer(TO));
    reserved.put("require", new Integer(REQUIRE));
    reserved.put("export", new Integer(EXPORT));
    reserved.put("entrypoint", new Integer(ENTRYPOINT));
    reserved.put("external", new Integer(EXTERNAL));
    reserved.put("data", new Integer(DATA));
    reserved.put("type", new Integer(TYPE));
    reserved.put("area", new Integer(AREA));
    reserved.put("struct", new Integer(STRUCT));
    reserved.put("bitdata", new Integer(BITDATA));
    reserved.put("aligned", new Integer(ALIGNED));
    reserved.put("case", new Integer(CASE));
    reserved.put("of", new Integer(OF));
    reserved.put("if", new Integer(IF));
    reserved.put("then", new Integer(THEN));
    reserved.put("else", new Integer(ELSE));
    reserved.put("let", new Integer(LET));
    reserved.put("in", new Integer(IN));
  }

  /** Return a printable representation of the current token. */
  public String describeToken() {
    return describeToken(token, lexemeText);
  }

  /** Return a printable representation of a token. */
  public String describeToken(int token, String lexeme) {
    switch (token) {
      case ENDINPUT:
        return "end of input";
      case POPEN:
        return "open parenthesis, \"(\"";
      case PCLOSE:
        return "close parenthesis, \")\"";
      case SOPEN:
        return "open bracket, \"[\"";
      case SCLOSE:
        return "close bracket, \"]\"";
      case BOPEN:
        return "open brace, \"{\"";
      case BCLOSE:
        return "close brace, \"}\"";
      case COMMA:
        return "comma, \",\"";
      case SEMI:
        return "semicolon, \";\"";
      case VARID:
      case CONID:
        return "identifier \"" + lexeme + "\"";
      case VARSYM:
      case CONSYM:
        return "\"" + lexeme + "\" symbol";
      case NATLIT:
        return "integer literal, " + lexeme;
      case BITLIT:
        return "bit literal, " + lexeme;
      case STRLIT:
        return "string literal, " + lexeme;
      case EQ:
        return "\"=\" symbol";
      case COCO:
        return "\"::\"";
      case BAR:
        return "\"|\" symbol";
      case DOT:
        return ". (dot operator)";
      case FROM:
        return "\"<-\" symbol";
      case TO:
        return "\"->\" symbol";
      case REQUIRE:
        return "\"require\" keyword";
      case EXPORT:
        return "\"export\" keyword";
      case ENTRYPOINT:
        return "\"entrypoint\" keyword";
      case EXTERNAL:
        return "\"external\" keyword";
      case DATA:
        return "\"data\" keyword";
      case TYPE:
        return "\"type\" keyword";
      case AREA:
        return "\"area\" keyword";
      case STRUCT:
        return "\"struct\" keyword";
      case BITDATA:
        return "\"bitdata\" keyword";
      case ALIGNED:
        return "\"aligned\" keyword";
      case CASE:
        return "\"case\" keyword";
      case OF:
        return "\"of\" keyword";
      case IF:
        return "\"if\" keyword";
      case THEN:
        return "\"then\" keyword";
      case ELSE:
        return "\"else\" keyword";
      case LET:
        return "\"let\" keyword";
      case IN:
        return "\"in\" keyword";
    }
    return (lexeme == null) ? "lexeme" : ("lexeme, \"" + lexeme + "\"");
  }

  /** Read the next token and return the corresponding integer code. */
  public int nextToken() {
    for (; ; ) {
      skipWhitespace();
      markPosition();
      lexemeText = null;
      switch (c) {
        case EOF:
          return token = ENDINPUT;

        case '(':
          nextChar();
          return token = POPEN;
        case ')':
          nextChar();
          return token = PCLOSE;

        case '[':
          nextChar();
          return token = SOPEN;
        case ']':
          nextChar();
          return token = SCLOSE;

        case '{':
          nextChar();
          if (c == '-') {
            skipNestedComment();
            break;
          }
          return token = BOPEN;
        case '}':
          nextChar();
          return token = BCLOSE;

        case ',':
          nextChar();
          return token = COMMA;
        case ';':
          nextChar();
          return token = SEMI;

        case '\"':
          try {
            lexemeText = stringLiteral();
            return token = STRLIT;
          } catch (Failure f) {
            report(f);
          }
          break;

        case '\'':
          try {
            charLiteral();
            return token = NATLIT;
          } catch (Failure f) {
            report(f);
          }
          break;

        case '-':
          {
            int start = col;
            nextChar();
            if (c == '-') {
              /* Haskell is, IMHO, broken in its attempt to apply maximal munch to one
               * line comments.  But, in the interests of avoiding unneccessary
               * differences, we follow its lead here.  A better approach would be to
               * replace this block with: nextLine(); break;
               * TODO: revisit this decision!
               */
              do {
                nextChar();
              } while (c == '-');
              if (!isOpsym((char) c)) {
                nextLine();
                break;
              }
              nextChar();
            }
            return symbol(start);
          }

        default:
          if (Character.isJavaIdentifierStart((char) c)) {
            return identifier(col);
          } else if (isOpsym((char) c)) {
            nextChar();
            return symbol(col - 1);
          } else if (Character.digit((char) c, 10) >= 0) {
            return number();
          } else {
            report(new Warning(getPos(), "Ignoring illegal character"));
            nextChar();
          }
      }
    }
  }

  private boolean isWhitespace(int c) {
    return (c == ' ') || (c == '\t') || (c == '\f');
  }

  private void skipWhitespace() {
    while (isWhitespace(c)) {
      nextChar();
    }
    while (c == EOL) {
      nextLine();
      while (isWhitespace(c)) {
        nextChar();
      }
    }
  }

  /**
   * Skip over a nested comment between {- and -}, allowing nested pairs of the same tags in
   * between. We assume that the leading "{" character has already been read and the following "-"
   * has been detected.
   */
  private void skipNestedComment() { // Assumes c=='-'
    nextChar(/* - */ );
    // TODO: This would be the place to look for pragma annotations ...
    for (int nesting = 1; nesting > 0 && c != EOF; ) {
      if (c == '{') {
        nextChar();
        if (c == '-') {
          nextChar();
          nesting++;
        }
      } else if (c == '-') {
        nextChar();
        if (c == '}') {
          nextChar();
          nesting--;
        }
      } else if (c == EOL) {
        nextLine();
      } else {
        nextChar();
      }
    }
    if (c == EOF) {
      report(new Failure(getPos(), "Unterminated comment"));
    }
  }

  private int number() { // Assumes c is a decimal digit
    int start = col;

    if (c == '0') { // Possible hexadecimal or octal literal
      nextChar();
      if (c == 'x' || c == 'X') {
        nextChar();
        digits("hexadecimal literal", 16);
        multiplier();
        return token = NATLIT;
      } else if (c == 'o' || c == 'O') {
        nextChar();
        digits("octal literal", 8);
        multiplier();
        return token = NATLIT;
      } else if (c == 'b' || c == 'B') {
        nextChar();
        digits("binary literal", 2);
        multiplier();
        return token = NATLIT;
      } else if (Character.digit((char) c, 10) < 0) { // just a zero
        nat = bigD[0];
        return token = NATLIT;
      }
    }
    digits("numeric literal", 10); // read decimal prefix
    multiplier();
    return token = NATLIT;
  }

  private BigInteger nat;

  /** Return the most recently recognized numeric literal value as a BigInteger. */
  public BigInteger getNat() {
    return nat;
  }

  /**
   * Return the most recently recognized literal as an int, or throw an exception if the value is
   * out of range.
   */
  public int getInt() throws Failure {
    if (nat.compareTo(MAX_INT) > 0) {
      throw new Failure(getPos(), "Literal is too large (limit is " + MAX_INT + ")");
    }
    return nat.intValue();
  }

  /** A BigInteger for the largest positive value that can be represented by a (signed) int. */
  public static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);

  /**
   * Return the most recently recognized literal as a Word, or throw an exception if the value is
   * out of range.
   */
  public long getWord() throws Failure {
    if (nat.compareTo(Word.maxUnsigned()) > 0) {
      throw new Failure(getPos(), "Literal is too large (limit is " + Word.maxUnsigned() + ")");
    }
    return nat.longValue();
  }

  private static BigInteger[] bigD = new BigInteger[17];

  private static BigInteger kb;

  private static BigInteger mb;

  private static BigInteger gb;

  private static BigInteger tb;

  static {
    for (int i = 0; i < 17; i++) {
      bigD[i] = BigInteger.valueOf((long) i);
    }
    kb = BigInteger.valueOf((long) 1024);
    mb = kb.multiply(kb);
    gb = mb.multiply(kb);
    tb = gb.multiply(kb);
  }

  /**
   * Generate a printable representation of a big integer following the conventions used for lexical
   * analysis. TODO: should this code be elsewhere?
   */
  public static String bigToString(BigInteger val) {
    BigInteger v = val.shiftRight(40);
    if (v.shiftLeft(40).equals(val)) {
      return v + "T";
    }
    v = val.shiftRight(30);
    if (v.shiftLeft(30).equals(val)) {
      return v + "G";
    }
    v = val.shiftRight(20);
    if (v.shiftLeft(20).equals(val)) {
      return v + "M";
    }
    v = val.shiftRight(10);
    if (v.shiftLeft(10).equals(val)) {
      return v + "K";
    }
    return val.toString();
  }

  /**
   * Recognize a sequence of one or more digits of a particular radix, setting nat to the
   * corresponding numeric value.
   */
  private void digits(String where, int radix) {
    int count = 0;
    nat = bigD[0];

    // Read main digit sequence:
    for (; ; ) {
      int d = Character.digit((char) c, radix);
      if (d >= 0) { // valid digit?
        count++;
        nat = nat.multiply(bigD[radix]).add(bigD[d]);
      } else if (c != '_') { // skip underscore inside an integer literal
        break;
      }
      nextChar();
    }

    // Literals must contain at least one digit (after any prefix)!
    if (count == 0) {
      report(new Failure(getPos(), "Missing digits for " + where));
    }
  }

  private void multiplier() {
    // Look for a trailing power of two multiplier:
    if (c == 'K') {
      nat = nat.multiply(kb);
    } else if (c == 'M') {
      nat = nat.multiply(mb);
    } else if (c == 'G') {
      nat = nat.multiply(gb);
    } else if (c == 'T') {
      nat = nat.multiply(tb);
    } else {
      return;
    }
    nextChar();
  }

  private String stringLiteral() throws Failure { // Assumes c=='\"'
    nextChar();
    StringBuilder buf = new StringBuilder();
    for (; ; ) {
      if (c == '\"') {
        nextChar();
        return buf.toString();
      } else if (c == EOL || c == EOF) {
        throw new Failure(getPos(), "Unterminated String literal");
      } else if (c == '\\') {
        if (nextChar() == '&') {
          nextChar();
        } else if (isWhitespace(c) || c == EOL) {
          skipWhitespace(); // skip gap
          if (c == '\\') {
            nextChar();
          } else {
            throw new Failure(getPos(), "Missing backslash at the end of gap");
          }
        } else {
          buf.append((char) escapeChar());
        }
      } else {
        buf.append((char) c);
        nextChar();
      }
    }
  }

  private void charLiteral() throws Failure { // Assumes c=='\''
    int val = nextChar();
    if (c == '\\') {
      nextChar();
      val = escapeChar();
    } else if (c == '\'' || c == EOL || c == EOF) {
      throw new Failure(getPos(), "Syntax error in character literal");
    } else {
      nextChar();
    }
    if (c == '\'') {
      nextChar();
    } else {
      throw new Failure(getPos(), "Missing ' (close quote) on character literal");
    }
    nat = BigInteger.valueOf(val);
  }

  private int escapeChar() throws Failure { // Assumes initial '\\' has been skipped already
    switch (c) {
      case 'o':
        nextChar();
        digits("octal character escape", 8);
        return getInt();

      case 'x':
        nextChar();
        digits("hexadecimal character escape", 16);
        return getInt();

      case '^':
        {
          int v = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_".indexOf(nextChar());
          if (v < 0) {
            throw new Failure(getPos(), "Missing control character in \\^ escape");
          }
          nextChar();
          return v;
        }

      default:
        if (Character.digit((char) c, 10) >= 0) {
          digits("decimal character escape", 10);
          return getInt();
        }
        for (int i = 0; i < escCodes.length; i++) {
          String str = escCodes[i];
          int len = str.length() - 2; // subtract one for init char, one for space
          if (str.regionMatches(2, line, col, len)) {
            nextChar(len);
            return str.charAt(0);
          }
        }
        throw new Failure(getPos(), "Unrecognized character escape");
    }
  }

  private static final String[] escCodes =
      new String[] {
        // charesc
        "\007 a",
        "\010 b",
        "\014 f",
        "\012 n",
        "\015 r",
        "\011 t",
        "\\ \\",
        "\" \"",
        "\' \'",
        "\013 v",

        // ascii
        "\000 NUL",
        "\001 SOH",
        "\002 STX",
        "\003 ETX",
        "\004 EOT",
        "\005 ENQ",
        "\006 ACK",
        "\007 BEL",
        "\010 BS",
        "\011 HT",
        "\012 LF",
        "\013 VT",
        "\014 FF",
        "\015 CR",
        "\016 SO",
        "\017 SI",
        "\020 DLE",
        "\021 DC1",
        "\022 DC2",
        "\023 DC3",
        "\024 DC4",
        "\025 NAK",
        "\026 SYN",
        "\027 ETB",
        "\030 CAN",
        "\031 EM",
        "\032 SUB",
        "\033 ESC",
        "\034 FS",
        "\035 GS",
        "\036 RS",
        "\037 US",
        "\040 SP",
        "\177 DEL"
      };

  private int identifier(int start) { // Assumes isJavaIdentifierStart(c)
    token = Character.isUpperCase(c) ? CONID : VARID;
    do {
      nextChar();
    } while (c != EOF && Character.isJavaIdentifierPart((char) c));
    lexemeText = line.substring(start, col);
    Integer kw = reserved.get(lexemeText);
    if (kw != null) {
      return token = kw.intValue();
    } else if (isBitLiteral(lexemeText)) {
      return token = BITLIT;
    } else if (Character.isUpperCase(lexemeText.charAt(0))) {
      return token = CONID;
    } else {
      return token = VARID;
    }
  }

  private int numBits;

  public int getNumBits() {
    return numBits;
  }

  /**
   * Test to determine whether a given identifier represents a bit vector literal. If the result is
   * true, then the width (total number of bits) is placed in numBits and the value is placed in
   * nat.
   */
  private boolean isBitLiteral(String s) {
    int width;
    switch (s.charAt(0)) {
      case 'B':
        width = 1;
        break;
      case 'O':
        width = 3;
        break;
      case 'X':
        width = 4;
        break;
      default:
        return false;
    }

    int radix = 1 << width;
    numBits = 0;
    nat = bigD[0];
    int l = s.length();
    for (int i = 1; i < l; i++) {
      char c = s.charAt(i);
      int d = Character.digit(c, radix);
      if (d >= 0) {
        numBits += width;
        nat = nat.multiply(bigD[radix]).add(bigD[d]);
      } else if (c != '_') {
        return false;
      }
    }
    return numBits > 0;
  }

  private int symbol(int start) {
    token = (c == ':') ? CONSYM : VARSYM;
    while (isOpsym((char) c)) {
      nextChar();
    }
    if (Character.isJavaIdentifierStart((char) c) && line.charAt(col - 1) == '$') {
      do {
        nextChar();
      } while (c != EOF && Character.isJavaIdentifierPart((char) c));
    }
    lexemeText = line.substring(start, col);
    Integer kw = reserved.get(lexemeText);
    return (kw == null) ? token : (token = kw.intValue());
  }

  private boolean isOpsym(char c) {
    if (c < 128) {
      switch (c) {
          // 35 is the ASCII code for '#'
        case '!':
        case 35:
        case '$':
        case '%':
        case '&':
        case '*':
        case '+':
        case '.':
        case '/':
        case '<':
        case '=':
        case '>':
        case '?':
        case '@':
        case '\\':
        case '^':
        case '|':
        case '-':
        case '~':
        case ':':
          return true;
      }
    } else {
      switch (Character.getType(c)) {
        case Character.DASH_PUNCTUATION:
        case Character.START_PUNCTUATION:
        case Character.END_PUNCTUATION:
        case Character.CONNECTOR_PUNCTUATION:
        case Character.OTHER_PUNCTUATION:
        case Character.MATH_SYMBOL:
        case Character.CURRENCY_SYMBOL:
        case Character.MODIFIER_SYMBOL:
        case Character.OTHER_SYMBOL:
          return true;
      }
    }
    return false;
  }

  public void enterSection() {
    if (!this.match(BOPEN)) {
      report(new Failure(getPos(), "Missing open brace, '{'"));
    }
  }

  public void itemEnd(String where) {
    if (token != ENDINPUT && token != SEMI && token != BCLOSE) {
      Position pos = getPos(); // location of first skipped token
      int nest = 1;
      do {
        if (nextToken() == BOPEN) {
          nest++;
        } else if (token == BCLOSE) {
          nest--;
        }
      } while (nest > 0 && !(token == SEMI && nest == 1) && token != ENDINPUT);
      if (where != null) {
        report(new Failure(pos, "Syntax error in " + where));
      }
    }
  }

  public void leaveSection() {
    if (!this.match(BCLOSE)) {
      report(new Failure(getPos(), "Missing close brace, '}'"));
    }
  }
}
