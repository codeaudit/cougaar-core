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
 * @version $Id: Predictor.java,v 1.3 2001-08-22 20:14:14 mthome Exp $
 */
  
public interface Predictor extends Serializable {
    
  /** @param Task for_task
   * @param PlugInDelegate plugin
   * @return AllocationResult A predictive result for the given task.
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   **/
  AllocationResult Predict(Task for_task, PlugInDelegate plugin);
    
}
