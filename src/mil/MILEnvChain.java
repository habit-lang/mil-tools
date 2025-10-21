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

public class MILEnvChain extends MILEnvHash {

  private MILEnv prior;

  /** Default constructor. */
  public MILEnvChain(TyconEnv tyconEnv, MILEnv prior) {
    super(tyconEnv);
    this.prior = prior;
  }

  void print() {
    System.out.println("Chain: ----------");
    printThis();
    if (prior != null) {
      prior.print();
    }
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Cfun findCfun(String id) {
    Cfun x = findCfunInThis(id);
    return (x != null || prior == null) ? x : prior.findCfun(id);
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Prim findPrim(String id) {
    Prim x = findPrimInThis(id);
    return (x != null || prior == null) ? x : prior.findPrim(id);
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Block findBlock(String id) {
    Block x = findBlockInThis(id);
    return (x != null || prior == null) ? x : prior.findBlock(id);
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public ClosureDefn findClosureDefn(String id) {
    ClosureDefn x = findClosureDefnInThis(id);
    return (x != null || prior == null) ? x : prior.findClosureDefn(id);
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Top findTop(String id) {
    Top x = findTopInThis(id);
    return (x != null || prior == null) ? x : prior.findTop(id);
  }
}
