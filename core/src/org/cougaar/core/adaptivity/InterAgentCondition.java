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

import java.util.Set;
import java.util.Collections;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.relay.Relay;

/**
 * The Condition part of a remotely-controlled Condition. This is the
 * Relay.Target that receives updates from the InterAgentOperatingMode
 * Relay.Source. An instance of this class is instantiated on the
 * target's blackboard and acts as any other Condition such as
 * providing an input to the adaptivity engine.
 **/
public class InterAgentCondition
  extends OMCBase
  implements Relay.Target, Condition, UniqueObject
{
  private UID uid;
  private MessageAddress source;
  private Relay.Token owner;

  InterAgentCondition(
      InterAgentOperatingMode iaom, UID uid, MessageAddress src, 
      Relay.Token owner) {
    super(iaom.getName(), iaom.getAllowedValues(), iaom.getValue());
    this.owner = owner;
    this.uid = uid;
    this.source = src;
  }

  // UniqueObject implementation
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID (unique identifier) of this UniqueObject. Should
   * never be used. The uid is always set in the constructor to match
   * that of the Relay.Source.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    throw new RuntimeException("Attempt to change UID");
  }

  // Relay.Target implementation
  /**
   * Get the address of the Agent holding the Source copy of
   * this Relay.
   **/
  public MessageAddress getSource() {
    return source;
  }

  /**
   * Get the current response for this target. Null indicates that
   * this target has no response. This implementation never has a
   * response so it always returns null.
   **/
  public Object getResponse() {
    return null;
  }

  /**
   * Update with new content. The only part of the source content used
   * is the value of the operating mode.
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. This implementation returns true only
   * if the new value differs from the current value.
   **/
  public boolean updateContent(Object content, Relay.Token token) {
    if (token != owner) throw new IllegalArgumentException("Not owner");
    InterAgentOperatingMode newOM = (InterAgentOperatingMode) content;
    if (getValue().compareTo(newOM.getValue()) != 0) {
      setValue(newOM.getValue());
      return true;
    }
    return false;
  }
}
