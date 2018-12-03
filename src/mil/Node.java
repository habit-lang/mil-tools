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
import core.*;
import java.io.PrintWriter;

abstract class Node {

  protected int num = count++;

  protected static int count = 0;

  /**
   * The list of Labels that are successors to this Node. TODO: we don't really need the full
   * generality that this provides ... for CallLabel, it will always be noLabels; for GotoLabel, it
   * will always be a singleton containing lab; for BlockCFG and InitCFG it will always be a
   * singleton; for ClosureDefnCFG, it will contain at most one entry.
   */
  protected Label[] succs;

  /**
   * Return a string that can be used as a label for the generated code corresponding to this node.
   */
  abstract String label();

  /** Return a string that can be used as the name of this node in debugging output. */
  String nodeName() {
    return label();
  }

  /** Generate dot code for this CFG node on the specified PrintWriter. */
  public void toDot(PrintWriter out) {
    out.println(
        num + "[label=\"" + nodeName() + "\"," + dotAttrs() + "];"); // Output the node itself
    for (int i = 0; i < succs.length; i++) { // Add edges to successors
      out.println(num + " -> " + succs[i].num + ";");
    }
  }

  /** Return a string with the options (e.g., fillcolor) for displaying this CFG node. */
  abstract String dotAttrs();
}
