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
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.Task;

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
