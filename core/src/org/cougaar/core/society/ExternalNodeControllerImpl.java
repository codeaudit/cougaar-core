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

package org.cougaar.core.society;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.cougaar.util.PropertyTree;

/**
 * Implementation of <code>ExternalNodeController</code>.
 *
 * @see ExternalNodeController
 */
public class ExternalNodeControllerImpl 
extends UnicastRemoteObject 
implements ExternalNodeController {

  private ExternalNodeActionListener eListener;

  private final Node node;

  public ExternalNodeControllerImpl(Node node) throws RemoteException {
    this.node = node;
  }

  public ExternalNodeActionListener getExternalNodeActionListener() {
    return eListener;
  }

  public void setExternalNodeActionListener(
      ExternalNodeActionListener eListener) {
    this.eListener = eListener;
  }

  public String getHostName() {
    try {
      return node.findHostName();
    } catch (java.net.UnknownHostException e) {
      return null;
    }
  }

  public NodeIdentifier getNodeIdentifier() {
    return node.getNodeIdentifier();
  }

  public List getClusterIdentifiers() {
    return node.getClusterIdentifiers();
  }

}
