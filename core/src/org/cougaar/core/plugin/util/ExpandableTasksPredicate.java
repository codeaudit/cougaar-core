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

import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.Task;

import java.util.Enumeration;

public class ExpandableTasksPredicate implements UnaryPredicate, NewExpandableTasksPredicate {
    
    private Verb myVerb;

    public ExpandableTasksPredicate() {
    }

    /** Overloaded constructor for using from the scripts. 
     *  Discouraged to use from plugins directly.
     */
    public ExpandableTasksPredicate( Verb ver ) {
	myVerb = ver;
    }

    public void setVerb( Verb vb ) {
	myVerb = vb;
    }
    
    public boolean execute(Object o) {
	if ( o instanceof Task ) {
	    Task t = ( Task ) o;
		if ( (t.getWorkflow() == null) &&
		     (t.getPlanElement() == null) &&
		     (t.getVerb().equals( myVerb ) )  ) {
		    return true;
		}
	}
	return false;
    }
}
