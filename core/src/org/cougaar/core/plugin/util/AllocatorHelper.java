/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin.util;

import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.domain.planning.ldm.plan.Preposition;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.ScoringFunction;
import org.cougaar.domain.planning.ldm.plan.AspectScorePoint;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.Role;

import org.cougaar.domain.planning.ldm.asset.AbstractAsset;
import org.cougaar.domain.planning.ldm.asset.Asset;


import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.Subscriber;


import java.util.Enumeration;
import java.util.Vector;

/**
 * Helper class for building Allocator Plugins.
 */
public class AllocatorHelper {

    /**
     * UpdatePV looks for differences between the reported and
     * estimated allocation results. If they are
     * different then the estimated value is set to the reported
     * value in both the cases.
     * @deprecated use boolean PlugInHelper.updatePlanElement(PlanElement pe) instead, calling publishChange(pe) if updatePlanElement returns true
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
     * @deprecated use PlugInHelper.updateAllocationResult(IncrementalSubscription sub) instead.
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
     * @deprecated use PlugInHelper.createEstimatedAllocationResult(Task t, RootFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, RootFactory ldmf) {
        return ExpanderHelper.createEstimatedAllocationResult(t, ldmf, 0.0);
    }
}
