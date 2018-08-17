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

abstract class CFG extends Node {

  private Labels labels = null;

  private Labels labelsLast = null;

  /** Add an element to the end of the list in this class. */
  public void add(Label elem) {
    Labels ns = new Labels(elem, null);
    labelsLast = (labelsLast == null) ? (labels = ns) : (labelsLast.next = ns);
  }

  /**
   * Captures the blocks whose code will be included in this CFG; code for other blocks can be
   * referenced via a function call.
   */
  protected Blocks includedBlocks;

  /**
   * Dump a summary of this CFG that lists its start label and the labels of all the nodes that it
   * includes.
   */
  void display() {
    if (labels != null) {
      StringBuilder buf = new StringBuilder(this.label());
      buf.append(": {");
      for (Labels ls = labels; ls != null; ls = ls.next) {
        buf.append(" ");
        buf.append(ls.head.label());
      }
      buf.append(" }");
      System.out.println(buf.toString());
    }
  }

  /** Generate dot code for this CFG on the specified PrintWriter. */
  public void cfgToDot(PrintWriter out) {
    this.toDot(out);
    for (Labels ls = labels; ls != null; ls = ls.next) {
      ls.head.toDot(out);
    }
  }

  /**
   * Register a control flow edge between the specified source and destination with the given (non
   * unit) arguments.
   */
  Label edge(Node src, Block b, Atom[] args) {
    // If the target for this edge is a block that is not included in this CFG, then we need
    // to make a regular (i.e., non-tail) call to it:
    if (!Blocks.isIn(b, includedBlocks)) {
      Label lab = new CallLabel(b);
      add(lab);
      lab.calledFrom(src, args);
      return lab;
    }

    // Have we already created a CodeLabel for the specified block in this CFG?
    for (Labels ls = labels; ls != null; ls = ls.next) {
      if (ls.head.hasCodeFor(b)) {
        Label lab = ls.head;
        // Was src already listed as a predecessor of lab?
        if (lab.needsGoto(src)) {
          Label ilab = new GotoLabel(lab); // intermediate label
          add(ilab);
          lab.calledFrom(ilab, args);
          ilab.calledFrom(src, args);
          return ilab;
        }
        lab.calledFrom(src, args); // register connection
        return lab;
      }
    }

    // This is the first time we have requested a label for the specified block:
    Label dst = new CodeLabel(b);
    add(dst);
    dst.calledFrom(src, args);
    return dst;
  }

  /**
   * Calculate the list of successors for all of the labeled Nodes in this CFG. We treat the labels
   * list as a queue, appending new elements as they are discovered.
   */
  void findSuccs() {
    for (Labels ls = labels; ls != null; ls = ls.next) {
      ls.head.findSuccs(this);
    }
  }

  /**
   * Calculate a parameter elimination substitution for this CFG that accounts for all of the
   * renamings that are required when there is a control flow in to a block with a unique
   * predecessor.
   */
  TempSubst paramElim() {
    TempSubst s = null;
    for (Labels ls = labels; ls != null; ls = ls.next) {
      s = ls.head.paramElim(s);
    }
    return s;
  }

  /** Generate an LLVM function definition for this CFG node. */
  llvm.FuncDefn toLLVMFuncDefn(LLVMMap lm, DefnVarMap dvm, TempSubst s) {
    llvm.Local[] formals = formals(lm, dvm);
    int n = Labels.length(labels);
    String[] ss = new String[1 + n];
    llvm.Code[] cs = new llvm.Code[1 + n];
    ss[0] = "entry";
    int i = 1;
    for (Labels ls = labels; ls != null; ls = ls.next) {
      ss[i] = ls.head.label();
      cs[i++] = ls.head.toLLVMLabel(lm, dvm, s);
    }
    return toLLVMFuncDefn(lm, dvm, s, formals, ss, cs);
  }

  /** Calculate an array of formal parameters for the associated LLVM function definition. */
  abstract llvm.Local[] formals(LLVMMap lm, DefnVarMap dvm);

  /**
   * Helper function for constructing a function definition with the fiven formal parameters and
   * code by connecting the the associated MIL Defn for additional details.
   */
  abstract llvm.FuncDefn toLLVMFuncDefn(
      LLVMMap lm, DefnVarMap dvm, TempSubst s, llvm.Local[] formals, String[] ss, llvm.Code[] cs);
}
