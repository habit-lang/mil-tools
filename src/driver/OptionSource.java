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
package driver;

import compiler.*;
import java.io.*;

/**
 * Read lines from a given reader, skipping blank lines, skipping blanks at the start of a line, and
 * skipping lines whose first non-blank character is # (indicating a comment). Return null at the
 * end of the input.
 */
public class OptionSource extends Source {

  private Reader input;

  private String description;

  /** Default constructor. */
  public OptionSource(Handler handler, Reader input, String description) {
    super(handler);
    this.input = input;
    this.description = description;
  }

  /** Buffer for input lines. */
  private StringBuilder buf;

  /** Single element buffer for next input character. */
  private int c0;

  /** Line number count. */
  private int lineNo = 0;

  /** Return current line number count. */
  public int getLineNo() {
    return lineNo;
  }

  /** Return a description of this source. */
  public String describe() {
    return description;
  }

  /** Read the next line, skipping blank lines, or returning null at the end of the file. */
  public String readLine() {
    try {
      if (buf == null) { // First time?
        buf = new StringBuilder(); // - Initialize buffer
        c0 = input.read(); // - Read first char
      } else {
        buf.setLength(0); // Else reset buffer for a new line
      }
      for (; ; ) { // Keep scanning until we find a line
        if (c0 == (-1)) { // ... or reach the end of the input
          return null;
        }
        lineNo++;
        while (c0 != (-1) && Character.isWhitespace(c0) && c0 != '\n' && c0 != '\r') {
          c0 = input.read(); // Skip leading whitespace
        }
        if (c0 == '#') { // Skip lines beginning with #
          do {
            c0 = input.read();
          } while (c0 != (-1) && c0 != '\n' && c0 != '\r');
        } else {
          while (c0 != (-1) && c0 != '\n' && c0 != '\r') {
            buf.append((char) c0); // Capture the rest of the line in buffer
            c0 = input.read();
          }
        }
        if (c0 != (-1)) { // If we reached an end of line, skip it
          c0 = input.read();
        }
        if (buf.length() > 0) { // If we read any characters, return them
          return buf.toString();
        }
      }
    } catch (IOException e) {
      handler.report(new Failure("Unable to read from " + description));
      return null;
    }
  }

  /** Close this source by closing the associated Reader. */
  public void close() {
    if (input != null) { // close the input stream
      try {
        input.close();
      } catch (IOException e) {
        /* ignore error */
      }
      input = null; // prevent further access
    }
  }
}
