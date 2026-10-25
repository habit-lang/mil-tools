/*
    Copyright 2018-26 Mark P Jones, Portland State University

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

/** Represents an area containing a character string. */
public class StringArea extends Area {

  /** The string in this area. */
  private String str;

  /** Default constructor. */
  public StringArea(Position pos, String id, String str) {
    super(pos, id);
    this.str = str;
  }

  private static int count = 0;

  public static final Type refString = Type.ref(Tycon.string.asType());

  private Type expected = refString;

  /** Construct a string area for an anonymous string literal. */
  public StringArea(Position pos, String str) {
    this(pos, "str" + count++, str);
    declared = StringArea.refString;
  }

  /** Find the list of Defns that this Defn depends on. */
  Defns dependencies() {
    return null;
  }

  /** Display a printable representation of this definition on the specified PrintWriter. */
  void dump(PrintWriter out) {
    dumpHeader(out, isEntrypoint);
    out.print(id + " <- \"");
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      // TODO: This code deals with details of concrete syntax that
      // seem out of place here ...
      switch (c) {
        case '"':
          out.print("\\\"");
          break;
        case '\\':
          out.print("\\\\");
          break;
        case '\n':
          out.print("\\n");
          break;
        case '\r':
          out.print("\\r");
          break;
        case '\t':
          out.print("\\t");
          break;
        default:
          if (c >= 32 && c <= 126) {
            out.print(c);
          } else {
            out.print("\\");
            out.print((int) c);
            if (i + 1 < str.length() && Character.isDigit(str.charAt(i + 1))) {
              out.print("\\&");
            }
          }
      }
    }
    out.println("\"");
  }

  /**
   * Type check the body of this definition, but reporting rather than throwing an exception error
   * if the given handler is not null.
   */
  void checkBody(Handler handler) throws Failure {
    /* Nothing to do here */
  }

  /**
   * Calculate a generalized type for this binding, adding universal quantifiers for any unbound
   * type variable in the inferred type. (There are no "fixed" type variables here because all MIL
   * definitions are at the top level.)
   */
  void generalizeType(Handler handler) throws Failure {
    if (declared != null && !declared.alphaEquiv(expected)) {
      throw new Failure(
          pos,
          "Declared type \""
              + declared
              + "\" for \""
              + id
              + "\" does not match expected type \""
              + expected
              + "\"");
    }
    declared = expected;
  }

  /** Collect the set of types in this AST fragment and replace them with canonical versions. */
  void collect(TypeSet set) {
    expected = expected.canonType(set);
    if (declared != null) {
      declared = declared.canonScheme(set);
    }
  }

  /** Update all declared types with canonical versions. */
  void canonDeclared(MILSpec spec) {
    declared = declared.canonScheme(spec);
  }

  /**
   * Generate a specialized version of an entry point. This requires a monomorphic definition (to
   * ensure that the required specialization is uniquely determined, and to allow the specialized
   * version to share the same name as the original).
   */
  Defn specializeEntry(MILSpec spec) throws Failure {
    return this;
  }

  /**
   * Return an Atom corresponding to a specialized version of this memory area under the given
   * substitution. (We do not currently allow memory areas with polymorphic types, so the
   * substitution will have no effect here. But we keep the plumbing, just in case it might be
   * needed in future, for example, in connection with type expressions that are used to specify
   * size and alignment.
   */
  Atom specializeArea(MILSpec spec, Type inst) {
    return new TopArea(inst, this);
  }

  void topLevelRepTransform(Handler handler, RepTypeSet set) {
    declared = expected = Tycon.word.asType();
  }

  /** Rewrite the components of this definition to account for changes in representation. */
  void repTransform(Handler handler, RepTypeSet set) {
    // TODO: likely unnecessary; always Word
    declared = expected = expected.canonType(set);
  }

  void inScopeOf(Handler handler, MILEnv milenv, AtomExp init) throws Failure {
    /* nothing to do */
  }

  /** Calculate a staticValue (which could be null) for each top level definition. */
  void calcStaticValues(LLVMMap lm, llvm.Program prog) {
    llvm.StringInitializer si = new llvm.StringInitializer(str);
    prog.add(new llvm.Constant(llvm.Mods.entry(isEntrypoint), id, si));
    staticValue = calcStaticValue(id);
  }
}
