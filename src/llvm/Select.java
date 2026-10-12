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
package llvm;

/** Select one of two values using a Boolean condition. */
public class Select extends Rhs {

  private Value condition;

  private Value ifTrue;

  private Value ifFalse;

  public Select(Value condition, Value ifTrue, Value ifFalse) {
    this.condition = condition;
    this.ifTrue = ifTrue;
    this.ifFalse = ifFalse;
  }

  public void append(StringBuilder buf) {
    buf.append("select ");
    condition.append(buf);
    buf.append(", ");
    ifTrue.append(buf);
    buf.append(", ");
    ifFalse.append(buf);
  }
}
