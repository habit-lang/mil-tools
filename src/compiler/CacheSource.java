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
package compiler;

/** A Source that caches the text of all lines that it reads. */
public class CacheSource extends Source {

  /** The original source for this Source object. */
  private Source source;

  /** A cache of the lines held in this source. */
  private String[] cache;

  /** Records the number of lines in the cache. */
  private int used;

  int addToCache(String val) {
    if (cache == null) {
      cache = new String[10];
    } else if (used >= cache.length) {
      String[] newarray = new String[2 * cache.length];
      for (int i = 0; i < cache.length; i++) {
        newarray[i] = cache[i];
      }
      cache = newarray;
    }
    cache[used] = val;
    return used++;
  }

  /** Construct a new caching source. */
  public CacheSource(Handler handler, Source source) {
    super(handler);
    this.source = source;
    this.cache = null;
    this.used = 0;
  }

  /** Return a printable description of this source. */
  public String describe() {
    return source.describe();
  }

  /** Read the next line from the input stream. */
  public String readLine() {
    String line = source.readLine();
    if (line != null) { // Store text in the cache
      addToCache(line);
    }
    return line;
  }

  /** Return the current line number. */
  public int getLineNo() {
    return source.getLineNo();
  }

  /**
   * Return the text of a specific line, if it is available. If the requested line has not been read
   * yet, then a null is returned.
   */
  public String getLine(int lineNo) {
    return (cache == null || lineNo <= 0 || lineNo > used) ? null : cache[lineNo - 1];
  }

  /** Close the input stream and any associated resources. */
  public void close() {
    source.close();
    this.cache = null;
  }
}
