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
package lc;

import compiler.*;
import core.*;
import mil.*;

public class TypeAnn extends LCDefn {

  private String[] ids;

  private TypeExp type;

  /** Default constructor. */
  public TypeAnn(Position pos, String[] ids, TypeExp type) {
    super(pos);
    this.ids = ids;
    this.type = type;
  }

  /**
   * Annotate bindings in the given list using information (such as a type signature or fixity) that
   * is provided by this definition.
   */
  public void annotateBindings(Handler handler, TyconEnv tenv, Bindings bs) {
    try {
      // Validate declared type scheme:
      Scheme declared = type.toScheme(tenv);

      // Add declared type to internal bindings:
      for (int i = 0; i < ids.length; i++) {
        Binding binding = Bindings.find(ids[i], bs);
        if (binding == null) {
          handler.report(new SignatureWithoutBindingFailure(pos, ids[i]));
        } else {
          binding.attachDeclaredType(handler, pos, declared);
        }
      }

    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Print an indented description of this abstract syntax node, including a name for the node
   * itself at the specified level of indentation, plus more deeply indented descriptions of any
   * child nodes.
   */
  void indent(IndentOutput out, int n) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < ids.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append(ids[i]);
    }
    //  if (declared!=null) {
    //    buf.append(" :: ");
    //    buf.append(declared.toString());
    //  }
    buf.append(" :: <can't display type exp>"); // TODO: fix this!
    out.indent(n, buf.toString());
  }
}
