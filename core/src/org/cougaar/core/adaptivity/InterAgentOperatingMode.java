/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import org.cougaar.core.relay.*;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.util.UID;

/**
 * A remotely-controlled Condition. Allows an adaptivity engine in one
 * agent to control a Condition of the adaptivity engine in another
 * agent. It is instantiated in the controlling agent and transferred
 * to the controlled agent using the Relay logic providers. A copy
 * of the instance is published in the controlled agent's blackboard
 * and used like any other Condition.
 **/
public class InterAgentOperatingMode
  extends OperatingModeImpl
  implements Relay.Source, Relay.Content
{
  private transient Set targets = Collections.EMPTY_SET;
  private UID uid;

  // Constructors
  public InterAgentOperatingMode(String name,
                                 OMCRangeList allowedValues)
  {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }

  public InterAgentOperatingMode(String name,
                                 OMCRangeList allowedValues,
                                 Comparable value)
  {
    super(name, allowedValues, value);
  }

  // Initialization methods
  /**
   * Set the message address of the target. This implementation
   * presumes that there is but one target.
   * @param targetAddress the address of the target agent.
   **/
  public void setTarget(ClusterIdentifier targetAddress) {
    targets = Collections.singleton(targetAddress);
  }

  // UniqueObject interface
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (uid != null) throw new RuntimeException("Attempt to change UID");
    this.uid = uid;
  }

  // Relay.Source interface
  /**
   * Get all the addresses of the target agents to which this Relay
   * should be sent. For this implementation this is always a
   * singleton set contain just one target.
   **/
  public Set getTargets() {
    return targets;
  }

  /**
   * Get an object representing the value of this Relay suitable
   * for transmission. This implementation uses itself to represent
   * its Content.
   **/
  public Relay.Content getContent() {
    return this;
  }

  /**
   * Get an object representing a response from the target with the
   * specified address. This implementation needs and has no responses
   * so we just return null.
   **/
  public Relay.Response getResponse(ClusterIdentifier ma) {
    return null;                // We don't expect to receive Responses
  }

  /**
   * Set the response that was sent from a target. For LP use only.
   * This implemenation does nothing because responses are not needed
   * or used.
   **/
  public void setResponse(ClusterIdentifier targetAddress, Relay.Response resp) {
    // No response expected
  }

  // Relay.Content implementation
  /**
   * Create an object to be published to the target's blackboard as
   * described by this Content. Often this will be the Content
   * itself for those implementations whose getContent() method
   * returns itself. Other implementations may create an instance of
   * a different class that is just sufficient to implement the
   * Relay.Target interface.
   * <p>
   * This implementation creates a new InterAgentCondition
   * @param uid the UID of the target instance. In some cases this
   * may be redundant with information in this Content, but simple
   * Content implementations need not carry the UID since it is also
   * passed in the Directive.
   **/
  public Relay.Target create(UID uid, ClusterIdentifier source, Relay.Token owner) {
    return new InterAgentCondition(this, uid, source, owner);
  }
}
