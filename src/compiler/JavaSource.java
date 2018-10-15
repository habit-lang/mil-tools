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
package compiler;

import java.io.IOException;
import java.io.Reader;

/**
 * An implementation of the Source interface that follows the low- level lexical conventions of Java
 * for Unicode escapes, etc.. Uses a Reader to obtain its input. Also expands tabs.
 */
public class JavaSource extends Source {

  private Reader input;

  private int tabwidth;

  private String description;

  private static final int DEFAULT_TABWIDTH = 8;

  public JavaSource(Handler handler, String description, Reader input, int tabwidth) {
    super(handler);
    this.description = description;
    this.input = input;
    this.tabwidth = tabwidth;
  }

  public JavaSource(Handler handler, String description, Reader input) {
    this(handler, description, input, DEFAULT_TABWIDTH);
  }

  public JavaSource(String description, Reader input) {
    this(null, description, input);
  }

  /** Return a description of this source. */
  public String describe() {
    return description;
  }

  private int c0;

  private int c1 = 0;

  private int lineNumber = 0;

  private void skip() throws IOException {
    c0 = c1;
    if (c0 != (-1)) {
      c1 = input.read();
      if (c0 == 26 && c1 == (-1)) {
        c0 = c1;
      }
    }
  }

  private StringBuilder buf;

  /**
   * Read the next line from the input stream.
   *
   * @return The next line, or null at the end of the input stream.
   */
  public String readLine() {
    if (input == null) { // Return null when done
      return null;
    }

    if (buf == null) { // Allocate or clear buffer
      buf = new StringBuilder();
    } else {
      buf.setLength(0);
    }

    try {
      if (lineNumber++ == 0) { // Set lookahead character
        skip(); // for first input line.
        skip();
      }
      if (c0 == (-1)) { // File ends at the beginning
        // TODO: is it really safe to close here (or to omit the
        // close altogether)?  The issue is that we don't want the
        // lines of text for this source to be thrown away prematurely.
        // close();
        return null; // of a line?
      }

      while (c0 != (-1) && c0 != '\n' && c0 != '\r') {
        if (c0 == '\\') {
          skip();
          if (c0 == 'u') { // Unicode escapes
            do {
              skip();
            } while (c0 == 'u');
            int n = 0;
            int i = 0;
            int d = 0;
            while (i < 4 && c0 != (-1) && (d = Character.digit((char) c0, 16)) >= 0) {
              n = (n << 4) + d;
              i++;
              skip();
            }
            if (i != 4) {
              // TODO: fix the zero column number in the following diagnostic message ...
              report(
                  new Warning(
                      new SourcePosition(this, lineNumber, 0), "Error in Unicode escape sequence"));
            } else {
              buf.append((char) n);
            }
          } else {
            buf.append('\\'); // Output the delayed backslash
          }
        } else if (c0 == '\t' && tabwidth > 0) { // Expand tabs
          int n = tabwidth - (buf.length() % tabwidth);
          for (; n > 0; n--) {
            buf.append(' ');
          }
          skip();
        } else {
          buf.append((char) c0);
          skip();
        }
      }
      if (c0 == '\r') { // Skip CR, LF, CRLF
        skip();
      }
      if (c0 == '\n') {
        skip();
      }
    } catch (IOException e) {
      close();
    }

    return buf.toString();
  }

  /** Return the current line number. */
  public int getLineNo() {
    return lineNumber;
  }

  /**
   * Close the input stream and any associated resources. The default for this method is to do
   * nothing.
   */
  public void close() {
    if (input != null) {
      try {
        input.close();
      } catch (IOException e) {
        // Should I complain?
      }
      input = null;
      buf = null;
    }
  }
}
