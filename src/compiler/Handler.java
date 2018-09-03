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
 * Represents a handler for diagnostics. In particular applications, we can use subclasses to
 * specify how diagnostics should be handled.
 */
public abstract class Handler {

  /** Count how many diagnostics have been reported. */
  private int numDiagnostics = 0;

  public int getNumDiagnostics() {
    return numDiagnostics;
  }

  /** Count how many failures have been reported. */
  private int numFailures = 0;

  public int getNumFailures() {
    return numFailures;
  }

  /** Signal whether failures have been detected. */
  public boolean hasFailures() {
    return numFailures > 0;
  }

  /** Throw a failure to abort compilation if this handler has already reported failures. */
  public void abortOnFailures() throws Failure {
    if (numFailures > 0) {
      if (numFailures == 1) {
        throw new Failure("Aborting after previously reported error");
      }
      throw new Failure("Aborting after " + numFailures + " previously reported errors");
    }
  }

  /** Report a problem to this diagnostic handler. */
  public void report(Diagnostic d) {
    numDiagnostics++;
    if (d instanceof Failure) {
      numFailures++;
    }
    respondTo(d);
  }

  /**
   * Respond to a diagnostic report. Subclasses should override this method to deal with diagnostic
   * reports in an appropriate way. Diagnostics will normally be passed to this method indirectly
   * via a call to report().
   */
  protected abstract void respondTo(Diagnostic d);

  /**
   * Reset the diagnostic handler. This should set the diagnostic handler back to the state of a
   * freshly created handler. As a default, we just reset the counters.
   */
  public void reset() {
    numDiagnostics = 0;
    numFailures = 0;
  }
}
