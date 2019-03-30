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
package llvm;

import java.io.PrintWriter;

/** Represents a function type. */
public class FunctionType extends Type {

  /** The return type (could be vd for void). */
  private Type retType;

  /** The argument types. */
  private Type[] argTypes;

  /** Default constructor. */
  public FunctionType(Type retType, Type[] argTypes) {
    this.retType = retType;
    this.argTypes = argTypes;
  }

  /** Append the name of this type to the specified buffer. */
  public void append(StringBuilder buf) {
    retType.append(buf);
    buf.append(" (");
    append(buf, argTypes);
    buf.append(")");
  }

  /** Calculate a default value of this type, suitable for use as an initial value. */
  public Value defaultValue() {
    debug.Internal.error("no default value of function type");
    return null;
  }

  void printFunDecl(PrintWriter out, String name) {
    out.print("declare ");
    out.print(retType.toString());
    out.print(" @" + name + "(");
    for (int i = 0; i < argTypes.length; i++) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(argTypes[i].toString());
    }
    out.println(")");
  }
}
