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

/**
 * A Plan is an abstract data structure which consists of 
 * a set of PlanElements (which are associations between Tasks and
 *  Allocations).
 *
 * A future version of Plan may include an accessor to get an object
 * which can be asked to respond to plan-related cluster queries (e.g.
 * like ClusterCollectionOfPlanElements)
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Plan.java,v 1.2 2001-04-05 19:27:18 mthome Exp $
 */

public interface Plan 
{ 
  /**@return String Name of Plan */
  String getPlanName();

} 
