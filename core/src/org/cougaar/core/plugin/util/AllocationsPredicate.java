/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin.util;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Task;

public class AllocationsPredicate  implements UnaryPredicate, NewAllocationsPredicate {
    
    private Verb myVerb;

    public AllocationsPredicate() {
    }

    /** Overloaded constructor for using from the scripts. 
     *  Discouraged to use from plugins directly.
     */
    public AllocationsPredicate( Verb ver ) {
	myVerb = ver;
    }

    public void setVerb( Verb vb ) {
	myVerb = vb;
    }

    public boolean execute(Object o) {
	if (o instanceof PlanElement) {
	    Task t = ((PlanElement)o).getTask();
	    if (t.getVerb().equals( myVerb )) {

		// if the PlanElement is for the correct kind of task - make sure its an allocation
		PlanElement p = ( PlanElement )o;
		if ( p instanceof Allocation) {
		    return true;
		}

	    }
	}
	return false;
    }
}
