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
import java.io.PrintWriter;

/** Defines an abstract machine with a bytecode (really "int"code) execution engine. */
public class Machine {

  private Value[] globals;

  private int numGlobals = 0;

  int addGlobal(Value val) {
    if (globals == null) {
      globals = new Value[40];
    } else if (numGlobals >= globals.length) {
      Value[] newarray = new Value[2 * globals.length];
      for (int i = 0; i < globals.length; i++) {
        newarray[i] = globals[i];
      }
      globals = newarray;
    }
    globals[numGlobals] = val;
    return numGlobals++;
  }

  private String showGlobal(int i) {
    String d = (i >= 0 && i < numGlobals && globals[i] != null) ? globals[i].toString() : "?";
    return "[" + d + "]";
  }

  private static final int STOP = 0;

  private static final int JUMP = 1;

  private static final int JFALSE = 2;

  private static final int JNTAG = 3;

  private static final int CALL = 4;

  private static final int RETURN = 5;

  private static final int CJUMP = 6;

  private static final int CCALL = 7;

  private static final int PRIM = 8;

  private static final int LOAD = 9;

  private static final int GLOAD = 10;

  private static final int STORE = 11;

  private static final int COPY = 12;

  private static final int GCOPY = 13;

  private static final int GSAVE = 14;

  private static final int ALLOC = 15;

  private static final int SEL = 16;

  private int[] prog;

  private int nextAddr = 0;

  int getNextAddr() {
    return nextAddr;
  }

  int emit(int val) {
    if (prog == null) {
      prog = new int[4000];
    } else if (nextAddr >= prog.length) {
      int[] newarray = new int[2 * prog.length];
      for (int i = 0; i < prog.length; i++) {
        newarray[i] = prog[i];
      }
      prog = newarray;
    }
    prog[nextAddr] = val;
    return nextAddr++;
  }

  void stop() {
    emit(STOP);
  }

  int jump(int addr) {
    emit(JUMP);
    emit(addr);
    return nextAddr - 1;
  }

  int jfalse(int addr) {
    emit(JFALSE);
    emit(addr);
    return nextAddr - 1;
  }

  int jntag(int tag, int addr) {
    emit(JNTAG);
    emit(tag);
    emit(addr);
    return nextAddr - 1;
  }

  int call(int o, int addr) {
    emit(CALL);
    emit(o);
    emit(addr);
    return nextAddr - 1;
  }

  void retn() {
    emit(RETURN);
  }

  void cjump() {
    emit(CJUMP);
  }

  void ccall(int o) {
    emit(CCALL);
    emit(o);
  }

  void prim(int o, int p) {
    emit(PRIM);
    emit(o);
    emit(p);
  }

  void load(int src) {
    emit(LOAD);
    emit(src);
  }

  int gload(int i) {
    emit(GLOAD);
    emit(i);
    return nextAddr - 1;
  }

  void store(int dst) {
    emit(STORE);
    emit(dst);
  }

  void copy(int src, int dst) {
    emit(COPY);
    emit(src);
    emit(dst);
  }

  int gcopy(int i, int dst) {
    emit(GCOPY);
    emit(i);
    emit(dst);
    return nextAddr - 2;
  }

  void gsave(int src, int i) {
    emit(GSAVE);
    emit(src);
    emit(i);
  }

  int alloc(int tag, int n, int o) {
    emit(ALLOC);
    emit(tag);
    emit(n);
    emit(o);
    return nextAddr - 3;
  }

  void sel(int i, int dst) {
    emit(SEL);
    emit(i);
    emit(dst);
  }

  /**
   * Set the value of a particular address in the program memory. This is used to backpatch the
   * generated code with values (such as forward references) that were not known at the point where
   * the instructions were generated.
   */
  void patch(int iaddr, int val) {
    prog[iaddr] = val;
  }

  /**
   * Write a bytecode listing of the program in this machine to the specified PrintWriter using the
   * given AddrMap.
   */
  public void dump(PrintWriter out, AddrMap addrMap) {
    int pc = 0;
    while (pc < nextAddr) {
      out.print(addrMap.codeLabel(pc) + "\t");
      switch (prog[pc++]) {
        case STOP:
          out.println("STOP");
          out.println();
          continue;

        case JUMP:
          out.println("JUMP " + addrMap.codeLabel(prog[pc++]));
          out.println();
          continue;

        case JFALSE:
          out.println("JFALSE " + addrMap.codeLabel(prog[pc++]));
          out.println();
          continue;

        case JNTAG:
          out.println("JNTAG " + prog[pc] + ", " + addrMap.codeLabel(prog[pc + 1]));
          out.println();
          pc += 2;
          continue;

        case CALL:
          out.println("CALL o=" + prog[pc] + ", " + addrMap.codeLabel(prog[pc + 1]));
          pc += 2;
          continue;

        case RETURN:
          out.println("RETURN");
          out.println();
          continue;

        case CJUMP:
          out.println("CJUMP");
          out.println();
          continue;

        case CCALL:
          out.println("CCALL o=" + prog[pc++]);
          out.println();
          continue;

        case PRIM:
          out.println("PRIM o=" + prog[pc] + ", " + Prim.showPrim(prog[pc + 1]));
          // TODO: should introduce primitive table before this ...
          pc += 2;
          continue;

        case LOAD:
          out.println("LOAD  " + prog[pc++]);
          continue;

        case GLOAD:
          out.println("GLOAD  " + addrMap.globalLabel(prog[pc]) + showGlobal(prog[pc]));
          pc++;
          continue;

        case STORE:
          out.println("STORE " + prog[pc++]);
          continue;

        case COPY:
          out.println("COPY " + prog[pc] + ", " + prog[pc + 1]);
          pc += 2;
          continue;

        case GCOPY:
          out.println(
              "GCOPY "
                  + addrMap.globalLabel(prog[pc])
                  + showGlobal(prog[pc])
                  + ", "
                  + prog[pc + 1]);
          pc += 2;
          continue;

        case GSAVE:
          out.println("GSAVE " + prog[pc] + ", " + addrMap.globalLabel(prog[pc + 1]));
          out.println();
          pc += 2;
          continue;

        case ALLOC:
          out.println(
              "ALLOC "
                  + addrMap.codeLabel(prog[pc])
                  + ", n="
                  + prog[pc + 1]
                  + ", o="
                  + prog[pc + 2]);
          pc += 3;
          continue;

        case SEL:
          out.println("SEL " + prog[pc] + ", " + prog[pc + 1]);
          pc += 2;
          continue;
      }
    }
    addrMap.dump(out); // dump symbol table
    out.println("Code size = " + nextAddr + " words");
  }

  private static class Context {

    private int fp;

    private int pc;

    private Context enclosing;

    /** Default constructor. */
    private Context(int fp, int pc, Context enclosing) {
      this.fp = fp;
      this.pc = pc;
      this.enclosing = enclosing;
    }

    Context reuse(int fp, int pc, Context enclosing) {
      this.fp = fp;
      this.pc = pc;
      Context old = this.enclosing;
      this.enclosing = enclosing;
      return old;
    }
  }

  private Context saved;

  private Context cached = null;

  private int callDepth;

  private int maxCallDepth;

  /**
   * Set the control stack for this machine to empty (but cached contexts from previous runs may be
   * kept).
   */
  private void resetControlStack() {
    saved = null;
    callDepth = 0;
    maxCallDepth = 0;
  }

  public int getMaxCallDepth() {
    return maxCallDepth;
  }

  /**
   * Push an entry on to the current control stack to save the specified fp and pc values. This will
   * either use a previously cached Context, or, if required, allocate a new one.
   */
  private void saveContext(int fp, int pc) {
    if (++callDepth > maxCallDepth) {
      maxCallDepth = callDepth;
    }
    if (cached != null) { // Use a cached context if one is available
      cached.fp = fp;
      cached.pc = pc;
      Context ncached = cached.enclosing;
      cached.enclosing = saved;
      saved = cached;
      cached = ncached;
    } else { // Or allocate a new context if necessary
      saved = new Context(fp, pc, saved);
    }
  }

  private Value[] stack = new Value[4000];

  private int instrCount = 0;

  public int getInstrCount() {
    return instrCount;
  }

  public void exec(PrintWriter out, int pc) {
    resetControlStack();
    int fp = 0; // Current frame pointer
    instrCount = 0; // Count number of instructions executed
    Value val = null; // Special "value" register
    try {
      for (; ; ) {
        instrCount++;
        switch (prog[pc]) {
          case STOP: // STOP:  Terminate execution.
            return;

          case JUMP: // JUMP addr:  Continue execution at the specified address.
            pc = prog[pc + 1];
            continue;

          case JFALSE: // JFALSE addr:  Conditional jump if the content of the value register is
                       // false.
            if (val.getBool()) {
              pc += 2; // skip address
            } else {
              pc = prog[pc + 1]; // perform jump
            }
            continue;

          case JNTAG: // JNTAG t addr:  Conditional jump if the content of the value register is not
                      // tagged with t.
            if (val.getTag() == prog[pc + 1]) {
              pc += 3; // skip address
            } else {
              pc = prog[pc + 2]; // perform jump
            }
            continue;

          case CALL: // CALL o, addr:  Jump to the specified address, incrementing fp by o (which is
                     // typically
            // where any parameters for the call are stored).  The value of the program counter
            // (after
            // this instruction) and the current frame offset are stored so that they can be
            // restored
            // by a subsequent RETURN instruction.
            saveContext(fp, pc + 3);
            fp += prog[pc + 1];
            pc = prog[pc + 2];
            continue;

          case CJUMP: // CJUMP:  Jump to closure in the value register
            pc = val.getTag();
            continue;

          case CCALL: // CCALL o:  Call closure in the value register, starting a new frame at the
                      // specified
            // offset in the current frame.
            saveContext(fp, pc + 2);
            fp += prog[pc + 1];
            pc = val.getTag();
            continue;

          case RETURN: // RETURN:  Return to calling procedure, restoring fp and pc from saved
                       // context.  We cache a
            // small number of Context structures so that they can be reused on subsequent calls
            // without
            // additional allocation; this may prove to be an unnecessary/premature "optimization"
            // ...
            {
              Context restore = saved;
              saved = saved.enclosing;
              fp = restore.fp;
              pc = restore.pc;
              if (cached == null) {
                restore.fp = 0;
                restore.enclosing = null;
                cached = restore;
              } else if (cached.fp < 10) {
                restore.fp = cached.fp + 1;
                restore.enclosing = cached;
                cached = restore;
              }
            }
            callDepth--;
            continue;

          case LOAD: // LOAD src:  Load from specified frame slot into the value register.
            val = stack[fp + prog[pc + 1]];
            pc += 2;
            continue;

          case GLOAD: // GLOAD i:  Load global item i into the value register.
            val = globals[prog[pc + 1]];
            pc += 2;
            continue;

          case STORE: // STORE dst:  Store the contents of the value register in the specified frame
                      // slot.
            stack[fp + prog[pc + 1]] = val;
            pc += 2;
            continue;

          case COPY: // COPY src dst:  Copy a value from a source slot into a destination slot.  We
                     // could produce
            // a similar effect by using a sequence: LOAD src; STORE dst.  The COPY instruction,
            // however,
            // provides a more compact encoding, and allows us to hold another temporary in the
            // value
            // register.  For example, we can swap the values in a pair of locations using the code
            // sequence:  LOAD v; COPY w v; STORE w
            stack[fp + prog[pc + 2]] = stack[fp + prog[pc + 1]];
            pc += 3;
            continue;

          case GCOPY: // GCOPY i dst:  Copy global item i into the specified frame slot.
            stack[fp + prog[pc + 2]] = globals[prog[pc + 1]];
            pc += 3;
            continue;

          case GSAVE: // GSAVE src i:  Save value in the source frame location as global item i.
            globals[prog[pc + 2]] = stack[fp + prog[pc + 1]];
            debug.Log.println("Saved global: " + prog[pc + 2] + " [" + globals[prog[pc + 2]] + "]");
            pc += 3;
            continue;

          case ALLOC: // ALLOC tag n o:  Allocate a new data value with the specified tag and n
                      // fields, starting at
            // offset o in the current frame.  The result is saved in the value register.
            {
              int n = prog[pc + 2];
              int base = fp + prog[pc + 3];
              Value[] vals = new Value[n];
              for (int i = 0; i < n; i++) {
                vals[i] = stack[base + i];
              }
              val = new DataValue(prog[pc + 1], vals);
              pc += 4;
            }
            continue;

          case SEL: // SEL i dst:  Extract the ith component of the data object in the value
                    // register and store
            // the result in frame slot dst.
            stack[fp + prog[pc + 2]] = val.getComponent(prog[pc + 1]);
            pc += 3;
            continue;

          case PRIM: // PRIM o p:  Execute primitive number p using parameters at offset o in the
                     // current frame.
            Prim.exec(out, prog[pc + 2], fp + prog[pc + 1], stack);
            pc += 3;
            continue;
        }
      }
    } catch (Failure f) {
      out.println("Execution aborted: pc=" + pc + ", " + f.getText());
      out.println("Value: " + (val == null ? "null" : val.toString()));
      out.println("Frame:");
      for (int i = 0; i < 10 && fp + i < stack.length && stack[fp + i] != null; i++) {
        out.println(" +" + i + ": " + stack[fp + i]);
      }
    }
  }
}
