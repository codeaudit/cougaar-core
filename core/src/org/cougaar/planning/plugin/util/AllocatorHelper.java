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

package org.cougaar.planning.plugin.util;

import java.util.Collection;
import java.util.Enumeration;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Task;

/**
 * Helper class for building Allocator Plugins.
 */
public class AllocatorHelper {

    /**
     * UpdatePV looks for differences between the reported and
     * estimated allocation results. If they are
     * different then the estimated value is set to the reported
     * value in both the cases.
     * @deprecated use boolean PluginHelper.updatePlanElement(PlanElement pe) instead, calling publishChange(pe) if updatePlanElement returns true
     */
    public static void updatePV ( PlanElement pe, CollectionSubscription sub ) {

	    if (pe.getReportedResult() != null) {
	      //compare the result objects.
        // If they are NOT ==, re-set the estimated result.
        // For now ignore whether their compositions are equal.
        AllocationResult repar = pe.getReportedResult();
	      AllocationResult estar = pe.getEstimatedResult();
	      if ( (estar == null) || (! (repar == estar) ) ) {
	      	pe.setEstimatedResult(repar);
		      sub.getSubscriber().publishChange( pe, null );
	      }
	    }
    }

    /** Takes a subscription, gets the changed list and updates the changedList.
     * @deprecated use PluginHelper.updateAllocationResult(IncrementalSubscription sub) instead.
     */
    public static void updateAllocationResult ( IncrementalSubscription sub ) {

	Enumeration changedPEs = sub.getChangedList();
	while ( changedPEs.hasMoreElements() ) {
	    PlanElement pe = (PlanElement)changedPEs.nextElement();
	    if (pe.getReportedResult() != null) {
		//compare entire pv arrays
		AllocationResult repar = pe.getReportedResult();
		AllocationResult estar = pe.getEstimatedResult();
		if ( (estar == null) || (!repar.isEqual(estar)) ) {
		    pe.setEstimatedResult(repar);
		    sub.getSubscriber().publishChange( pe, null );
		}
	    }
	}
    }


    /**
     * Checks if the Task is of specified OFTYPE.
     */
    public static boolean isOfType( Task t, String p, String typeid ) {
	Enumeration prepPhrases =  ( (Task)t ).getPrepositionalPhrases();
	PrepositionalPhrase pPhrase;
	String prep;
	AbstractAsset aa = null;
	String mytypeid = null;

	while ( prepPhrases.hasMoreElements() ) {
	    pPhrase = ( PrepositionalPhrase ) prepPhrases.nextElement();
	    prep = pPhrase.getPreposition();
	    if ( prep.equals( p ) ) {
		Object indirectobj = pPhrase.getIndirectObject();
		if( indirectobj instanceof AbstractAsset ) {
		    aa = (AbstractAsset) indirectobj;
		    mytypeid = aa.getTypeIdentificationPG().getTypeIdentification();
		    if ( mytypeid.equals( typeid ) )  return true;
		}
	    }
	}
	return false;
    }

    /**
     * @deprecated use PluginHelper.createEstimatedAllocationResult(Task t, PlanningFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, PlanningFactory ldmf) {
        return ExpanderHelper.createEstimatedAllocationResult(t, ldmf, 0.0);
    }
}
