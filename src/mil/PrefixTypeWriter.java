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
package mil;

import compiler.*;
import core.*;

public abstract class PrefixTypeWriter extends TypeWriter {

  private Prefix prefix;

  /** Default constructor. */
  /**
   * Initialize this PrefixTypeWriter by choosing distinct names for each of the generic type
   * variables.
   */
  public PrefixTypeWriter(Prefix prefix) {
    this.prefix = prefix;

    ids = new String[prefix.numGenerics()];
    for (int i = 0; i < ids.length; i++) {
      // try to generate sensible names for generic variables based on hints provided by their
      // original
      // names, ignoring any numeric suffix.
      ids[i] = chooseName(dropDigitSuffix(prefix.getGen(i).getId()));
    }
  }

  /** The array of names that we will use for generic variables. */
  private String[] ids;

  /** Remove any trailing numeric digits from the end of this name. */
  private static String dropDigitSuffix(String str) {
    int n = str.length() - 1;
    if (n > 0 && Character.isDigit(str.charAt(n))) {
      while (--n > 0 && Character.isDigit(str.charAt(n))) ;
      return str.substring(0, n + 1);
    }
    return str;
  }

  /**
   * Choose a name for a generic variable, starting with the "ideal" choice, but adding a numeric
   * suffix if necessary to ensure that the chosen name does not conflict with any names already in
   * use.
   */
  private String chooseName(String ideal) {
    if (ideal == null || ideal.length() == 0) { // Given an empty ideal, try to generate
      for (int suffix = -1; ; suffix++) { // a name with a single letter followed
        for (char c = 'a'; c <= 'z'; c++) { // by a numeric suffix
          String s = Character.toString(c);
          if (suffix >= 0) {
            s += suffix;
          }
          if (nameAvailable(s)) {
            return s;
          }
        }
      }
    } else {
      String alt = ideal; // Try the name as written
      if (!nameAvailable(alt)) { // If that's not available ...
        int suffix = 0;
        do {
          alt = ideal + suffix++; // ... add a numeric suffix
        } while (!nameAvailable(alt));
      }
      return alt;
    }
  }

  /**
   * Test to see if the specified candidate name is available (i.e., is not already used for another
   * generic).
   */
  private boolean nameAvailable(String candidate) {
    for (int i = 0; i < ids.length && ids[i] != null; i++) {
      if (ids[i].equals(candidate)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Output a name for the nth generic variable using the identifiers that have been chosen in ids
   * where possible.
   */
  void writeTGen(int n) {
    if (ids != null && n >= 0 && n < ids.length) {
      write(ids[n]);
    } else {
      super.writeTGen(n);
    }
  }

  /** Output a quantifier list of quantifiers corresponding to the given prefix. */
  void writeQuantifiers() {
    int n = prefix.numGenerics();
    if (n > 0) {
      write("forall");
      for (int i = 0; i < n; i++) {
        write(" (");
        writeTGen(i);
        write(" :: ");
        write(prefix.getGen(i).getKind().toString());
        write(")");
      }
      write(". ");
    }
  }
}
