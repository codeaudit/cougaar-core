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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.cougaar.core.service.DataProtectionKey;

public class DataProtectionOutputStreamStub extends FilterOutputStream {
  private DataProtectionKeyStub keyStub;
  // Our own buffer so we don't pollute the caller's buffers.
  private byte[] buf = new byte[8192];
  public DataProtectionOutputStreamStub(OutputStream s, DataProtectionKey keyStub) {
    super(s);
    this.keyStub = (DataProtectionKeyStub) keyStub;
  }

  public void write(int b) throws IOException {
    byte bb = (byte) b;
    bb ^= keyStub.xor;
    out.write(bb);
  }

  public void write(byte[] b, int offset, int nb) throws IOException {
    byte xor = keyStub.xor;
    while (nb > 0) {
      int tnb = Math.min(nb, buf.length);
      for (int i = 0; i < tnb; i++) {
        buf[i] = (byte) (b[offset + i] ^ xor);
      }
      out.write(buf, 0, tnb);
      offset += tnb;
      nb -= tnb;
    }
  }

  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public String toString() {
    return "DataProtectionOutputStreamStub key " + keyStub;
  }
}
