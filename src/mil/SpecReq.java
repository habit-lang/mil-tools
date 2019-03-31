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
package mil;

import compiler.*;
import core.*;

/**
 * Represents a specialization request, each of which pairs a definition in the original program
 * (identified by a trailing orig on the variable name) with a definition of the same type in the
 * specialized program (identified by a trailing spec on the variable name). When these requests are
 * created, the specialized definition will be initialized with the required monomorphic type, but
 * the body of the definition will not have been specified.
 */
abstract class SpecReq {

  /**
   * Complete the construction of a specialized version of a definition in the original program as
   * specified by a specialization request.
   */
  abstract void specialize(MILSpec spec);
}
