/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.cluster.ClusterIdentifier;
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
