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

  int FROM = 27;

  int TO = 28;

  int REQUIRE = 29;

  int EXPORT = 30;

  int ENTRYPOINT = 31;

  int EXTERNAL = 32;

  int DATA = 33;

  int TYPE = 34;

  int AREA = 35;

  int STRUCT = 36;

  int BITDATA = 37;

  int CASE = 38;

  int OF = 39;

  int IF = 40;

  int THEN = 41;

  int ELSE = 42;

  int LET = 43;

  int IN = 44;
}
