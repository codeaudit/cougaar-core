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
 
package org.cougaar.planning.ldm.plan;

import org.cougaar.core.agent.ClusterIdentifier;
import java.io.Serializable;

/**
 * Directive interface
 * Directive is the highest-level message which is directly relevant to
 * real operations.  There are no direct implementations of Directive,
 * but there are several subclasses with implementations.
 **/

public interface Directive
  extends Serializable
{

   /** getPlan method
    * Returns an object that represents the plan
    * that this task is in reference to.  All Tasks
    * are members of a Plan.
    * <PRE> Plan myplan = mydirective.getPlan(); </PRE>
    *
    * @return Plan
    **/
    
  Plan getPlan();
  
  /**
    * @return ClusterIdentifier Identifies the originator of this message
    */
  ClusterIdentifier getSource();

  /*
   *@return ClusterIdentifier Identifies the receiver of the message
   */
  ClusterIdentifier getDestination();
   
 }
