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
 * A org.cougaar.core.society.NodeMessage  provides a basic implementation of 
 *  NodeMessage
 */
public class NodeMessage 
  extends Message
{
  //
  // Unlike ClusterMessage there is no IncarnationNumber.  This might
  // be required in the future for reliable Node communication.
  // 

  /**
   * Constructor
   * <p>
   * @param source The NodeIdentifier of creator node 
   * @param destination The NodeIdentifier of the target node
   * @return org.cougaar.core.society.NodeMessage
   **/
  public NodeMessage(NodeIdentifier s, NodeIdentifier d) {
    super(s, d);
  }

  /** 
   * no-arg Constructor.
   * This is not generally allowed in 1.1 event handling because 
   * EventObject requires a source object during construction.  Base 
   * class does not support this type of construction so it cannot 
   * be done here.
   * @return org.cougaar.core.society.NodeMessage
   */
  public NodeMessage() {
    super();
  }

  /**
   *  We provide the translation from the object version.  Unfortunately 
   * we cannot return a different type in java method overloading so the 
   * method signature is changed.  Mark it final to allow the compilier 
   * to inline optimize the function.
   * @return NodeIdentifier Identifies the originator of this directive
   */
  public final NodeIdentifier getSource(){
    return (NodeIdentifier)getOriginator();
  }

  /**
   * We provide the translation from the Object version in Message to the 
   * Type sepecific version for the Node messageing subsystem.
   * Mark it final to allow the compilier to inline optimize the function.
   * @return NodeIdentifier Identifies the reciever of the directive
   */
  public final NodeIdentifier getDestination() {
    return (NodeIdentifier)getTarget();
  }

  /**
   * Source is stored as na object so that message can service all objects.
   * Mark it final to allow the compilier to inline optimize the function.
   * @param asource - Set the NodeIdentifier of the originator of this message
   */
  public final void setSource(NodeIdentifier asource) {
    setOriginator( asource );
  }

  /**
   * Target is stored as na object so that message can service all objects.
   * Mark it final to allow the compilier to inline optimize the function.
   * @param adestination - Set the NodeIdentifier of the receiver of this message
   */
  public final void setDestination(NodeIdentifier adestination) {
    setTarget(adestination);
  }

  public String toString() {
    return "<NodeMessage "+getSource()+" - "+getDestination()+">";
  }
}
