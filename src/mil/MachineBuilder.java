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
import compiler.Handler;
import core.*;
import java.util.HashMap;

public class MachineBuilder {

  private Machine machine = new Machine();

  public Machine getMachine() {
    return machine;
  }

  private HashMap<Defn, Fixup> fixups = new HashMap();

  abstract static class Fixup {

    abstract void extendAddrMap(Handler handler, HashAddrMap addrMap, Defn d);

    /** Process fixup updates once the address for a given definition is known. */
    abstract void setAddr(Defn d, int addr, MachineBuilder builder);

    /**
     * Use this fixup to patch (or defer patching) the specified address with a reference to the
     * given definition.
     */
    abstract void patchAddr(int iaddr, Defn d, int off, MachineBuilder builder);
  }

  static class FixupList extends Fixup {

    private int iaddr;

    private int off;

    private FixupList rest;

    /** Default constructor. */
    FixupList(int iaddr, int off, FixupList rest) {
      this.iaddr = iaddr;
      this.off = off;
      this.rest = rest;
    }

    void extendAddrMap(Handler handler, HashAddrMap addrMap, Defn d) {
      handler.report(new Failure("Address of " + d + " is unresolved"));
    }

    /** Process fixup updates once the address for a given definition is known. */
    void setAddr(Defn d, int addr, MachineBuilder builder) {
      FixupList fs = this;
      do {
        builder.patch(fs.iaddr, addr + fs.off);
      } while ((fs = fs.rest) != null);
      builder.resolve(d, addr);
    }

    /**
     * Use this fixup to patch (or defer patching) the specified address with a reference to the
     * given definition.
     */
    void patchAddr(int iaddr, Defn d, int off, MachineBuilder builder) {
      builder.defer(iaddr, d, off, this);
    }
  }

  static class FixupResolved extends Fixup {

    private int addr;

    /** Default constructor. */
    FixupResolved(int addr) {
      this.addr = addr;
    }

    void extendAddrMap(Handler handler, HashAddrMap addrMap, Defn d) {
      d.extendAddrMap(addrMap, addr);
    }

    /** Process fixup updates once the address for a given definition is known. */
    void setAddr(Defn d, int addr, MachineBuilder builder) {
      debug.Internal.error("Repeated attempts to resolve address of \"" + d + "\"");
    }

    /**
     * Use this fixup to patch (or defer patching) the specified address with a reference to the
     * given definition.
     */
    void patchAddr(int iaddr, Defn d, int off, MachineBuilder builder) {
      builder.patch(iaddr, addr + off);
    }
  }

  int getNextAddr() {
    return machine.getNextAddr();
  }

  void patch(int iaddr, int addr) {
    machine.patch(iaddr, addr);
  }

  void patchToHere(int iaddr) {
    machine.patch(iaddr, getNextAddr());
  }

  public HashAddrMap makeAddrMap(Handler handler) {
    HashAddrMap addrMap = new HashAddrMap();
    for (Defn d : fixups.keySet()) {
      fixups.get(d).extendAddrMap(handler, addrMap, d);
    }
    return addrMap;
  }

  /**
   * Call to specify that the given definition has the associated address in the machine that is
   * being constructed. If there are any unresolved fixups for the definition, then they are patched
   * with the new address. The fixups table is then updated with the final address in case there are
   * any future references. An internal error is triggered if an address has been specified
   * previously for the same definition. (i.e., This should not be allowed to happen!)
   */
  void setAddr(Defn d, int addr) {
    Fixup f = fixups.get(d);
    if (f == null) { // no previous references to this definition
      resolve(d, addr);
    } else {
      f.setAddr(d, addr, this);
    }
  }

  /** Add a fixup entry that associates the given definition with a specific address. */
  void resolve(Defn d, int addr) {
    fixups.put(d, new FixupResolved(addr));
  }

  /**
   * Insert the address corresponding to a given definition at the specified address. If the final
   * address is not known, then we create a Fixup entry so that it can be patched later instead.
   */
  void patchAddr(int iaddr, Defn d, int off) {
    Fixup f = fixups.get(d);
    if (f == null) {
      defer(iaddr, d, off, null);
    } else {
      f.patchAddr(iaddr, d, off, this);
    }
  }

  /**
   * Add a fixup entry to indicate that a reference to a given definition at the specified
   * instruction address should be patched when the address is known.
   */
  void defer(int iaddr, Defn d, int off, FixupList others) {
    fixups.put(d, new FixupList(iaddr, off, others));
  }

  void stop() {
    machine.stop();
  }

  void jump(Block b) {
    patchAddr(machine.jump(0), b, 0);
  }

  int jfalse(int addr) {
    return machine.jfalse(addr);
  }

  int jntag(int tag, int addr) {
    return machine.jntag(tag, addr);
  }

  void call(int o, Block b) {
    patchAddr(machine.call(o, 0), b, 0);
  }

  void retn() {
    machine.retn();
  }

  void cjump() {
    machine.cjump();
  }

  void ccall(int o) {
    machine.ccall(o);
  }

  void prim(int o, int p) {
    machine.prim(o, p);
  }

  void load(int src) {
    machine.load(src);
  }

  void gload(TopLevel t, int i) {
    patchAddr(machine.gload(0), t, i);
  }

  void gload(External e) {
    patchAddr(machine.gload(0), e, 0);
  }

  void gload(Area a) {
    patchAddr(machine.gload(0), a, 0);
  }

  void gload(Value v) {
    machine.gload(machine.addGlobal(v));
  }

  void store(int dst) {
    machine.store(dst);
  }

  void copy(int src, int dst) {
    machine.copy(src, dst);
  }

  void gcopy(TopLevel t, int i, int dst) {
    patchAddr(machine.gcopy(0, dst), t, i);
  }

  void gcopy(External e, int dst) {
    patchAddr(machine.gcopy(0, dst), e, 0);
  }

  void gcopy(Area a, int dst) {
    patchAddr(machine.gcopy(0, dst), a, 0);
  }

  void gcopy(Value v, int dst) {
    machine.gcopy(machine.addGlobal(v), dst);
  }

  void alloc(int tag, int n, int o) {
    machine.alloc(tag, n, o);
  }

  void alloc(ClosureDefn k, int n, int o) {
    patchAddr(machine.alloc(0, n, o), k, 0);
  }

  void sel(int i, int dst) {
    machine.sel(i, dst);
  }

  /** Mapping from temporaries to locations in the current frame. */
  private HashMap<Temp, Integer> tempMap = new HashMap();

  /** Reset this builder for a new frame. */
  public void resetFrame() {
    tempMap.clear();
  }

  /**
   * Find the frame location corresponding to a given temporary; this should not fail if the source
   * MIL code is valid and all of the temporaries have been properly introduced to the builder (via
   * extend, described below).
   */
  public int lookup(Temp t) {
    Integer n = tempMap.get(t);
    if (n == null) {
      debug.Internal.error("No frame location for temporary " + t);
    }
    return n.intValue();
  }

  /** Add bindings within the current frame for the given list of temporaries. */
  public void extend(Temp[] ts, int o) {
    for (int i = 0; i < ts.length; i++) {
      tempMap.put(ts[i], o + i);
    }
  }

  int saveGlobal(int i) {
    int addr = machine.addGlobal(null);
    machine.gsave(i, addr);
    return addr;
  }
}
