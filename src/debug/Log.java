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
package debug;

import java.io.PrintStream;

/** This class provides a simple debugging log. */
public class Log {

  private static PrintStream out = null;

  private static int indent = 0;

  public static void on(PrintStream out) {
    Log.out = out;
  }

  public static void on() {
    on(System.out);
  }

  public static void off() {
    on(null);
  }

  public void indent() {
    indent++;
  }

  public void undent() {
    indent--;
  }

  private static void spaces() {
    for (int i = 0; i < indent; i++) {
      System.out.print("  ");
    }
  }

  public static void print(String msg) {
    if (out != null) {
      spaces();
      out.print(msg);
    }
  }

  public static void println(String msg) {
    if (out != null) {
      spaces();
      out.println(msg);
    }
  }
}
