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

import java.util.Set;

import org.cougaar.core.component.Service;

/**
 * Read access to the society topology.
 */
public interface TopologyReaderService extends Service {

  // type constants, in order of containment:
  int AGENT = 0;
  int NODE = 1;
  int HOST = 2;
  int SITE = 3;
  int ENCLAVE = 4;

  /**
   * Get the parent name for the specified child type
   * and child name.
   * <p>
   * For example, To lookup the node for agent "A":
   * <pre>
   *   getParentForChild(NODE, AGENT, "A");
   * </pre>
   *
   * @param parentType one of the above type constants, such
   *    as NODE
   * @param childType one of the above type constants, such
   *    as AGENT
   * @param childName the name of the child
   *
   * @return the name of the parent, or null if the child
   *    is not known
   */
  String getParentForChild(
      int parentType,
      int childType,
      String childName);

  /**
   * Get a listing of all children that match the specified
   * parent type and parent name.
   * <p>
   * For example, to lookup the all agents on node "N":
   * <pre>
   *   getChildrenForParent(AGENT, NODE, "N");
   * </pre>
   *
   * @param childType one of the above type constants, such
   *    as AGENT
   * @param parentType one of the above type constants, such
   *    as NODE
   * @param parentName the name of the parent
   *
   * @return a set of all matching children, or null if the
   *    parent is not known
   */
  Set getChildrenOnParent(
      int childType,
      int parentType,
      String parentName);

  /**
   * Get a listing of all the names for the specified
   * type.
   * <p>
   * For example, to list all hosts:<pre>
   *   getAll(HOST);
   * </pre>
   *
   * @param type one of the above type constants, such
   *    as AGENT
   *
   * @return a set of all matching names
   */
  Set getAll(int type);

  /**
   * Get the TopologyEntry for the given agent.
   */
  TopologyEntry getEntryForAgent(String agent);

  /**
   * Get a Set of agent TopologyEntry elements for all
   * agents matching the given query.
   * <p>
   * @param agent if non-null, the required agent name, otherwise
   *     matches all agent names.
   * @param node if non-null, the required node name, otherwise
   *     matches all node names
   * @param host if non-null, the required host name, otherwise
   *     matches all host names
   * @param site if non-null, the required site name, otherwise
   *     matches all site names
   * @param enclave if non-null, the required enclave name, otherwise
   *     matches all enclave names
   *
   * @return a Set of TopologyEntry elements
   *
   * @see #getEntryForAgent(String)
   */
  Set getAllEntries(
      String agent,
      String node, 
      String host, 
      String site, 
      String enclave);

  /** 
   * @see #getEntryForAgent(String) preferred, this is for 
   *    restart use only 
   */
  long getIncarnationForAgent(String agent);

  //
  // These "lookup*" variants force a cache refresh.
  // They are more expensive and shouldn't be used unless 
  // absolutely necessary.  In the future they will likely
  // be replaced with QoS options for:
  //   {origin, confidence, staleness}
  //

  String lookupParentForChild(
      int parentType,
      int childType,
      String childName);
  Set lookupChildrenOnParent(
      int childType,
      int parentType,
      String parentName);
  Set lookupAll(int type);
  TopologyEntry lookupEntryForAgent(String agent);
  Set lookupAllEntries(
      String agent,
      String node, 
      String host, 
      String site, 
      String enclave);
  long lookupIncarnationForAgent(String agent);

}
