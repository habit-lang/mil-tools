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
import java.io.PrintWriter;

/** Class for displaying bytecode listings. */
public class BytecodeDumper extends Dumper {

  private Machine machine;

  private AddrMap addrMap;

  /** Default constructor. */
  public BytecodeDumper(Machine machine, AddrMap addrMap) {
    this.machine = machine;
    this.addrMap = addrMap;
  }

  public String description() {
    return "bytecode listing";
  }

  public void dump(PrintWriter out) {
    machine.dump(out, addrMap);
  }
}
