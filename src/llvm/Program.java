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
package llvm;

import java.io.IOException;
import java.io.PrintWriter;

/** Represents an LLVM program comprising a sequence of definitions. */
public class Program {

  private Defns defns = null;

  private Defns defnsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(Defn elem) {
    Defns ns = new Defns(elem, null);
    defnsLast = (defnsLast == null) ? (defns = ns) : (defnsLast.next = ns);
  }

  static void printComment(PrintWriter out, String indent, String comment) {
    // Attempt to print a comment over multiple lines if necessary by interpreting embedded newlines
    boolean indented = false;
    int len = comment.length();
    for (int i = 0; i < len; i++) {
      char c = comment.charAt(i);
      if (c == '\n') {
        if (indented) {
          out.println();
          indented = false;
        }
      } else {
        if (!indented) {
          out.print(indent);
          out.print("; ");
          indented = true;
        }
        out.print(c);
      }
    }
    if (indented) {
      out.println();
    }
  }

  /** Write a description of this LLVM program to standard output. */
  public void dump() {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  /** Write a description of this LLVM program to a named file. */
  public void dump(String name) {
    try {
      PrintWriter out = new PrintWriter(name);
      dump(out);
      out.close();
    } catch (IOException e) {
      System.out.println("Attempt to create llvm output in \"" + name + "\" failed");
    }
  }

  /** Write a description of this LLVM program to an arbitrary PrintWriter. */
  public void dump(PrintWriter out) {
    // TODO: write general headers here
    for (Defns ds = defns; ds != null; ds = ds.next) {
      ds.head.print(out);
    }
  }

  private static int count = 0;

  public String freshName(String prefix) {
    return prefix + "." + count++;
  }
}
