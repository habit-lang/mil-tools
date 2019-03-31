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

/** A source input phase that reads lines of input from standard input. */
public class StdinSource extends Source {

  /** Construct a standard input source with a specified diagnostic handler. */
  public StdinSource(Handler handler) {
    super(handler);
  }

  /** Return a description of this source. */
  public String describe() {
    return "standard input";
  }

  /** Flag indicates when the end of input has been found. */
  private boolean foundEOF = false;

  /** Counter records numbers of each line that is returned. */
  private int lineNumber = 0;

  /** A StringBuilder that is used to store input lines as they are read. */
  private StringBuilder buf = new StringBuilder();

  /**
   * Read the next line from the input stream.
   *
   * @return The next line, or null at the end of the input stream.
   */
  public String readLine() {
    if (foundEOF) {
      return null;
    }
    lineNumber++;
    buf.setLength(0);
    for (; ; ) {
      int c = 0;
      try {
        c = System.in.read();
      } catch (Exception e) {
        report(new Failure("Error in input stream"));
      }
      if (c == '\n') {
        break;
      } else if (c < 0) {
        foundEOF = true;
        break;
      }
      buf.append((char) c);
    }
    return buf.toString();
  }

  /** Return the current line number. */
  public int getLineNo() {
    return lineNumber;
  }
}
