/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility;

import org.cougaar.core.mts.MessageAddress;

/**
 * A ticket specifies the destination node and other parameters
 * of agent movement.
 * <p>
 * A ticket is immutable.
 */
public abstract class AbstractTicket implements java.io.Serializable {

 
  public AbstractTicket() {
  }

  /**
   * An optional identifier for this ticket instance.
   * <p>
   * The identifier <u>must</u> be serializable and should be
   * immutable.   A UID is a good identifier.
   */
  public abstract Object getIdentifier();

  /**
   * Destination node for the mobile agent.
   * <p>
   * If the destination node is null then the agent should 
   * remain at its current node.  This is is useful for
   * a "force-restart".
   */
  public abstract MessageAddress getDestinationNode();
  
  /**
   * If true, force the agent to be restarted even if the agent 
   * is already on the destination node.
   * <p>
   * If false then a move of an agent already at the destination 
   * will reply with a trivial success.  In practice one would 
   * typically set this to <b>false</b> for performance reasons.
   * <p>
   * The "force reload" capability is primarily designed to test
   * the agent-side mobility requirements without actually 
   * relocating the agent.
   * <p>
   * If the agent and its components (plugins, services, etc) 
   * correctly implement the suspend and persistence APIs then 
   * the restart of an agent on the same node should have 
   * <i>no</i> permanent side effects.  A hundred restarts 
   * should have no side effect other than a temporary 
   * performance penalty.
   * <p>
   * On the other hand, a failure to support a restart might 
   * result in an agent:<ul>
   *  <li>memory leak (garbage-collection)</li>
   *  <li>thread leak (didn't stop/pool all theads)</li>
   *  <li>serialization/deserialization error</li>
   *  <li>state loss (some state not captured)</li>
   *  <li>internal deadlock (synchronization bug)</li>
   *  <li>persistence error</li>
   *  <li>naming-service mess</li>
   *  <li>crypto-key loss (unable to re-obtain identity)</li>
   *  <li>conflict with other services (health-check, etc)</li>
   * </ul>
   * or some other error that (ideally) should be easier to 
   * debug than the full relocation of the agent on another 
   * node.
   */
  public abstract boolean isForceRestart();
  
  public abstract int hashCode();
   
  public abstract boolean equals(Object o);

  public abstract String toString();

  private static long serialVersionUID;

}
