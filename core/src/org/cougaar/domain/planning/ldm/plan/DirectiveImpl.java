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

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewDirective;
import java.io.Serializable;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.cougaar.core.cluster.ClaimableImpl;

/**
 * A DirectiveImpl  provides a basic implementation of
 *  Directive for extension purposes only.
 */

public class DirectiveImpl 
  extends ClaimableImpl
  implements Directive, NewDirective, Serializable
{

  protected ClusterIdentifier source = null;
  protected ClusterIdentifier destination = null;

  //protected transient Plan theplan;   // Made transient for Persistence

  /** 
   *	no-arg Constructor.
   * @return DirectiveImpl
   */
  protected DirectiveImpl() {
    super();
  }
   
		
  //Directive interface method implementations
		
  /** getPlan method
   * Returns an object that represents the plan
   * that this task is in reference to.  All Tasks
   * are members of a Plan.
   * <PRE> Plan myplan = mydirective.getPlan(); </PRE>
   * @return Plan
   **/
    
  public Plan getPlan() {
    //return theplan;
    return PlanImpl.REALITY;
  }
  
  /**
   * @return ClusterIdentifier Identifies the originator of this message
   */
  public ClusterIdentifier getSource() {
    return source;
  }

  /*
   *@return ClusterIdentifier Identifies the receiver of the message
   */
  public ClusterIdentifier getDestination() {
    return destination;
  }
  
  /** @param plan - the plan this directive is attached to*/
  public void setPlan(Plan plan) {
    //theplan = plan;
  }
    
  /*
   *	Depricated because it is inherited from the base interface Message
   * @param asource - Set the ClusterIdentifier of the originator of this message
   */
  public void setSource(ClusterIdentifier asource) {
    source = asource;
  }
  
  /*
   * @param adestination - Set the ClusterIdentifier of the receiver of this message
   */
  public void setDestination(ClusterIdentifier adestination) {
    destination = adestination;
  }


  //
  // implement read/write object here to provide top-level object stack implementations
  //
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
  }
}
