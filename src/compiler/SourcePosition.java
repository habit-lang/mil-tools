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

/**
 * A position within a source Source object.
 *
 * <p>[We might also like to have a mechanism for starting an editor at a particular position so
 * that users can view and potentially change the corresponding sections of source code.]
 */
public class SourcePosition extends Position {

  private Source source;

  private int row;

  private int column;

  public SourcePosition(Source source, int row, int column) {
    this.source = source;
    this.row = row;
    this.column = column;
  }

  public SourcePosition(Source source) {
    this(source, 0, 0);
  }

  /** Return the source for this position. */
  public Source getSource() {
    return source;
  }

  /** Return the row number for this position. */
  public int getRow() {
    return row;
  }

  /** Return the column number for this position. */
  public int getColumn() {
    return column;
  }

  /** Update the coordinates of this position. */
  public void updateCoords(int row, int column) {
    this.row = row;
    this.column = column;
  }

  /** Obtain a printable description of the source position. */
  public String describe() {
    StringBuilder buf = new StringBuilder();
    if (source != null) {
      buf.append('"');
      buf.append(source.describe());
      buf.append('"');
      if (row > 0) {
        buf.append(", ");
      }
    }
    if (row > 0) {
      buf.append("line ");
      buf.append(row);
    }
    String line = source.getLine(row);
    if (line != null) {
      buf.append('\n');
      buf.append(line);
      buf.append('\n');
      for (int i = 0; i < column; i++) {
        buf.append(' ');
      }
      buf.append('^');
    }
    return (buf.length() == 0) ? "input" : buf.toString();
  }

  /** Copy a source position. */
  public Position copy() {
    return new SourcePosition(source, row, column);
  }
}
