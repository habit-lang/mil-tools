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

public class LayoutLexer extends CoreLexer implements CoreTokens {

  /**
   * Construct a layout-aware lexical analyzer.
   *
   * @param optional if this is set to true, then layout is optional. Otherwise, the use of layout
   *     is mandatory to begin and end sections. In the interests of faster tests, the setting of
   *     optional is not recorded in each object of the class; instead, it is used here in the
   *     constructor to make a one-time choice for the values of beginToken and endToken.
   */
  public LayoutLexer(Handler handler, boolean optional, Source source) {
    super(handler, source);
    if (optional) {
      beginToken = BOPEN;
      endToken = BCLOSE;
    } else {
      beginToken = NOTOKEN;
      endToken = NOTOKEN;
    }
  }

  /**
   * Records the token that is used to begin a section with explicit layout. Setting this and end to
   * NOTOKEN results in a lexer that supports only implicit layout.
   */
  private int beginToken;

  /** Records the token that is used to end a section with explicit layout. */
  private int endToken;

  /**
   * NOTOKEN is a dummy token code that we avoid using for any real token. It is used to flag the
   * absence of a delayedToken, or to avoid returning the code for a real token when an implicit END
   * is required.
   */
  private static final int NOTOKEN = 47;

  /**
   * SOFTEND is a dummy token code that is used to indicate that an implicit '}' has been inserted
   * at the end of a layout section.
   */
  private static final int SOFTEND = 48;

  public String describeToken(int token, String lexeme) {
    switch (token) {
      case NOTOKEN:
        return "(missing token)";
      case SOFTEND:
        return "(automaticaly inserted) ';'";
      default:
        return super.describeToken(token, lexeme);
    }
  }

  /**
   * A stack of integer values (which can be expanded as necessary) records the column numbers for
   * current uses of the offside rule. The intial size of the stack specified here is almost
   * completely arbitrary; the only firm constraint is that it should be at least one so that a
   * doubling of the array size makes sense in the push() method below. By choosing a slightly
   * larger value, we will be able to read many inputs without ever having to reallocate space for
   * the stack; of course, this won't make any real difference in practice!
   */
  private int[] indents = new int[10];

  /**
   * The current nesting level serves as a stack pointer into the array of indents, pointing to the
   * most recently saved value. Thus the top of the stack can be read from indents[nesting], we can
   * test for a non-empty stack using nesting>=0, and we can pop from the stack using nesting--.
   */
  private int nesting = (-1);

  /**
   * A special column number, HARD, is used as the indentation for an explicit '{' token in the
   * input stream. On the assumption that all real column numbers are greater than or equal to 0, it
   * suffices to pick a negative number as the value of HARD. Note then that every (real) token will
   * be considered as starting to the right of a HARD indent.
   */
  private static final int HARD = (-1);

  /**
   * Indentations are added to the offside stack by calling the push() method, which automatically
   * expands the amount of memory allocated to the stack if necessary.
   */
  private void push(int indent) {
    if (++nesting == indents.length) {
      int[] newIndents = new int[2 * indents.length];
      for (int i = 0; i < indents.length; i++) {
        newIndents[i] = indents[i];
      }
      indents = newIndents;
    }
    indents[nesting] = indent;
  }

  /**
   * Enter a section of input text that is (potentially) subject to layout. In essence, a call to
   * enterSection() is equivalent to: <code>
   *    nextToken(); // skip '{' at beginning of layout block
   *  </code> except that the '{' token here may not actually appear in the input text; instead, the
   * lexer inserts an implicit '{', if it is needed.
   */
  public void enterSection() {
    if (token == beginToken) {
      super.nextToken();
      push(HARD);
    } else {
      push(getPos().getColumn());
    }
  }

  /**
   * Check for the end of a section, which might require skipping tokens in an attempt to recover
   * from a syntax error. Note that we can assume nesting>=0, because this method will (i.e.,
   * should) only be called from within a section.
   */
  public void itemEnd(String where) {
    // If we're at a previously inserted end section or at a SEMI, then we're done.
    if (token == SOFTEND || token == SEMI) {
      return;
    }

    // If we're in a soft section, we should insert a soft end to avoid a syntax error.
    if (indents[nesting] != HARD) {
      delayedToken = token;
      token = SOFTEND;
      return;
    }

    // Otherwise we must be in a hard section, and not at a SEMI.  So if we don't find
    // a '}' here, then we're going to need to skip some tokens.
    if (token != BCLOSE) {
      super.itemEnd(where);
    }
  }

  /**
   * Leave a section of input text that is (potentially) subject to layout. We assume that this
   * method will only be called as a last resort, and that no explicit test for the closing '}' will
   * have been made (because it might not be inserted until we call this method!). We also assume
   * that this function will only be invoked after a matching call to enterSection().
   */
  public void leaveSection() {
    // From the assumption that there was a previous enterSection() call, we can assume that
    // nesting>=0.
    if (indents[nesting--] == HARD) {
      if (token == endToken) {
        nextToken();
      } else {
        report(new Failure(getPos(), "Missing close brace, '}'"));
      }
    } else {
      if (token == SOFTEND) {
        nextToken();
      }
    }
  }

  /**
   * We always assume that a user won't call nextToken() until they have recognized that token as
   * part of a valid input. In particular, this should mean that a parser will not move past a
   * NOTOKEN that is returned when an implicit close of layout is encountered.
   */
  public int nextToken() {
    if (delayedToken != NOTOKEN) {
      int lastToken = token;
      token = delayedToken;
      delayedToken = NOTOKEN;
      if (lastToken == SEMI) {
        return token;
      }
    } else {
      token = super.nextToken();
    }
    if (nesting >= 0) {
      int startCol = getPos().getColumn();
      if (startCol == indents[nesting]) {
        switch (token) {
            // Allows insertion of semicolons to be disabled for
            // specific tokens.
          default:
            delayedToken = token;
            token = SEMI;
            // Add tokens that do suppress an inserted SEMI here
            // using the DelayTokens macro.
          case THEN:
          case ELSE:
          case OF:
          case IN:
            break;
        }
      } else if (startCol < indents[nesting]) {
        delayedToken = token;
        token = SOFTEND;
      }
    }
    return token;
  }

  /**
   * Records the code of a token that has been delayed so that another token can be 'inserted' as a
   * result of layout.
   */
  private int delayedToken = NOTOKEN;
}
