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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionService;

public class DataProtectionServiceStub implements DataProtectionService {
  public OutputStream getOutputStream(DataProtectionKeyEnvelope pke, OutputStream os)
    throws IOException
  {
    try {
      DataProtectionKey key = new DataProtectionKeyStub();
      pke.setDataProtectionKey(key);
      return new DataProtectionOutputStreamStub(os, key);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      IOException ioe = new IOException("Exception creating DataProtectionOutputStreamStub");
      ioe.initCause(e);
      throw ioe;
    }
  }

  public InputStream getInputStream(DataProtectionKeyEnvelope pke, InputStream is)
    throws IOException
  {
    try {
      DataProtectionKey key = pke.getDataProtectionKey();
      return new DataProtectionInputStreamStub(is, key);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      IOException ioe = new IOException("Exception creating DataProtectionInputStreamStub");
      ioe.initCause(e);
      throw ioe;
    }
  }
}

