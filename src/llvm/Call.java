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


/** Function calls. */
public class Call extends Rhs {

  /** Option settings. Current use limited to true ==> insert "tail" marker. */
  private boolean options;

  /** The type of value that will be returned. */
  private Type ty;

  /** The function value to be called. */
  private Value func;

  /** The arguments to this call. */
  private Value[] args;

  /** Default constructor. */
  public Call(boolean options, Type ty, Value func, Value[] args) {
    this.options = options;
    this.ty = ty;
    this.func = func;
    this.args = args;
  }

  public Call(Type ty, Value func, Value[] args) {
    this(false, ty, func, args);
  }

  /** Append a printable string for this instruction to the specified buffer. */
  public void append(StringBuilder buf) {
    if (options) {
      buf.append("tail ");
    }
    buf.append("call ");
    ty.append(buf);
    buf.append(" ");
    func.appendName(buf);
    Value.append(buf, "(", args, ")");
  }
}
