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
import compiler.Failure;
import core.*;

abstract class Value {

  static final BoolValue True = new BoolValue(true);

  static final BoolValue False = new BoolValue(false);

  public abstract String toString();

  long getInt() throws Failure {
    throw new Failure("value is not an integer");
  }

  boolean getBool() throws Failure {
    throw new Failure("value is not Boolean");
  }

  int getTag() throws Failure {
    throw new Failure("value is not tagged");
  }

  Value getComponent(int i) throws Failure {
    throw new Failure("value does not have components");
  }
}
