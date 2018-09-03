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
import compiler.BuiltinPosition;
import compiler.Position;
import core.*;

/** Names for type variables/parameters. */
public class Tyvar extends Name {

  private Kind kind;

  /** Default constructor. */
  public Tyvar(Position pos, String id, Kind kind) {
    super(pos, id);
    this.kind = kind;
  }

  public static final Tyvar[] noTyvars = new Tyvar[0];

  public static final Tyvar arg = new Tyvar(BuiltinPosition.pos, "arg", KAtom.STAR);

  public static final Tyvar res = new Tyvar(BuiltinPosition.pos, "", KAtom.STAR);

  public static final Tyvar star = new Tyvar(BuiltinPosition.pos, "", KAtom.STAR);

  public static final Tyvar tuple = new Tyvar(BuiltinPosition.pos, "", KAtom.TUPLE);

  public static final Tyvar area = new Tyvar(BuiltinPosition.pos, "", KAtom.AREA);

  public static final Tyvar nat = new Tyvar(BuiltinPosition.pos, "", KAtom.NAT);

  /** Return the kind of this type constructor. */
  public Kind getKind() {
    return kind;
  }

  public void fixKinds() {
    kind = kind.fixKind();
  }
}
