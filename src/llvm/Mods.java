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


/** Package functionality for dealing with modifiers (linkage, etc.). */
public class Mods {

  public static final int NONE = 0;

  public static final int PRIVATE = 1;

  public static final int INTERNAL = 2;

  public static final int UNNAMED_ADDR = 4;

  public static int entry(boolean isEntrypoint) {
    return isEntrypoint ? NONE : INTERNAL;
  }

  public static boolean isLocal(int mods) {
    return (mods & (PRIVATE | INTERNAL)) != 0;
  }

  public static String toString(int mods) {
    if (mods == NONE) {
      return "";
    } else {
      StringBuilder buf = new StringBuilder();
      if ((mods & PRIVATE) != 0) {
        buf.append("private ");
      }
      if ((mods & INTERNAL) != 0) {
        buf.append("internal ");
      }
      if ((mods & UNNAMED_ADDR) != 0) {
        buf.append("unnamed_addr ");
      }
      return buf.toString();
    }
  }
}
