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

/**
 * Directive interface
 * Directive is the highest-level message which is directly relevant to
 * real operations.  There are no direct implementations of Directive,
 * but there are several subclasses with implementations.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewDirective.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
 **/

public interface NewDirective extends Directive {
  /** @param plan - the plan this directive is attached to*/
    void setPlan(Plan plan);
    
  /*
  	*	Depricated because it is inherited from the base interface Message
   * @param asource - Set the ClusterIdentifier of the originator of this message
   */
  void setSource(ClusterIdentifier asource);
  
  /*
   * @param adestination - Set the ClusterIdentifier of the receiver of this message
   */
  void setDestination(ClusterIdentifier adestination);
  
 }