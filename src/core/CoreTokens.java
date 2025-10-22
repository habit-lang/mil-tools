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

public interface CoreTokens {

  int ENDINPUT = 7;

  int POPEN = 8;

  int PCLOSE = 9;

  int SOPEN = 10;

  int SCLOSE = 11;

  int BOPEN = 12;

  int BCLOSE = 13;

  int COMMA = 14;

  int SEMI = 15;

  int BACKTICK = 16;

  int VARID = 17;

  int CONID = 18;

  int VARSYM = 19;

  int CONSYM = 20;

  int NATLIT = 21;

  int BITLIT = 22;

  int STRLIT = 23;

  int EQ = 24;

  int COCO = 25;

  int BAR = 26;

  int DOT = 27;

  int UNDER = 28;

  int FROM = 29;

  int TO = 30;

  int REQUIRE = 31;

  int EXPORT = 32;

  int ENTRYPOINT = 33;

  int EXTERNAL = 34;

  int INFIX = 35;

  int INFIXL = 36;

  int INFIXR = 37;

  int DATA = 38;

  int TYPE = 39;

  int AREA = 40;

  int STRUCT = 41;

  int BITDATA = 42;

  int ALIGNED = 43;

  int PROXY = 44;

  int CASE = 45;

  int OF = 46;

  int IF = 47;

  int THEN = 48;

  int ELSE = 49;

  int LET = 50;

  int IN = 51;
}
