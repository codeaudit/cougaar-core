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

package org.cougaar.core.wp.resolver.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.cougaar.core.service.wp.AddressEntry;

/**
 * Implementation of RMIAccess.
 * <p>
 * Note that the entry value is final; we could just as well
 * advertise this entry in any other directory service
 * (JavaSpaces, LDAP, FTP, etc).
 */
public class RMIAccessImpl 
extends UnicastRemoteObject
implements RMIAccess {

  private final AddressEntry entry;

  public RMIAccessImpl(
      AddressEntry entry,
      RMIClientSocketFactory csf,
      RMIServerSocketFactory ssf) throws RemoteException {
    super(0, csf, ssf);
    this.entry = entry;
  }

  public AddressEntry getAddressEntry() {
    return entry;
  }

  public String toString() {
    return "RMIAccessImpl for "+entry;
  }
}
