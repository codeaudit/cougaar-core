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

import org.cougaar.core.plugin.PlugInDelegate;

import java.io.Serializable;

/**
 *
 * A Predictor is an object intended to be available on an 
 * OrganizationalAsset which provides a prediction of how the 
 * associated remote cluster WOULD respond if it were allocated a given
 * task. The predictor should be self-contained, meaning that it should
 * not require any resources other than those of the provided task and
 * its own internal resources to provide the allocation response.
 *
 * It should be noted that a Predictor is not required for every 
 * OrganizationalAsset : some clusters will not provide Predictors.
 *
 * It is anticipated that a predictor class will be optionally specified in 
 * a cluster's initialization file (<clustername>.ini) which will allow
 * the cluster to pass an instance of the predictor embedded in the
 * OrganizationalAsset copy of itself when it hooks up with other clusters.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Predictor.java,v 1.2 2001-04-05 19:27:18 mthome Exp $
 */
  
public interface Predictor extends Serializable {
    
  /** @param Task for_task
   * @param PlugInDelegate plugin
   * @return AllocationResult A predictive result for the given task.
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   **/
  AllocationResult Predict(Task for_task, PlugInDelegate plugin);
    
}
