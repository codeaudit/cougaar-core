/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.util;

import java.util.BitSet;

/**
 * A two dimensional set of bits. Like java.io.util.Bitset but the
 * bits are addressed in two dimensions. The set is grown as needed to
 * accomodate indices in either dimension. Indices must be positive.
 **/
public class BitSet2D {
  private BitSet theSet = new BitSet();
  int height = 100;

  public BitSet2D(int height) {
    this.height = height;
  }

  public BitSet2D() {
    this.height = 8;
  }

  private int index(int x, int y) {
    if (y >= height) {
      BitSet newSet = new BitSet();
      int newheight = y + 1;
      for (int i = 0, n = theSet.size(); i < n; i++) {
        int xx = i / height;
        int yy = i % height;
        int ii = yy + newheight * xx;
        if (theSet.get(i)) {
          newSet.set(ii);
        } else {
          newSet.clear(ii);
        }
      }
      theSet = newSet;
      height = newheight;
    }
    return y + height * x;
  }

  public boolean get(int x, int y) {
    return theSet.get(index(x, y));
  }

  public void set(int x, int y) {
    theSet.set(index(x, y));
  }

  public void clear(int x, int y) {
    theSet.clear(index(x, y));
  }
}
