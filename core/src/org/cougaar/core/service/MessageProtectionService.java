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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.ProtectedOutputStream;
import org.cougaar.core.mts.ProtectedInputStream;

import java.io.InputStream;
import java.io.OutputStream;

public interface MessageProtectionService extends Service
{
    public byte[] encryptHeader(byte[] rawData, 
				MessageAddress source,
				MessageAddress destination);

    public byte[] decryptHeader(byte[] rawData, 
				MessageAddress source,
				MessageAddress destination);

    public ProtectedOutputStream getOutputStream(OutputStream os,
						 MessageAddress src,
						 MessageAddress dst,
						 MessageAttributes attrs);

    public ProtectedInputStream getInputStream(InputStream is,
					       MessageAddress src,
					       MessageAddress dst,
					       MessageAttributes attrs);



}
