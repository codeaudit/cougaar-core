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

package org.cougaar.core.plugin.util;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

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
