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

public abstract class TypeWriter {

  /** Precedence value to indicate that parens are not required. */
  public static final int NEVER = Integer.MIN_VALUE;

  /** Precedence value to indicate that parens are required. */
  public static final int ALWAYS = Integer.MAX_VALUE;

  /**
   * A precedence value for function arrows (any value between NEVER and ALWAYS would do for now.
   */
  public static final int FUNPREC = 5;

  /**
   * Output a string as part of this type; the intention is that we can override this method to
   * implement a TypeWriter that will print on the screen, or to a string buffer, etc. as necessary.
   */
  abstract void write(String str);

  /** Output a representation of a generic variable. */
  void writeTGen(int n) {
    write("_" + n);
  }

  /**
   * Maintains a stack of type arguments as we decode the TAp structure of a given Type value. We
   * choose a small initial size but allow this to increase dynamically for more complex types that
   * require a larger stack. (TODO: in the "idle curiousity" category: I wonder what the
   * distribution of maximum stack sizes actually needed will look like in practice ...)
   */
  private Type[] stack = new Type[4];

  private int sp = 0;

  /** Push an argument on to the stack. */
  void push(Type t) {
    if (sp >= stack.length) {
      Type[] newstack = new Type[2 * stack.length];
      for (int i = 0; i < stack.length; i++) {
        newstack[i] = stack[i];
      }
      stack = newstack;
    }
    stack[sp++] = t;
  }

  /** Pop an argument off of the stack. */
  Type pop() {
    return stack[--sp];
  }

  /** Write an open parenthesis, if required. */
  void open(boolean b) {
    if (b) write("(");
  }

  /** Write a close parenthesis, if required. */
  void close(boolean b) {
    if (b) write(")");
  }
}
