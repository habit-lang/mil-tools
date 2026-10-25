/*
    Copyright 2018-26 Mark P Jones, Portland State University

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
import java.util.HashMap;

public class MILSpec extends TypeSet {

  /** A mapping from (canonical) versions of DataType values to TypeSpecs mappings. */
  private HashMap<DataType, TypeSpecs> dataTypeSpecs = (new HashMap<DataType, TypeSpecs>());

  /** Create a mapping from datatype dt to the given list of TypeSpecs. */
  void putTypeSpecs(DataType dt, TypeSpecs ts) {
    dataTypeSpecs.put(dt, ts);
  }

  /** Return the current list of TypeSpecs for the datatype dt. */
  TypeSpecs getTypeSpecs(DataType dt) {
    return dataTypeSpecs.get(dt);
  }

  /**
   * Display a summary of the mapping from datatypes to specialized instances (likely for debugging
   * or other analysis).
   */
  public void dump(PrintWriter out) {
    out.println("Specialized Primitives: -----------------");
    for (Prim p : primSpecMap.keySet()) {
      out.println("Specialized instances of: " + p.getId() + " :: " + p.getBlockType());
      for (Prims ps = primSpecMap.get(p); ps != null; ps = ps.next) {
        out.println("   " + ps.head.getId() + " :: " + ps.head.getBlockType());
      }
      out.println();
    }

    out.println("Specializations: ------------------------");
    for (Defn d : specialized.keySet()) {
      out.print("Specialized instances of: ");
      d.printlnSig(out);
      for (Defns ds = specialized.get(d); ds != null; ds = ds.next) {
        out.print("   ");
        ds.head.printlnSig(out);
      }
      out.println();
    }

    out.println("Specialized Datatypes: ------------------");
    for (DataType dt : dataTypeSpecs.keySet()) {
      for (TypeSpecs ts = getTypeSpecs(dt); ts != null; ts = ts.next) {
        out.println("-- " + ts.inst + "  ~~>  " + ts.dt.asType());
        ts.dt.dumpTypeDefinition(out);
      }
    }
    super.dump(out);
  }

  /**
   * Override the TypeSet method for calculating canonical versions of a type with a Tycon at its
   * head, replacing concrete instances of parameterized algebraic datatypes with new,
   * unparameterized types.
   */
  protected Type canon(Tycon h, int args) {
    Tycon t = h.specInst(this, args);
    return (t == null) ? super.canon(h, args) : t.asType();
  }

  /**
   * A mapping from definitions in the original program to the list of all specialized versions of
   * that definition in the specialized program.
   */
  private HashMap<Defn, Defns> specialized = (new HashMap<Defn, Defns>());

  /** A list of requested specializations. */
  private SpecReqs requested = null;

  /** Request a new specialization. */
  void request(SpecReq req) {
    requested = new SpecReqs(req, requested);
  }

  /** Stores a specialized version of a MIL program, constructed using this MILSpec object. */
  private MILProgram prog = new MILProgram();

  /** Return the specialized version of the MIL program. */
  public MILProgram getProg() {
    return prog;
  }

  /**
   * Generate specialized versions of all the definitions that have been requested, allowing for the
   * fact that new requests might be added in the process.
   */
  void generate() {
    while (requested != null) {
      // Process the queue of specialization requests
      SpecReq req = requested.head;
      requested = requested.next;
      req.specialize(this);
    }
    // Calculate SCCs for the resulting specialized program
    prog.shake();
    // Update declared types to use the specialized datatypes
    prog.canonDeclared(this);
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  Block specializedBlock(Block d, BlockType inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);
    int num = 0; // count number of existing specializations

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      Block prev = ds.head.isBlockOfType(inst);
      if (prev != null) {
        return prev;
      }
      num++;
    }

    // Create a new item, save the associated instance type, insert
    // in the specialized table, and add a specialization request:
    Block newDefn = new Block(d, num);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    request(new SpecBlock(d, newDefn));
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  ClosureDefn specializedClosureDefn(ClosureDefn d, AllocType inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);
    int num = 0; // count number of existing specializations

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      ClosureDefn prev = ds.head.isClosureDefnOfType(inst);
      if (prev != null) {
        return prev;
      }
      num++;
    }

    // Create a new item, save the associated instance type, insert
    // in the specialized table, and add a specialization request:
    ClosureDefn newDefn = new ClosureDefn(d, num);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    request(new SpecClosureDefn(d, newDefn));
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  TopLevel specializedTopLevel(TopLevel d, Scheme inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);
    int num = 0; // count number of existing specializations

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      TopLevel prev = ds.head.isTopLevelOfType(inst);
      if (prev != null) {
        return prev;
      }
      num++;
    }

    // Create a new item, save the associated instance type, insert
    // in the specialized table, and add a specialization request:
    TopLevel newDefn = new TopLevel(d, num);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    request(new SpecTopLevel(d, newDefn));
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  External specializedExternal(External d, Scheme inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);
    int num = 0; // count number of existing specializations

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      External prev = ds.head.isExternalOfType(inst);
      if (prev != null) {
        return prev;
      }
      num++;
    }

    // Create a new item, save the associated instance type, insert
    // in the specialized table, and add a specialization request:
    External newDefn = new External(d, num);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    request(new SpecExternal(d, newDefn));
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  MemArea specializedMemArea(MemArea d, Scheme inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);
    int num = 0; // count number of existing specializations

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      MemArea prev = ds.head.isMemAreaOfType(inst);
      if (prev != null) {
        return prev;
      }
      num++;
    }

    // Create a new item, save the associated instance type, insert
    // in the specialized table, and add a specialization request:
    MemArea newDefn = new MemArea(d, num);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    request(new SpecMemArea(d, newDefn));
    return newDefn;
  }

  /** Request a specialized version of a given definition as an entry point to the new program. */
  void addEntry(Handler handler, Defn d) {
    try {
      prog.addEntry(d.specializeEntry(this));
    } catch (Failure f) {
      handler.report(f);
    }
  }

  /**
   * Request a specialized version of the given definition as the main definition for this program.
   */
  void addMain(Handler handler, Defn main) {
    if (main != null) {
      try {
        prog.setMain(main.specializeEntry(this));
      } catch (Failure f) {
        handler.report(f);
      }
    }
  }

  /**
   * A mapping from primitives in the original program to lists of specialized versions in the
   * specialized program.
   */
  private HashMap<Prim, Prims> primSpecMap = (new HashMap<Prim, Prims>());

  /** Get the list of specialized versions of a given primitive. */
  Prims getPrims(Prim p) {
    return primSpecMap.get(p);
  }

  /** Update the list of specialized versions of a given primitive. */
  void putPrims(Prim p, Prims ps) {
    primSpecMap.put(p, ps);
  }
}
