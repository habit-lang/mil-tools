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
package compiler;

/** A base class for building lexical analyzers that use a Source object as input. */
public abstract class SourceLexer extends Lexer {

  /** The Source object for this lexical analyzer. */
  protected Source source;

  /** Holds the text of the current line. */
  protected String line;

  /**
   * Holds the position in the current line; a zero index indicates the first character in the line,
   * while an index of line.length() indicates a "virtual EOL" at the end of the line.
   */
  protected int col = (-1);

  private SourcePosition pos;

  protected static final int EOF = -1;

  protected static final int EOL = '\n';

  protected int c;

  public SourceLexer(Handler handler, Source source) {
    super(handler);
    this.source = source;
    this.pos = new SourcePosition(source);
    this.line = source.readLine();
    nextChar();
  }

  public Position getPos() {
    return pos.copy();
  }

  protected void markPosition() {
    pos.updateCoords(source.getLineNo(), col);
  }

  protected void nextLine() {
    line = source.readLine();
    col = (-1);
    nextChar();
  }

  protected int nextChar() {
    if (line == null) {
      c = EOF;
      col = 0; // EOF is always at column 0
    } else if (++col >= line.length()) {
      c = EOL;
    } else {
      c = line.charAt(col);
    }
    return c;
  }

  protected int nextChar(int num) {
    if (line == null) {
      c = EOF;
    } else if ((col += num) >= line.length()) {
      c = EOL;
    } else {
      c = line.charAt(col);
    }
    return c;
  }

  /**
   * Move back one character in the input stream. It is only safe to call this method if a
   * corresponding character (not a newline or end of file) has previously been read. This (I
   * believe :-) is equivalent to requiring that col is strictly positive.
   */
  protected void backup() {
    if (col > 0) {
      c = line.charAt(--col);
    }
  }

  public void close() {
    if (source != null) {
      source.close();
      source = null;
    }
  }
}
