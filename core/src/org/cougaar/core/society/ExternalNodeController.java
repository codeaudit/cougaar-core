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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.cougaar.util.PropertyTree;

/**
 * The <code>ExternalNodeController</code> is the external API for a loaded 
 * Node that allows an outside JVM "client" to access and control the Node.
 * <p>
 * Currently uses RMI, but could be modified to use another protocol
 * (e.g. HTTP server).
 */
public interface ExternalNodeController extends Remote {

  /**
   * Get the external action listener.
   * <p>
   * Could be modified to support multiple listeners.
   */
  ExternalNodeActionListener getExternalNodeActionListener()
    throws RemoteException;

  /**
   * Set the external action listener.
   * <p>
   * Could be modified to support multiple listeners.
   */
  void setExternalNodeActionListener(
      ExternalNodeActionListener eListener) throws RemoteException;

  //
  // Could support a listener-push Object to restrict items of interest, 
  // which would keep the RMI/notification costs down.  
  //
  // For example, we could define an interface:
  //   public interface ExternalNodeActionInterests implements Serializable {
  //     ..
  //     boolean interestedInClusterAdd();
  //     ..
  //   }
  // Note that this is non-Remote, or at least the external client's
  // implementation needn't subclass this to an RMI Remote but instead
  // "upload" a serialized implementation (via a method in this controller).
  // The Node can then check this (presumed) local "interests" interface and 
  // not send a listener event if the implementation returns false for 
  // that type of event, which would reduce needless RMI messaging.   The 
  // cost is that the client must upload a new "ExternalNodeActionInterests" 
  // implementation if it modifies it's interests.
  //

  /**
   * Get the host name for the controlled Node.
   */
  String getHostName() throws RemoteException;

  /**
   * Get the <code>NodeIdentifier</code> for the controlled Node.
   *
   * The NodeIdentifier's address should match the bound name of
   * this ExternalNodeController in the RMI registry.
   */
  NodeIdentifier getNodeIdentifier() throws RemoteException;

  /**
   * Return a <code>List</code> of <code>ClusterIdentifiers</code> for 
   * all the clusters currently running on the Node.
   *
   * Currently just returns the ClusterIdentifiers, but in the future a 
   * similar method might be created to return a PropertyTree of "clusters" 
   * and additional cluster information, such as the names of all loaded 
   * PlugIns, etc.
   */
  List getClusterIdentifiers() throws RemoteException;

  //
  // Other information-getters will be added here, such as:
  //   platform: OS/CPU/memory/network/etc
  //   software: configuration/version/security/JDK/etc
  //   clusters: names/details/etc
  // All of these are subject to security checks, of course...
  //

  //
  // Other control capabilities will be added here, such as:
  //   clusters: remove/halt/transfer/clone/etc
  //   binders: security/logging/profiling/restrictions/etc
  // All of these are subject to security checks, of course...
  //

}
