/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.persist;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.cougaar.core.service.DataProtectionKey;

public class DataProtectionInputStreamStub extends FilterInputStream {
  private DataProtectionKeyStub keyStub;
  // Our own buffer so we don't pollute the callers buffers.
  private byte[] buf = new byte[8192];

  public DataProtectionInputStreamStub(InputStream s, DataProtectionKey keyStub) {
    super(s);
    this.keyStub = (DataProtectionKeyStub) keyStub;
  }

  public int read() throws IOException {
    int b = super.read();
    if (b < 0) return b;
    return (b ^ keyStub.xor) & 0xff;
  }

  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  public int read(byte[] b, int offset, int nb) throws IOException {
    int total = -1;
    byte xor = keyStub.xor;
    while (nb > 0) {
      int tnb = Math.min(nb, buf.length);
      tnb = super.read(buf, 0, tnb);
      if (tnb < 0) break;
      for (int i = 0; i < tnb; i++) {
        b[offset + i] = (byte) (buf[i] ^ xor);
      }
      offset += tnb;
      nb -= tnb;
      if (total < 0) {
        total = tnb;
      } else {
        total += tnb;
      }
    }
    return total;
  }

  public String toString() {
    return "DataProtectionInputStreamStub key " + keyStub;
  }
}
