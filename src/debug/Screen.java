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
package debug;

/** This class provides a simple mechanism for formatted output. */
public class Screen {

  private int col = 0;

  public int getIndent() {
    return col;
  }

  public void indent(int n) {
    if (n < col) {
      System.out.println();
      col = 0;
    }
    for (; col < n; col++) {
      System.out.print(" ");
    }
  }

  public void println() {
    System.out.println();
    col = 0;
  }

  public void print(String s) {
    System.out.print(s);
    col += s.length();
  }

  public void print(int i) {
    print(Integer.toString(i));
  }

  public void print(Object[] objs) {
    if (objs.length > 0) {
      print(objs[0].toString());
      for (int i = 1; i < objs.length; i++) {
        print(" ");
        print(objs[i].toString());
      }
    }
  }

  public void space() {
    print(" ");
  }
}
