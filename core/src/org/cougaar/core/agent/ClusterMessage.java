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

package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.mts.Message;
import java.io.Serializable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


/**
 * A org.cougaar.core.agent.ClusterMessage  provides a basic implementation of 
 *  ClusterMessage
 */

public class ClusterMessage 
  extends Message
{
  protected long theIncarnationNumber;

  /**
   *   Constructor
   *   <p>
   *   @param source The ClusterIdentifier of creator cluster 
   *		@param destination The ClusterIdentifier of the target cluster
   *   @return org.cougaar.core.agent.ClusterMessage
   **/
  public ClusterMessage(ClusterIdentifier s, ClusterIdentifier d, long incarnationNumber) {
    super( s, d );
    theIncarnationNumber = incarnationNumber;
  }
    
  /** 
   *	no-arg Constructor.
   * 	This is not generally allowed in 1.1 event handling because EventObject requires
   *	a source object during construction.  Base class does not support this type of 
   *	construction so it cannot be done here.
   *  	@return org.cougaar.core.agent.ClusterMessage
   */
  public ClusterMessage() {
    super();
  }

  public long getIncarnationNumber() {
    return theIncarnationNumber;
  }

  public void setIncarnationNumber(long incarnationNumber) {
    theIncarnationNumber = incarnationNumber;
  }

  /**
   *  We provide the translation from the object version.  Unfortunately we cannot return
   *	a different type in java method overloading so the method signature is changed.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @return ClusterIdentifier Identifies the originator of this directive
   */
  public final ClusterIdentifier getSource(){
    return (ClusterIdentifier)getOriginator();
  }

  /**
   *	We provide the translation from the Object version in Message to the 
   *	Type sepecific version for the Cluster messageing subsystem.
   *  Mark it final to allow the compilier to inline optimize the function.
   *	@return ClusterIdentifier Identifies the reciever of the directive
   */
  public final ClusterIdentifier getDestination() {
    return (ClusterIdentifier)getTarget();
  }
  
  /*
   *  Source is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param asource - Set the ClusterIdentifier of the originator of this message
   */
  public final void setSource(ClusterIdentifier asource) {
    setOriginator( asource );
  }
  
  /*
   *  Target is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param adestination - Set the ClusterIdentifier of the receiver of this message
   */
  public final void setDestination(ClusterIdentifier adestination) {
    setTarget( adestination );
  }

  public String toString() {
    return "<ClusterMessage "+getSource()+" - "+getDestination()+">";
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(theIncarnationNumber);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    super.readExternal(in);
    theIncarnationNumber = in.readLong();
  }
}
