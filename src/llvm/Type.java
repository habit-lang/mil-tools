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


/** Base class for representing LLVM types. */
public abstract class Type {

  /** A class for representing primitive LLVM types. */
  private static class Basic extends Type {

    /** Name of the type. */
    private String name;

    /** Default value. */
    private Value def;

    /** Default constructor. */
    private Basic(String name, Value def) {
      this.name = name;
      this.def = def;
    }

    /** Get the name of this type as a String. */
    public String toString() {
      return name;
    }

    /** Calculate a default value of this type, suitable for use as an initial value. */
    public Value defaultValue() {
      return def;
    }
  }

  /** Represents the void type. TODO: eliminate this! */
  public static final Type vd = new Basic("void", Int.ONES);

  /** Represents the type of Boolean values. */
  public static final Type i1 = new Basic("i1", Bool.FALSE);

  /** Represents the type of 8 bit signed integer values. */
  public static final Type i8 = new Basic("i8", Int.ZERO);

  /** Represents the type of 16 bit signed integer values. */
  public static final Type i16 = new Basic("i16", Int.ZERO);

  /** Represents the type of 32 bit signed integer values. */
  public static final Type i32 = new Basic("i32", Int.ZERO);

  /** Represents the type of 64 bit signed integer values. */
  public static final Type i64 = new Basic("i64", Int.ZERO);

  /** Get the type of the ith component in this (assumed) structure type. */
  public Type at(int i) {
    debug.Internal.error("invalid at() on type: " + this);
    return null;
  }

  /** Represents a pointer type. */
  private static class PtrType extends Type {

    /** The type of value that is pointed to. */
    private Type ty;

    /** Default constructor. */
    private PtrType(Type ty) {
      this.ty = ty;
    }

    /** Get the type of value that this (assumed) pointer type points to. */
    public Type ptsTo() {
      return ty;
    }

    /** Append the name of this type to the specified buffer. */
    public void append(StringBuilder buf) {
      ty.append(buf);
      buf.append('*');
    }

    /** Calculate a default value of this type, suitable for use as an initial value. */
    public Value defaultValue() {
      return new Null(this);
    }
  }

  /** Get the type of value that this (assumed) pointer type points to. */
  public Type ptsTo() {
    debug.Internal.error("invalid ptsTo() on a type: " + this);
    return null;
  }

  /**
   * Identifies the type of pointers to this type, or null if there has not been any previous
   * reference to this pointer type.
   */
  private Type ptrType = null;

  /**
   * Return the type of pointers to values of this type. Initializes the ptrType field if necessary
   * to cache the pointer type for future uses.
   */
  public Type ptr() {
    return (ptrType == null) ? ptrType = new PtrType(this) : ptrType;
  }

  /** Get the type of the ith component in this (assumed) structure type. */
  public Type definition() {
    debug.Internal.error("invalid definition() on type: " + this);
    return null;
  }

  /** Get the name of this type as a String. */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    this.append(buf);
    return buf.toString();
  }

  /** Append the name of this type to the specified buffer. */
  public void append(StringBuilder buf) {
    buf.append(toString());
  }

  /** Append the types in the given array to the specified buffer as a comma separated list. */
  static void append(StringBuilder buf, Type[] tys) {
    for (int i = 0; i < tys.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      tys[i].append(buf);
    }
  }

  /** Calculate a default value of this type, suitable for use as an initial value. */
  public abstract Value defaultValue();

  public Type codePtrType() {
    return ptsTo().definition().at(0);
  }
}
