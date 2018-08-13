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
import java.util.HashMap;

public class MILSpec extends TypeSet {

  /**
   * A mapping from (canonical) types for specific, monomorphic instances of parameterized algebraic
   * datatypes in the original program to the corresponding specialized (non-parameterized)
   * datatypes.
   */
  private HashMap<Type, DataName> specDataNames = new HashMap();

  void put(Type inst, DataName dn) {
    specDataNames.put(inst, dn);
  }

  DataName get(Type inst) {
    return specDataNames.get(inst);
  }

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
    for (Type t : specDataNames.keySet()) {
      DataName dn = specDataNames.get(t);
      if (t != dn.asType()) {
        out.println("  " + t + "  ~~>  " + dn.asType());
        Cfun[] cfuns = dn.getCfuns();
        for (int i = 0; i < cfuns.length; i++) {
          out.println("      " + cfuns[i].getId() + " :: " + cfuns[i].getAllocType());
        }
        out.println();
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
    return h.specializeTycon(this, super.canon(h, args));
  }

  /**
   * A mapping from definitions in the original program to the list of all specialized versions of
   * that definition in the specialized program.
   */
  private HashMap<Defn, Defns> specialized = new HashMap();

  /** A list of requested specializations. */
  private SpecReqs requested = null;

  /** Request a new specialization. */
  public void request(SpecReq req) {
    requested = new SpecReqs(req, requested);
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  public Block specializedBlock(Block d, BlockType inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      Block prev = ds.head.isBlockOfType(inst);
      if (prev != null) {
        return prev;
      }
    }

    // Create a new item, insert in specialized table, and add a request to complete the definition
    // later:
    Block newDefn = new Block(d);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    requested = new SpecReqs(new SpecBlock(d, newDefn), requested);
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  public ClosureDefn specializedClosureDefn(ClosureDefn d, AllocType inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      ClosureDefn prev = ds.head.isClosureDefnOfType(inst);
      if (prev != null) {
        return prev;
      }
    }

    // Create a new item, insert in specialized table, and add a request to complete the definition
    // later:
    ClosureDefn newDefn = new ClosureDefn(d);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    requested = new SpecReqs(new SpecClosureDefn(d, newDefn), requested);
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  public TopLevel specializedTopLevel(TopLevel d, Scheme inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      TopLevel prev = ds.head.isTopLevelOfType(inst);
      if (prev != null) {
        return prev;
      }
    }

    // Create a new item, insert in specialized table, and add a request to complete the definition
    // later:
    TopLevel newDefn = new TopLevel(d);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    requested = new SpecReqs(new SpecTopLevel(d, newDefn), requested);
    return newDefn;
  }

  /** Request a version of a definition that is specialized to a given monomorphic type. */
  public External specializedExternal(External d, Scheme inst) {
    debug.Log.println("Requesting specialization of " + d + " :: " + inst);
    // Get the list of previous specializations:
    Defns specs = specialized.get(d);

    // Search previous specializations for a matching type:
    for (Defns ds = specs; ds != null; ds = ds.next) {
      External prev = ds.head.isExternalOfType(inst);
      if (prev != null) {
        return prev;
      }
    }

    // Create a new item, insert in specialized table, and add a request to complete the definition
    // later:
    External newDefn = new External(d);
    newDefn.setDeclared(inst);
    specialized.put(d, new Defns(newDefn, specs));
    requested = new SpecReqs(new SpecExternal(d, newDefn), requested);
    return newDefn;
  }

  private HashMap<Prim, Prims> primSpecMap = new HashMap();

  Prims getPrims(Prim p) {
    return primSpecMap.get(p);
  }

  void putPrims(Prim p, Prims ps) {
    primSpecMap.put(p, ps);
  }

  private MILProgram prog = new MILProgram();

  public MILProgram getProg() {
    return prog;
  }

  /** Request a specialized version of a given definition as an entry point to the new program. */
  void addEntry(Defn d) throws Failure {
    // !  System.out.println("Specializing entry for " + d);
    prog.addEntry(d.specializeEntry(this));
  }

  /**
   * Generate specialized versions of all the definitions that have been requested, allowing for the
   * fact that new requests might be added in the process.
   */
  void generate() {
    while (requested != null) { // Process the queue of specialization requests
      SpecReq req = requested.head;
      requested = requested.next;
      req.specialize(this);
    }
    prog.shake(); // Calculate SCCs for the resulting specialized program
    prog.canonDeclared(this); // Update declared types to use the specialized datatypes
  }

  /**
   * Build a list of all the zero arity (no parameters), nonrecursive, datatypes with one or more
   * constructors that do not already have an associated bitSize, and might therefore be candidates
   * for replacing with bitdata types.
   */
  public DataNames bitdataCandidates() {
    DataNames cands = null;
    for (Type t : specDataNames.keySet()) {
      DataName dn = specDataNames.get(t);
      if (t == dn.asType() && dn.getArity() == 0 && !dn.isRecursive() && dn.bitSize() == null) {
        Cfun[] cfuns = dn.getCfuns();
        if (cfuns != null && cfuns.length > 0) {
          debug.Log.println("DataName " + dn + " is a candidate for bitdata representation");
          cands = new DataNames(dn, cands);
        }
      }
    }
    return cands;
  }
}
