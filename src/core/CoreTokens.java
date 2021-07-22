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

  int DATA = 35;

  int TYPE = 36;

  int AREA = 37;

  int STRUCT = 38;

  int BITDATA = 39;

  int ALIGNED = 40;

  int PROXY = 41;

  int CASE = 42;

  int OF = 43;

  int IF = 44;

  int THEN = 45;

  int ELSE = 46;

  int LET = 47;

  int IN = 48;
}
