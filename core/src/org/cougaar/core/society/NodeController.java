/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.cougaar.util.PropertyTree;

/**
 * The <code>NodeController</code> is the external API for a loaded Node
 * that allows an outside JVM (console) to access and control the Node.
 * <p>
 * Currently uses RMI, but could be modified to use another protocol
 * (e.g. HTTP server).
 */
public interface NodeController extends Remote {

  /**
   * Get the host name for the controlled Node.
   */
  public String getHostName() throws RemoteException;

  /**
   * Get the <code>NodeIdentifier</code> for the controlled Node.
   *
   * The NodeIdentifier's address should match the bound name of
   * this NodeController in the RMI registry.
   */
  public NodeIdentifier getNodeIdentifier() throws RemoteException;

  /**
   * Return a <code>List</code> of <code>ClusterIdentifiers</code> for 
   * all the clusters currently running on the Node.
   *
   * Currently just returns the ClusterIdentifiers, but in the future a 
   * similar method might be created to return a PropertyTree of "clusters" 
   * and additional cluster information, such as the names of all loaded 
   * PlugIns, etc.
   */
  public List getClusterIdentifiers() throws RemoteException;

  //
  // Other information-getters will be added here, such as:
  //   platform: OS/CPU/memory/network/etc
  //   software: configuration/version/security/JDK/etc
  //   clusters: names/details/etc
  // All of these are subject to security checks, of course...
  //

  /**
   * Instruct the Node to load and start the Clusters specified in the 
   * <code>PropertyTree</code>.
   *
   * The format of the PropertyTree must correspond with the existing
   * <code>AddClustersMessage</code>, as listed below.
   *
   * The PropertyTree should have a "clusters" entry that maps to
   * a List, where each entry in this "clusters" List should be either:<pre>
   *    1) the name of a Cluster, which matches a local ".ini" file
   * </pre>
   * or<pre>
   *    2) a PropertyTree for a parsed Cluster specification, as defined
   *       in <code>ClusterINIParser.parse(BufferedReader)</code>.
   * </pre>.
   *
   * Additional properties of the top-level PropertyTree may be added 
   * at a future time.
   */
  public void addClusters(PropertyTree pt) throws RemoteException;

  //
  // Other control capabilities will be added here, such as:
  //   clusters: remove/halt/transfer/clone/etc
  //   binders: security/logging/profiling/restrictions/etc
  // All of these are subject to security checks, of course...
  //

}
