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
package mil;

import compiler.*;
import core.*;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * An address map structure that uses a hash table to associate addresses of globals and code
 * definitions with string labels. In principle, we could use two separate hash tables here, one for
 * globals and one for code. But instead we use a single table with negative indices corresponding
 * to globals (global g has index -(1+g)) and non-negative indices corresponding to code labels.
 */
public class HashAddrMap extends AddrMap {

  private HashMap<Integer, String> map = new HashMap();

  void addGlobalLabel(int addr, String label) {
    map.put(-(1 + addr), label);
  }

  String globalLabel(int addr) {
    String s = map.get(-(1 + addr));
    return (s != null) ? s : super.globalLabel(addr);
  }

  void addCodeLabel(int addr, String label) {
    map.put(addr, label);
  }

  String codeLabel(int addr) {
    String s = map.get(addr);
    return (s != null) ? s : super.codeLabel(addr);
  }

  void dump(PrintWriter out) {
    out.println("Global symbols: ------");
    for (Integer n : map.keySet()) {
      int addr = n.intValue();
      if (addr < 0) {
        out.println("  " + (-(1 + addr)) + "\t" + map.get(n));
      }
    }
    out.println("Code symbols: --------");
    for (Integer n : map.keySet()) {
      int addr = n.intValue();
      if (addr >= 0) {
        out.println("  " + addr + "\t" + map.get(n));
      }
    }
    out.println("----------------------");
  }
}
