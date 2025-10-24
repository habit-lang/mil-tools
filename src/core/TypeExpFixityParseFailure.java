/*
    Copyright 2018-25 Mark P Jones, Portland State University

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
package core;

import compiler.*;
import mil.*;

class TypeExpFixityParseFailure extends Failure {

  /**
   * An object of this class is generated when we find a type expression using a string of infix
   * operators that cannot be parsed without an ambiguity.
   */
  public TypeExpFixityParseFailure(TypeExp lop, TypeExp rop) {
    // TODO: do something useful with the lop and rop parameters.
    super(lop.position(), "Ambiguous use of infix operators in type expression");
  }
}
