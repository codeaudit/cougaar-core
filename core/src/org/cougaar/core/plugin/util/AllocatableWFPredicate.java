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
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.Task;

import java.util.Enumeration;

public class AllocatableWFPredicate  implements UnaryPredicate, NewAllocatableWFPredicate {
    
    private Verb myVerb;

    public AllocatableWFPredicate() {
    }

    /** Overloaded constructor for using from the scripts. 
     *  Discouraged to use from plugins directly.
     */
    public AllocatableWFPredicate( Verb ver ) {
	myVerb = ver;
    }

    public void setVerb( Verb vb ) {
	myVerb = vb;
    }
    
    public boolean execute(Object o) {
	if (o instanceof PlanElement) {
	    PlanElement p = (PlanElement) o;
	    if (p instanceof Expansion) {
		Workflow wf = ((Expansion)p).getWorkflow();
		Enumeration e = wf.getTasks();
		Task t = (Task) e.nextElement();
		
		if ( t.getPlanElement() == null ) {
		    //Returns true if the current task is a supply task
		    if (t.getVerb().equals( myVerb )){
			return true;
		    }
		}
	    }
	}
	return false;
    }
}
