/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 *  THIS SOFTWARE IS MODIFIED FOR TESTING QUO_ULTRALLOG INTEGRATION
 */
package org.cougaar.lib.quo.performance;

import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.cluster.IncrementalSubscription;
import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.glm.ldm.asset.Organization;
import org.cougaar.domain.glm.ldm.asset.OrganizationPG;
import org.cougaar.lib.quo.performance.assets.*;

/**
 * A predicate that matches all "CODE" tasks
 */
class myTaskPredicate implements UnaryPredicate{
    public boolean execute(Object o) {
	boolean ret = false;
	if (o instanceof Task) {
	    Task t = (Task)o;
	    ret = t.getVerb().equals(Verb.getVerb("CODE"));
	}
	return ret;
    }
}

/**
 * A predicate that matches all organizations that can
 * fulfill the SoftwareDevelopment role
 */
class myProgrammersPredicate implements UnaryPredicate{
    public boolean execute(Object o) {
	boolean ret = false;
	if (o instanceof Organization) {
	    Organization org = (Organization)o;
	    OrganizationPG orgPG = org.getOrganizationPG();
	    ret = orgPG.inRoles(Role.getRole("SoftwareDevelopment"));
	}
	return ret;
    }
}

/**
 * A predicate that matches allocations of "CODE" tasks
 */
class myAllocationPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Allocation) {
      Task t = ((Allocation)o).getTask();
      ret = (t != null) && (t.getVerb().equals(Verb.getVerb("CODE")));
    }
    return ret;
  }
}

/**
 * This COUGAAR PlugIn allocates tasks of verb "CODE"
 * to Organizations that have the "SoftwareDevelopment" role.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugIn.java,v 1.1 2001-08-09 20:56:56 psharma Exp $
 **/
public class ManagerAllocatorPlugIn extends SimplePlugIn {
  
  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // SoftwareDevelopment orgs
  private IncrementalSubscription allocations;   // My allocations


  /**
   * subscribe to tasks and programming organizations
   */
  protected void setupSubscriptions() {
    tasks = (IncrementalSubscription)subscribe(new myTaskPredicate());
    programmers = (IncrementalSubscription)subscribe(new myProgrammersPredicate());
    allocations = (IncrementalSubscription)subscribe(new myAllocationPredicate());
  }


  /**
   * Top level plugin execute loop.  Allocate CODE tasks to organizations
   */
  protected void execute () {
      allocateUnallocatedTasks();
  }

  /**
   * Allocate the task to the asset
   */
  private void allocateTo(Asset asset, Task task) {
    AllocationResult estAR = null;
    Allocation allocation =
      theLDMF.createAllocation(task.getPlan(), task, asset,estAR, Role.ASSIGNED);
    publishAdd(allocation);
  }

    protected void  allocateUnallocatedTasks(){
  	//   process unallocated tasks
	Enumeration task_enum = tasks.elements();
	while (task_enum.hasMoreElements()) {
	    Task t = (Task)task_enum.nextElement();
	    if (t.getPlanElement() != null)
		continue;  //already allocated
	    Asset organization = (Asset)programmers.first();
	    //Asset organization = (Asset)programmers;
	    if (organization != null)   //if no organization yet, give up for now
		allocateTo(organization, t);
	}
  }
}





