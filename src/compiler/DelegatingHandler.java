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
 * A diagnostic handler that counts diagnostics and then passes them on to another handler. A
 * collection of DelegatingHandlers can be used to track the number of errors that occurred in
 * different phases, and then passing them to a shared handler that collects or reports on all
 * diagnostics as a single unit.
 */
public class DelegatingHandler extends Handler {

  private Handler handler;

  /** Construct a delegating handler. */
  public DelegatingHandler(Handler handler) {
    this.handler = handler;
  }

  /** Respond to a diagnostic. */
  protected void respondTo(Diagnostic d) {
    handler.report(d);
  }
}
