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

/**
*   imports
**/
import java.io.*;

import org.cougaar.domain.planning.ldm.policy.Policy;

/**
 *  MulticastMessage containing Policy objects
 **/
public class PolicyMulticastMessage 
  extends Message
{

  private Policy policy;

  /**
   *   Default Constructor for factory.
   *   <p>
   *   @param aSource The creator of this message used to consruct the super class
   **/
  public PolicyMulticastMessage() {
    super();
  }

  /**
   *   Constructor with a full parameter list
   *   <p>
   *   @param source The creator of this message used to consruct the super class
   *   @param aTarget The target for this message - must be a multicast address
   *   @param aPolicy  The policy of the message
    **/
  public PolicyMulticastMessage(MessageAddress aSource, MulticastMessageAddress aTarget, Policy aPolicy) {
    super(aSource, aTarget);
    setPolicy(aPolicy);
  }
    
  /**  Get the Policy in the message.
   *  @return Policy
   **/
  public Policy getPolicy() {
    return policy;
  }

  /** Set the policy object in the message
   *  @param Policy
   **/
  public void setPolicy(Policy aPolicy) {
    policy = aPolicy;
  }

  /**
   *   Overide the toString implemntation for all message classes
   *   @return String Formatted string for displayying all the internal data of the message.
   **/
  public String toString()
  {
    try {
      return super.toString() +
        " The Policy: " + getPolicy().toString();
    } catch (NullPointerException npe) {
      String output = "a Malformed Message: ";
      if ( getOriginator() != null )
        output += " The source: " + getOriginator().toString();
      else
        output += " The source: NULL";
      if ( getTarget() != null )
        output += "The Target: " + getTarget().toString();
      else  
        output += " The Target: NULL";
      if ( getPolicy() != null ) 
        output += " The Policy: " + getPolicy().toString();
      else
        output += " The Policy: NULL";

      return output;
    }
  }

  // externalizable support
  // we don't actually implement the Externalizable interface, so it is
  // up to subclasses to call these methods.
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(policy);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    super.readExternal(in);
    policy=(Policy)in.readObject();
  }
}
