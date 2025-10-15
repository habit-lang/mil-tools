/*
    Copyright 2018-19 Mark P Jones, Portland State University

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class TypeSet {

  /** A mapping from Tycons to lists of uses (i.e., types with that Tycon in head position). */
  private HashMap<Tycon, Types> tyconInstances = new HashMap();

  /** A mapping from other (singleton) type forms to lists of uses. */
  private HashMap<Type, Types> otherInstances = new HashMap();

  /**
   * A mapping from constant values (BigIntegers and Strings) to corresponding (canonical) TLits.
   */
  private HashMap<Object, TLit> litsToTypes = new HashMap();

  /** Write a description of this TypeSet to a PrintWriter. */
  public void dump(PrintWriter out) {
    out.println("Tycon mapping: --------------------------");
    for (Tycon tycon : tyconMap.keySet()) {
      Tycon ntycon = tyconMap.get(tycon);
      out.println("  " + tycon + " --> " + ntycon);
    }

    out.println("Tycon uses: -----------------------------");
    for (Tycon tycon : tyconInstances.keySet()) {
      out.println("Tycon: " + tycon.getId());
      for (Types ts = tyconInstances.get(tycon); ts != null; ts = ts.next) {
        out.println("   " + ts.head);
      }
    }

    int count = 0;
    for (Type type : otherInstances.keySet()) {
      if (0 == count++) {
        out.println("Other uses: -----------------------------");
      } else {
        out.print(", ");
      }
      out.print(type.toString());
      out.print(" (");
      int num = 0;
      for (Types ts = otherInstances.get(type); ts != null; ts = ts.next) {
        if (0 != num++) {
          out.print(", ");
        }
        out.print(ts.head.toString());
      }
      out.print(")");
    }
    if (count > 0) {
      out.println();
    }

    count = 0;
    for (Object o : litsToTypes.keySet()) {
      if (0 == count++) {
        out.println("Type literals used: ---------------------");
      } else {
        out.print(", ");
      }
      out.print(litsToTypes.get(o).toString());
    }
    if (count > 0) {
      out.println();
    }

    out.println("-----------------------------------------");
  }

  /**
   * A stack of Type values, used to record type constructor arguments while traversing the Type
   * spines.
   */
  private Type[] stack = new Type[10];

  /** The index of the next unused stack slot. */
  private int sp = 0;

  /** Push a type on to the stack, expanding the stack if necessary. */
  protected void push(Type t) {
    if (sp >= stack.length) {
      Type[] nstack = new Type[2 * stack.length];
      for (int i = 0; i < stack.length; i++) {
        nstack[i] = stack[i];
      }
      stack = nstack;
    }
    stack[sp++] = t;
  }

  /** Return the type on the stack corresponding to argument n (the first argument is at n==1). */
  Type stackArg(int n) {
    return stack[sp - n];
  }

  /** Discard the specified number of entries from the top of the stack. Assumes n<=sp. */
  protected void drop(int n) {
    sp -= n;
  }

  /**
   * Pop the specified number of arguments from the stack, and store them in a new array. Assumes
   * n<=sp.
   */
  protected Type[] pop(int n) {
    if (n == 0) {
      return Type.noTypes;
    } else {
      Type[] ts = new Type[n];
      for (int i = 0; i < n; i++) {
        ts[i] = stack[--sp];
      }
      return ts;
    }
  }

  /**
   * Find a canonical type expression for a type that has Tycon h at its head and arguments given by
   * the top n types on the stack.
   */
  protected Type canon(Tycon h, int args) {
    Types ts = tyconInstances.get(h); // Find previous uses of this item
    Type t = findMatch(args, ts); // And search for a match
    if (t == null) {
      t =
          rebuild(
              h.canonTycon(this).asType(), args); // If none found, build a canonical representative
      tyconInstances.put(h, new Types(t, ts)); // Add it to the list
    }
    return t; // Return the (old or new) canonical representative
  }

  /**
   * Build a canonical type (for the first time that this particular type was found) by combining
   * the specified head type with a number of arguments from the stack.
   */
  protected Type rebuild(Type t, int args) {
    for (; args > 0; args--) {
      t = new TAp(t, stack[--sp]);
    }
    return t;
  }

  /**
   * Scan the given list of Types for an entry with the specified number of arguments, each of which
   * matches the corresponding arguments on the top of the stack. If we find a match, then we remove
   * the arguments and return the canonical type.
   */
  private Type findMatch(int args, Types ts) {
    for (; ts != null; ts = ts.next) {
      if (ts.head.matches(this, args)) {
        drop(args); // remove arguments
        return ts.head; // and return canonical version
      }
    }
    return null;
  }

  /**
   * Find a canonical type expression for a type that has a TVar or a TGen at its head and arguments
   * given by the top n types on the stack. (Any singleton type objects could be used as indices.)
   * We do not expect to encounter cases like this in monomorphic programs produced as a result of
   * specialization, by including support for them here allows us to use TypeSet values with
   * programs that make use of polymorphic types, and also allows the use of TypeSets for rewriting
   * types within type schemes.
   */
  Type canonOther(Type head, int args) {
    if (args == 0) { // If there are no arguments, then head is already
      return head; // a canonical representative
    } else {
      Types ts = otherInstances.get(head); // Find previous uses of this item
      Type t = findMatch(args, ts); // And search for a match
      if (t == null) {
        t = rebuild(head, args); // If none found, build a canonical representative
        otherInstances.put(head, new Types(t, ts)); // Add it to the list
      }
      return t; // Return the (old or new) canonical representative
    }
  }

  /**
   * Find a canonical TLit for a given Object value (either a BigInteger or a String). We assume
   * well-kinded inputs, implying, in particular that we will never encounter a TLit with any
   * arguments.
   */
  TLit canonLit(Object val, TLit n, int args) {
    if (args != 0) {
      debug.Internal.error("kind error: TLits should not have arguments");
    }
    TLit m = litsToTypes.get(val); // Look for a previous use of a TLit with this value
    if (m != null) { // And return it if found
      return m;
    }
    litsToTypes.put(val, n); // Or make it canonical if it's the first occurrence
    return n;
  }

  /**
   * A worker function for canonAllocType: Returns a new array containing canonical versions of the
   * types in ts with respect to this TypeSet and using the given tenv to interpret TGen types.
   */
  Type[] canonTypes(Type[] ts, Type[] tenv) {
    Type[] us = new Type[ts.length];
    for (int i = 0; i < us.length; i++) {
      us[i] = ts[i].canonType(tenv, this);
    }
    return us;
  }

  /**
   * Holds a mapping from Tycons in the input program to corresponding canonical versions in the
   * output.
   */
  private HashMap<Tycon, Tycon> tyconMap = new HashMap();

  /** Records the set of Tycons that are used in the output program. */
  private HashSet<Tycon> tycons = new HashSet();

  /**
   * Add an entry to the tyconMap, associating a given Tycon with its canonical version in this
   * TypeSet.
   */
  void mapTycon(Tycon tycon, Tycon ntycon) {
    tyconMap.put(tycon, ntycon);
    addTycon(ntycon);
  }

  /**
   * Register the specified Tycon as being used in the output program. This method is used to
   * register tycons that do not necessarily appear in the range of the tyconMap (because they are
   * simple enumerations, for example, whose definitions do not change when we compute canonical
   * versions ... which is useful in turn for ensuring that we do not attempt to duplicate important
   * builtin types like Unit and Bool.
   */
  void addTycon(Tycon tycon) {
    tycons.add(tycon);
  }

  /** Test if the given Tycon is already used in the output program. */
  boolean containsTycon(Tycon tycon) {
    return tycons.contains(tycon);
  }

  /**
   * Return the canonical version of this tycon that is stored in this TypeSet, or null if there is
   * none.
   */
  Tycon mapsTyconTo(Tycon tycon) {
    return tyconMap.get(tycon);
  }

  private TreeSet<Tycon> sortedTycons() {
    TreeSet<Tycon> sorted =
        new TreeSet<Tycon>(
            new Comparator<Tycon>() {
              // TODO: This comparator returns 0 if the two inputs are equal, but otherwise
              // returns a code obtained by comparing the Tycon ids.  In the event that the
              // ids are equal, it arbitrarily returns 1.  As such, this is not really a
              // valid comparator, but it should be enough to ensure we get a sorted output
              // without dropping any elements ...
              public int compare(Tycon l, Tycon r) {
                if (l.equals(r)) {
                  return 0;
                }
                int c = l.getId().compareTo(r.getId());
                return (c == 0) ? 1 : c;
              }
            });
    sorted.addAll(tycons);
    return sorted;
  }

  /** Write definitions for all the types defined in this TypeSet to a PrintWriter. */
  public void dumpTypeDefinitions(PrintWriter out) {
    for (Tycon tycon : sortedTycons()) {
      tycon.dumpTypeDefinition(out);
    }
  }

  public void dumpTypeDiagrams(PrintWriter out) {
    out.println("\\documentclass{article}");
    out.println("\\usepackage{fullpage}");
    out.println("\\usepackage{tikz}");
    out.println("\\usepackage{fancyvrb,newverbs}");
    out.println();
    out.println("% General formatting for bitdata diagrams:");
    out.println("\\definecolor{bitdatacolor}{HTML}{E0F0E0}");
    out.println("\\newenvironment{bitdatapicture}%");
    out.println(
        "  {\\begin{center}\\begin{tikzpicture}[scale=0.1,>=stealth,rectangle,minimum size=0.5cm]}%");
    out.println("  {\\end{tikzpicture}\\end{center}}");
    out.println();
    out.println("% Select where type labels appear:");
    out.println("\\def\\typelabelAbove#1#2{#1}");
    out.println("\\def\\typelabelBeside#1#2{#2}");
    out.println("\\def\\typelabelNone#1#2{}");
    out.println("\\let\\typelabel\\typelabelAbove % default position of type labels");
    out.println(
        "\\let\\shorttypelabel\\typelabelBeside % default position of label for short types");
    out.println();
    out.println("% Select where field labels appear:");
    out.println("\\def\\fieldlabelUnder#1#2#3{#1}");
    out.println("\\def\\fieldlabelInside#1#2#3{#2}");
    out.println("\\def\\fieldlabelOver#1#2#3{#3}");
    out.println("\\def\\fieldlabelNone#1#2#3{}");
    out.println("\\let\\fieldlabel\\fieldlabelUnder % default position of field labels");
    out.println(
        "\\let\\shortfieldlabel\\fieldlabelOver % default position of label for short fields");
    out.println();
    out.println("% Formatting of width annotations:");
    out.println("\\def\\widthSlash#1{\\Verb\"/#1\"}");
    out.println("\\def\\widthParen#1{\\Verb\"\\,(#1)\"}");
    out.println(
        "\\def\\typewidth#1{\\ifnum#1>6\\relax{\\widthParen{#1}}\\fi}% #1=typewidth, modify to show fieldwidths");
    out.println(
        "\\def\\fieldwidth#1{\\ifnum#1>4\\relax{\\widthSlash{#1}}\\fi}% #1=fieldwidth, modify to show fieldwidths");
    out.println();
    out.println("\\begin{document}");
    for (Tycon tycon : sortedTycons()) {
      tycon.dumpTypeDiagram(out);
    }
    out.println("\\end{document}");
  }

  /** Cache a mapping of primitives to their canonical versions in this TypeSet. */
  private HashMap<Prim, Prim> primMap = new HashMap();

  Prim getPrim(Prim p) {
    return primMap.get(p);
  }

  void putPrim(Prim p, Prim q) {
    primMap.put(p, q);
  }

  /**
   * Build a list of all the zero arity (no parameters), non-recursive, datatypes with one or more
   * constructors that do not already have an associated bitSize, and might therefore be candidates
   * for replacing with bitdata types.
   */
  DataTypes bitdataCandidates() {
    DataTypes cands = null;
    for (Tycon tycon : tycons) {
      DataType dt = tycon.bitdataCandidate();
      if (dt != null && dt != Tycon.unit) {
        debug.Log.println("DataType " + dt + " is a candidate for bitdata representation");
        cands = new DataTypes(dt, cands);
      }
    }
    return cands;
  }

  MergeMap mergeMap() {
    // First, identify candidates for merging (nullary datatypes with at least one constructor):
    DataTypes[] cands = new DataTypes[4]; // Sort the types by number of constructors
    int numCands = 0;
    for (Tycon tycon : tycons) {
      DataType dt = tycon.mergeCandidate();
      if (dt != null && dt != Tycon.unit) { // TODO: why the special case for Unit?
        int n = dt.getCfuns().length;
        debug.Log.println(
            "DataType " + dt + " with " + n + " constructors is a candidate for merging");
        if (n >= cands.length) {
          DataTypes[] ncands = new DataTypes[Math.max(2 * cands.length, n + 1)];
          for (int i = 0; i < cands.length; i++) {
            ncands[i] = cands[i];
          }
          cands = ncands;
        }
        cands[n] = dt.insertInto(cands[n]);
        numCands++;
      }
    }

    // If no candidates were found, then return a null map:
    if (numCands == 0) {
      return null;
    }

    // Otherwise compare types more closely to search for equivalent definitions:
    MergeMap mmap = new MergeMap();
    for (int i = 0; i < cands.length; i++) {
      for (DataTypes dts = cands[i]; dts != null; dts = dts.next) {
        DataType dt = dts.head;
        for (DataTypes rest = dts.next; rest != null; rest = rest.next) {
          mmap.clearAssumed();
          if (dt.sameDataTypeMod(rest.head, mmap)) {
            mmap.confirmAssumed();
            break;
          }
        }
      }
    }
    mmap.clearAssumed();
    return mmap;
  }
}
