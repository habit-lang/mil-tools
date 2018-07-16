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
import compiler.Position;
import core.*;

/**
 * Represents an environment for MIL programs, each describing a set of block, closure, and
 * top-level definitions that are in scope as well as primitives, constructor functions, and the
 * underlying type environment. Our intention is that MILEnv and TyconEnv values form a "ladder"
 * something like the following:
 *
 * <p>MILEnv TyconEnv source file f2.mil me2 --------> te2 | | v v source file f1.mil me1 -------->
 * te1 | | v v builtins me0 --------> te0
 */
public abstract class MILEnv {

  protected TyconEnv tyconEnv;

  /** Default constructor. */
  public MILEnv(TyconEnv tyconEnv) {
    this.tyconEnv = tyconEnv;
  }

  public TyconEnv getTyconEnv() {
    return tyconEnv;
  }

  public abstract void print();

  public static Failure notFound(Position pos, String id) {
    return new Failure(pos, "No definition for \"" + id + "\"");
  }

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Cfun findCfun(String id) {
    return findCfunInThis(id);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  abstract Cfun findCfunInThis(String id);

  public Cfun mustFindCfun(Handler handler, Position pos, String id) {
    Cfun x = findCfun(id);
    if (x == null) {
      handler.report(notFound(pos, id));
    }
    return x;
  }

  public Cfun addCfun(Cfun x) {
    return addCfun(x.getId(), x);
  }

  public abstract Cfun addCfun(String id, Cfun x);

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Prim findPrim(String id) {
    return findPrimInThis(id);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  abstract Prim findPrimInThis(String id);

  public Prim mustFindPrim(Handler handler, Position pos, String id) {
    Prim x = findPrim(id);
    if (x == null) {
      handler.report(notFound(pos, id));
    }
    return x;
  }

  public Prim addPrim(Prim x) {
    return addPrim(x.getId(), x);
  }

  public abstract Prim addPrim(String id, Prim x);

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Block findBlock(String id) {
    return findBlockInThis(id);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  abstract Block findBlockInThis(String id);

  public Block mustFindBlock(Handler handler, Position pos, String id) {
    Block x = findBlock(id);
    if (x == null) {
      handler.report(notFound(pos, id));
    }
    return x;
  }

  public Block addBlock(Block x) {
    return addBlock(x.getId(), x);
  }

  public abstract Block addBlock(String id, Block x);

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public ClosureDefn findClosureDefn(String id) {
    return findClosureDefnInThis(id);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  abstract ClosureDefn findClosureDefnInThis(String id);

  public ClosureDefn mustFindClosureDefn(Handler handler, Position pos, String id) {
    ClosureDefn x = findClosureDefn(id);
    if (x == null) {
      handler.report(notFound(pos, id));
    }
    return x;
  }

  public ClosureDefn addClosureDefn(ClosureDefn x) {
    return addClosureDefn(x.getId(), x);
  }

  public abstract ClosureDefn addClosureDefn(String id, ClosureDefn x);

  /**
   * Look for an element corresponding to the given identifier at any layer within this environment.
   */
  public Top findTop(String id) {
    return findTopInThis(id);
  }

  /**
   * Look for an element corresponding to the given identifier in this specific environment layer.
   */
  abstract Top findTopInThis(String id);

  public Top mustFindTop(Handler handler, Position pos, String id) {
    Top x = findTop(id);
    if (x == null) {
      handler.report(notFound(pos, id));
    }
    return x;
  }

  public Top addTop(Top x) {
    return addTop(x.getId(), x);
  }

  public abstract Top addTop(String id, Top x);

  /**
   * Add specified constructor to this MILEnv, triggering a failure if there is already a
   * constructor with the same name.
   */
  public void addCfunAndTop(Cfun cf) throws Failure {
    if (findCfun(cf.getId()) != null || addCfun(cf) != null) {
      // TODO: it would be good to give position information for the other definition too
      multipleDefns(cf.getPos(), "constructor", cf.getId());
    }
    cf.addTopLevel();
  }

  /**
   * Throw a failure for multiple definitions of a given identifier as the same kind of item within
   * a MILEnv.
   */
  public static void multipleDefns(Position pos, String what, String id) throws Failure {
    throw new Failure(pos, "Multiple definitions for " + what + " \"" + id + "\"");
  }
}
