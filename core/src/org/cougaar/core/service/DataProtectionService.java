/*
 * <copyright>
 *  Copyright 1997-2003 Networks Associates Technology, Inc
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
 *
 * CHANGE RECORD
 * -
 */

package org.cougaar.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.cougaar.core.component.Service;

public interface DataProtectionService
  extends Service
{

  /** 
   * Protects a data stream by signing and/or encrypting the stream.
   *  The service client should create an output stream to which the
   *  encrypted and/or signed data should be persisted.
   *
   *  This service will return an OutputStream that the client should
   *  use to write the unprotected data. The encrypted key that must
   *  be used to decrypt the stream will be placed in the key
   *  envelope. The client is responsible for retaining the encrypted
   *  key and providing it when the stream is subsequently decrypted.
   *  The encrypted key is usually a symmetric key encrypted with the
   *  public key of the agent.
   *
   *  This service must be able to re-encrypt symmetric keys at any time.
   *  For instance, keys may be re-encrypted if the certificate containing
   *  the public key is about to expire, or if the certificate is revoked.
   *
   *  In order to get access to keys at any time, the client must
   *  implement the DataProtectionServiceClient interface,
   *  which provides an iterator over all the key envelopes into which
   *  keys have been placed. The client is responsible for storing the
   *  envelope, so that it is available in the Iterator.
   *
   *  @param pke provides a place to store the key used to encrypt the stream
   *  @param os  the output stream containing the encrypted and/or signed data
   *  @return    An output stream that the client uses to protect data.
   */
  OutputStream getOutputStream(DataProtectionKeyEnvelope pke,
				      OutputStream os)
      throws IOException;

  /** 
   * Unprotects a data stream by verifying and/or decrypting the stream.
   *
   *  The client should provide a key envelope having the same key
   *  that was used to encrypt the data.
   *  @param pke provides a place to retrieve the key for decrypting the stream
   *  @param is  the input stream containing the encrypted and/or signed data
   *  @return    An input stream containing the un-encrypted and/or verified data.
   */
  InputStream getInputStream(DataProtectionKeyEnvelope pke,
				    InputStream is)
      throws IOException;
}

