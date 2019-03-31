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


/** Represents a type that is introduced in a type definition. */
public class DefinedType extends Type {

  private Type definition;

  /** Default constructor. */
  public DefinedType(Type definition) {
    this.definition = definition;
  }

  private static int count = 0;

  private String name = "%dt" + count++;

  public DefinedType() {
    this(null);
  }

  public void define(Type definition) {
    this.definition = definition;
  }

  /** Get the type of the ith component in this (assumed) structure type. */
  public Type definition() {
    return definition;
  }

  /** Get the name of this type as a String. */
  public String toString() {
    return name;
  }

  /** Calculate a default value of this type, suitable for use as an initial value. */
  public Value defaultValue() {
    return definition().defaultValue();
  }
}
