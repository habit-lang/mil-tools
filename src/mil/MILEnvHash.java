/*
    Copyright 2018-25 Mark P Jones, Portland State University

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

abstract class MILEnvHash extends MILEnv {

  /** Default constructor. */
  MILEnvHash(TyconEnv tyconEnv) {
    super(tyconEnv);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  Cfun findCfunInThis(String id) {
    return tableCfun.get(id);
  }

  /** Construct a hash table to store mappings for this environment component. */
  protected HashMap<String, Cfun> tableCfun = new HashMap();

  public void printThis() {
    System.out.println(Cfun.class.getSimpleName() + ": " + tableCfun);

    System.out.println(Prim.class.getSimpleName() + ": " + tablePrim);

    System.out.println(Block.class.getSimpleName() + ": " + tableBlock);

    System.out.println(ClosureDefn.class.getSimpleName() + ": " + tableClosureDefn);

    System.out.println(Top.class.getSimpleName() + ": " + tableTop);
  }

  public Cfun addCfun(String id, Cfun x) {
    return tableCfun.put(id, x);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  Prim findPrimInThis(String id) {
    return tablePrim.get(id);
  }

  /** Construct a hash table to store mappings for this environment component. */
  protected HashMap<String, Prim> tablePrim = new HashMap();

  public Prim addPrim(String id, Prim x) {
    return tablePrim.put(id, x);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  Block findBlockInThis(String id) {
    return tableBlock.get(id);
  }

  /** Construct a hash table to store mappings for this environment component. */
  protected HashMap<String, Block> tableBlock = new HashMap();

  public Block addBlock(String id, Block x) {
    return tableBlock.put(id, x);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  ClosureDefn findClosureDefnInThis(String id) {
    return tableClosureDefn.get(id);
  }

  /** Construct a hash table to store mappings for this environment component. */
  protected HashMap<String, ClosureDefn> tableClosureDefn = new HashMap();

  public ClosureDefn addClosureDefn(String id, ClosureDefn x) {
    return tableClosureDefn.put(id, x);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  Top findTopInThis(String id) {
    return tableTop.get(id);
  }

  /** Construct a hash table to store mappings for this environment component. */
  protected HashMap<String, Top> tableTop = new HashMap();

  public Top addTop(String id, Top x) {
    return tableTop.put(id, x);
  }
}
