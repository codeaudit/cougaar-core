/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.io.*;

/** A string wrapper which serializes the enclosed string more
 * efficiently... maybe.
 **/

public class FastString implements Externalizable {
  private transient String string;
  public FastString() {}
  public FastString(String s) { string = s; }

  public String toString() {
    return string;
  }

  private static final int BUFSIZE = Short.MAX_VALUE;
  private static final byte[] srcbuf = new byte[BUFSIZE];

  public final void readExternal(ObjectInput in) throws IOException {
    int l = in.readShort();
    synchronized (srcbuf) {
      in.readFully(srcbuf,0,l);

      string=new String(srcbuf,0,l);
    }

    // intern short strings
    if (l < 18) string=string.intern();
  }

  private static final char[] dstbuf = new char[BUFSIZE];

  public final void writeExternal(ObjectOutput out) throws IOException {
    int l = string.length();
    if (l > BUFSIZE)
      throw new IOException("Attempt to serialize a FastString of length "+l+
                            " (limit is "+BUFSIZE+").\n"+
                            "  First 60 characters are \""+string.substring(0,60)+
                            "...\"");
    out.writeShort(l);
    char[] dst = dstbuf;        // localize the buffer reference
    synchronized (dst) {
      string.getChars(0, l, dst, 0);
      for (int i=0; i<l; i++) {
        out.write(dst[i]);      // upper bits ignored
      }
    }
  }

}
