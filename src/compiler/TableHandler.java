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

/**
 * A simple implementation of the Handler interface that records each diagnostic that is reported in
 * a table.
 */
public class TableHandler extends Handler {

  private Diagnostic[] diags;

  private int used;

  private int maxCapacity;

  /**
   * Construct a TableHandler that will hold up to a specified number of diagnostics. A zero value
   * for maxCapacity allows the table to grow arbitrarily.
   */
  public TableHandler(int maxCapacity) {
    this.maxCapacity = maxCapacity;
    diags = new Diagnostic[(maxCapacity > 0) ? maxCapacity : 10];
    used = 0;
  }

  /** Construct a TableHandler with no limit on the number of entries it will store. */
  public TableHandler() {
    this(0);
  }

  /**
   * Return a diagnostic from the table. (The total number of table entries can be obtained using
   * the getNumDiagnostics() method.)
   */
  public Diagnostic getDiagnostic(int index) {
    return (index >= 0 && index < used) ? diags[index] : null;
  }

  /** Respond to a diagnostic by storing it in the table. */
  protected void respondTo(Diagnostic d) {
    if (maxCapacity == 0 || used < maxCapacity) {
      if (used >= diags.length) {
        Diagnostic[] newDiags = new Diagnostic[2 * diags.length];
        for (int i = 0; i < diags.length; i++) {
          newDiags[i] = diags[i];
        }
        diags = newDiags;
      }
      diags[used++] = d;
    }
  }

  /**
   * Reset this TableHandler by emptying out the table. To reduce the risk of space leaks, we don't
   * just zero out the counter; we also null out all of the entries.
   */
  public void reset() {
    super.reset();
    for (int i = 0; i < used; i++) {
      diags[i] = null;
    }
    used = 0;
  }
}
