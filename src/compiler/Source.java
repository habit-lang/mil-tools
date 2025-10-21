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
package compiler;

/** A base class for line-oriented source input. */
public abstract class Source extends Phase {

  /** Construct a new source input phase with a specified handler. */
  public Source(Handler handler) {
    super(handler);
  }

  /** Return a printable description of the source. */
  public abstract String describe();

  /**
   * Read the next line from the input stream.
   *
   * @return The next line, or null at the end of the input stream.
   */
  public abstract String readLine();

  /**
   * Return the current line number. By convention, we use 0 to indicate the absence of a line
   * number; counting for regular lines begins at 1.
   */
  public abstract int getLineNo();

  /**
   * Return the text of a specific line, if it is available. If the requested source line is not
   * available, then a null is returned. This mechanism is useful with sources that cache either the
   * text of each line that has been read, or perhaps just the offsets of each line within a given
   * file. A console input source may use a queue like structure to provide history for the most
   * recently entered line. A source that uses an array of Strings as its input, or perhaps accesses
   * the text from a text editor's buffer in an IDE, might be able to offer up lines through this
   * method that have not yet been read using readLine(). On the other hand, some sources might
   * choose to return null for every call to getLine(), which is the default behavior specified
   * below, or to flush any caches of line data after the source has been closed.
   */
  public String getLine(int lineNo) {
    return null;
  }

  /**
   * Close the input stream and any associated resources. The default for this method is to do
   * nothing.
   */
  public void close() {
    // Do nothing!
  }
}
