/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
