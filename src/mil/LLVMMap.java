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
import java.util.HashMap;

class LLVMMap extends TypeSet {

  private llvm.Program prog;

  /** Default constructor. */
  LLVMMap(llvm.Program prog) {
    this.prog = prog;
  }

  /** Add a type definition to the program associated with this LLVMMap. */
  void typedef(llvm.DefinedType dt) {
    prog.add(new llvm.Typedef(dt));
  }

  void typedef(String comment, llvm.DefinedType dt) {
    prog.add(new llvm.DefnComment(comment, new llvm.Typedef(dt)));
  }

  /** Add a declaration for a primitive function to the program associated with this LLVMMap. */
  void declare(String name, llvm.FunctionType ftype) {
    prog.add(new llvm.FuncDecl(name, ftype));
  }

  private HashMap<Type, llvm.Type> typeMap = (new HashMap<Type, llvm.Type>());

  llvm.Type toLLVM(Type t) {
    Type c = t.canonType(this);
    llvm.Type u = typeMap.get(c);
    if (u == null) {
      u = c.toLLVMCalc(c, this, 0); // Calculate an appropriate llvm type
      typeMap.put(c, u); // Save the mapping from c to t
    }
    return u;
  }

  /**
   * Return the type of the tag values that are used to distinguish between different constructors.
   */
  public static llvm.Type tagType() {
    return llvm.Type.word();
  }

  /** The layout of a data value with unknown constructor. */
  public llvm.DefinedType dataTypeLayout = null;

  public llvm.DefinedType dataLayout() {
    if (dataTypeLayout == null) {
      dataTypeLayout = new llvm.DefinedType(new llvm.StructType(new llvm.Type[] {tagType()}));
      typedef("layout for data types", dataTypeLayout);
    }
    return dataTypeLayout;
  }

  private HashMap<Cfun, llvm.Type> cfunLayoutTypeCache = (new HashMap<Cfun, llvm.Type>());

  llvm.Type cfunLayoutType(Cfun key) {
    llvm.Type t = cfunLayoutTypeCache.get(key);
    if (t == null) {
      llvm.DefinedType dt = new llvm.DefinedType(key.cfunLayoutTypeCalc(this));
      typedef("layout for " + key, dt);
      cfunLayoutTypeCache.put(key, dt);
      return dt;
    }
    return t;
  }

  /** The layout of a closure with an unknown layout type. */
  public llvm.DefinedType closureTypeLayout = null;

  public llvm.DefinedType closureLayout() {
    if (closureTypeLayout == null) {
      closureTypeLayout =
          new llvm.DefinedType(new llvm.StructType(new llvm.Type[] {llvm.Type.ptr}));
      typedef("layout for closure types", closureTypeLayout);
    }
    return closureTypeLayout;
  }

  private HashMap<ClosureDefn, llvm.Type> closureLayoutTypeCache =
      (new HashMap<ClosureDefn, llvm.Type>());

  llvm.Type closureLayoutType(ClosureDefn key) {
    llvm.Type t = closureLayoutTypeCache.get(key);
    if (t == null) {
      llvm.DefinedType dt = new llvm.DefinedType(key.closureLayoutTypeCalc(this));
      typedef("layout for " + key, dt);
      closureLayoutTypeCache.put(key, dt);
      return dt;
    }
    return t;
  }

  private HashMap<Block, llvm.Global> blockGlobalMap = (new HashMap<Block, llvm.Global>());

  /**
   * Look for a global reference for the given definition in this LLVMMap, adding a new entry if
   * required.
   */
  llvm.Global globalFor(Block d) {
    llvm.Global g = blockGlobalMap.get(d);
    if (g == null) {
      blockGlobalMap.put(d, g = d.blockGlobalCalc(this));
    }
    return g;
  }

  private HashMap<ClosureDefn, llvm.Global> closureGlobalMap =
      (new HashMap<ClosureDefn, llvm.Global>());

  /**
   * Look for a global reference for the given definition in this LLVMMap, adding a new entry if
   * required.
   */
  llvm.Global globalFor(ClosureDefn d) {
    llvm.Global g = closureGlobalMap.get(d);
    if (g == null) {
      closureGlobalMap.put(d, g = d.closureGlobalCalc(this));
    }
    return g;
  }

  private HashMap<Prim, llvm.Global> primGlobalMap = (new HashMap<Prim, llvm.Global>());

  /**
   * Look for a global reference for the given definition in this LLVMMap, adding a new entry if
   * required.
   */
  llvm.Global globalFor(Prim d) {
    llvm.Global g = primGlobalMap.get(d);
    if (g == null) {
      primGlobalMap.put(d, g = d.primGlobalCalc(this));
    }
    return g;
  }

  /**
   * A global reference to the allocator function, initialized on first use, at which point we emit
   * an external declaration.
   */
  private llvm.Global allocFuncGlobal = null;

  /**
   * Return a Global reference to the alloc function, generating an appropriate LLVM declaration for
   * the first use.
   */
  llvm.Global allocFuncGlobal() {
    if (allocFuncGlobal == null) {
      String id = "alloc";
      llvm.FunctionType ft =
          new llvm.FunctionType(llvm.Type.ptr, new llvm.Type[] {llvm.Type.word()});
      allocFuncGlobal = new llvm.Global(ft, id);
      declare(id, ft);
    }
    return allocFuncGlobal;
  }
}
