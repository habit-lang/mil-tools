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

  int ENDINPUT = 8;

  int POPEN = 9;

  int PCLOSE = 10;

  int SOPEN = 11;

  int SCLOSE = 12;

  int BOPEN = 13;

  int BCLOSE = 14;

  int COMMA = 15;

  int SEMI = 16;

  int BACKTICK = 17;

  int VARID = 18;

  int CONID = 19;

  int VARSYM = 20;

  int CONSYM = 21;

  int NATLIT = 22;

  int BITLIT = 23;

  int STRLIT = 24;

  int EQ = 25;

  int COCO = 26;

  int BAR = 27;

  int DOT = 28;

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

  int CASE = 41;

  int OF = 42;

  int IF = 43;

  int THEN = 44;

  int ELSE = 45;

  int LET = 46;

  int IN = 47;
}
