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
package core;

import compiler.*;
import java.io.File;
import mil.*;

public abstract class Loader {

  /** Search path to use for locating input files. */
  protected String[] searchPath = null;

  /** Return the search path for this loader. */
  public void setSearchPath(String[] searchPath) {
    this.searchPath = searchPath;
  }

  /** Return a printable version of the search path for this loader. */
  public String showSearchPath() {
    StringBuilder buf = new StringBuilder();
    if (searchPath != null && searchPath.length > 0) {
      buf.append(searchPath[0]);
      for (int i = 1; i < searchPath.length; i++) {
        buf.append(":");
        buf.append(searchPath[i]);
      }
    }
    return buf.toString();
  }

  /**
   * Extend the search path for this loader to include paths listed in the given string, starting at
   * offset s.
   */
  public void extendSearchPath(String str, int s) {
    int n = 0; // Calculate number of separator characters
    for (int i = s; (i = str.indexOf(File.pathSeparatorChar, i)) >= 0; i++) {
      n++;
    }
    int m = (searchPath == null) ? 0 : searchPath.length;
    String[] newsp = new String[m + n + 1];
    for (int i = 0; i < m; i++) {
      newsp[i] = searchPath[i];
    }
    for (int j = 0; j < n; j++) {
      int e = str.indexOf(File.pathSeparatorChar, s);
      newsp[m + j] = str.substring(s, e);
      s = e + 1;
    }
    newsp[m + n] = str.substring(s);
    setSearchPath(newsp);
  }

  /** Search for the named file. */
  public String findFile(Handler handler, String name) {
    if (searchPath != null) {
      debug.Log.println(
          "Searching for \"" + name + "\" with search path \"" + showSearchPath() + "\"");
      File f = new File(name);
      if (!f.isFile() || !f.canRead()) {
        for (int i = 0; i < searchPath.length; i++) {
          debug.Log.println("Searching for \"" + name + "\" in folder \"" + searchPath[i] + "\"");
          f = new File(searchPath[i] + File.separator + name);
          try {
            if (f.isFile() && f.canRead()) {
              String path = f.getPath();
              debug.Log.println("Found file in \"" + path + "\"");
              return path;
            }
          } catch (Exception e) {
            /* ignore exceptions due to invalid paths or IO errors. */
          }
        }
        handler.report(new Failure("Unable to locate \"" + name + "\" on current search path"));
      }
    }
    return name;
  }
}
