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

package org.cougaar.core.service;

import java.util.Set;
import org.cougaar.core.component.Service;

/**
 * Read access to the society topology.
 */
public interface TopologyReaderService extends Service {

  /**
   * Types of agent entries.
   * <p>
   * The "AGENT_TYPE" represents leaf agents within the node.  The
   * node-agent uses the "NODE_AGENT_TYPE".  The "SYSTEM_TYPE" is
   * reserved for infrastructure use.
   * <p>
   * These constants can be "OR'ed" together and used in the query 
   * methods listed below.  For example, to list all node-agent
   * entries:
   * <pre>
   *   getAll(NODE_AGENT_TYPE);
   * </pre>
   * and to list all leaf-agent or system entries:
   * <pre>
   *   getAll((AGENT_TYPE | SYSTEM_TYPE));
   * </pre>
   */
  int AGENT_TYPE      = (1 << 0);
  int NODE_AGENT_TYPE = (1 << 1);
  int SYSTEM_TYPE     = (1 << 2);
  // note that below "3" references match above "2"+1.

  /**
   * An agent type mask that matches any agent type.
   */
  int ANY_TYPE = ((1 << 3) - 1);

  /**
   * The default "AGENT" type matches all node-agents and
   * leaf-agents:  (AGENT_TYPE | NODE_AGENT_TYPE).
   */
  int AGENT   = (AGENT_TYPE | NODE_AGENT_TYPE);

  /**
   * NODE, HOST, SITE, and ENCLAVE types.
   * <p>
   * A NODE contains one or more agents, minimally a node-agent.
   * A HOST contains one or more nodes.
   * A SITE contains one or more hosts.
   * An ENCLAVE contains one or more sites.
   */
  int NODE    = (1 << (3+0));
  int HOST    = (1 << (3+1));
  int SITE    = (1 << (3+2));
  int ENCLAVE = (1 << (3+3));

  /**
   * Set the timeout for future topology read requests.
   * <p>
   * The valid timeout values are:<ul>
   *   <li>
   *   Negative (<i>the default</i>) indicates that the client 
   *   wishes to wait as long as it takes for future topology 
   *   requests to complete.
   *   </li><p>
   *   <li>
   *   Zero indicates that future requests should quickly check the 
   *   cache, but not force a fetch if the response is not already 
   *   cached.
   *   </li><p>
   *   <li>
   *   Positive indicates the time in milliseconds to wait for the 
   *   future topology requests.
   *   </li><p>
   * </ul>
   * <p>
   * If a timeout occurs (millis &gt;= 0) then the "useStale" option 
   * below controls the behavior, which is to either return a stale 
   * response or throw a runtime exception of type
   *   <code>TopologyReaderService.TimeoutException</code>.
   *
   * @see #setUseStale(boolean)
   */
  void setTimeout(long millis);

  /**
   * @see #setTimeout(long)
   */
  long getTimeout();

  /**
   * Set the future behavior for topology requests that takes longer
   * than the timeout.
   * <p>
   * If the timeout is negative (<i>the default</i>) then the 
   * "useStale" option is disabled.
   * <p>
   * If "useStale" is true (<i>the default</i>) then a topology 
   * request that takes longer than the timeout limit will return 
   * the most recent stale value.  If there is no stale value then 
   * the behavior is as if "useStale" is false, as defined below.
   * <p>
   * If "useStale" is false then a topology request that takes 
   * longer than the timeout limit will throw a runtime exception
   * of type 
   *   <code>TopologyReaderService.TimeoutException</code>.
   * The exception captures the stale value if the client wishes 
   * to use it.
   *
   * @see #setTimeout(long)
   */
  void setUseStale(boolean useStale);

  /**
   * @see #setUseStale(boolean)
   */
  boolean getUseStale();

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

  /**
   * A TimeoutException is thrown if a topology request exceeds 
   * the timeout limit and either:<pre>
   *   a) "useStale" is false, or
   *   b) there is no stale value
   * </pre>.
   * <p>
   * The client can catch this exception and repeat their
   * request using the stale value (iff "hasStale()" is true).
   * The request must be <u>exactly</u> the same as the
   * timed-out request, otherwise an exception will be
   * thrown.
   */
  abstract class TimeoutException 
    extends RuntimeException {
      public abstract boolean hasStale();
      public abstract TopologyReaderService withStale();
    };

}
