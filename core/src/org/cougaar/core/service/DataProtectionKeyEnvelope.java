/*
 * <copyright>
 *  Copyright 1997-2002 Networks Associates Technology, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 *
 * </copyright>
 *
 * CHANGE RECORD
 * -
 */

package org.cougaar.core.service;

import java.io.IOException;

/**
 *  Implementations of this interface contain a secret key used to
 *  encrypt/decrypt persisted data. The getOutputStream method of the
 *  PersistenceProtectionService places the (encrypted) key used for
 *  encrypting the output stream in the envelope. The getInputStream
 *  method, retrieves the key from the envelope to decrypt the input
 *  stream. In addition, the PersistenceProtectionServiceClient has an
 *  iterator method that returns an iterator over a collection of
 *  PersistedKeyEnvelopes. When the PersistenceProtectionService
 *  iterates over the keys, it uses the getPersistedKey() method to
 *  retrieve the key. The PersistenceProtectionService can re-encrypt
 *  the key if it wishes, and call setPersistedKey() to notify the
 *  service client that it should persist the new key.
 **/
public interface DataProtectionKeyEnvelope
{
  /** 
   * Returns the persisted key in this envelope.
   */
  DataProtectionKey getDataProtectionKey() throws IOException;

  /** 
   * Saves an updated key to persisted storage.
   */
  void setDataProtectionKey(DataProtectionKey pk) throws IOException;
}
