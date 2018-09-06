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

/** Class for executing and displaying output from bytecode programs. */
public class ExecDumper extends Dumper {

  private Machine machine;

  private int pc;

  /** Default constructor. */
  public ExecDumper(Machine machine, int pc) {
    this.machine = machine;
    this.pc = pc;
  }

  public String description() {
    return "bytecode execution log";
  }

  public void dump(PrintWriter out) {
    machine.exec(out, pc);
  }
}
