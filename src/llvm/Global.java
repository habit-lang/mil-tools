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


/**
 * Represents a reference to a global variable. Note that this is actually a pointer to the global
 * variable, so if the variable itself has type T, then this variable will have type T*.
 */
public class Global extends Location {

  /** The name of this global variable. */
  private String name;

  /** Default constructor. */
  public Global(Type ty, String name) {
    super(ty);
    this.name = name;
  }

  /** Append the name for this value to the specified buffer. */
  public void appendName(StringBuilder buf) {
    buf.append("@");
    buf.append(name);
  }
}
