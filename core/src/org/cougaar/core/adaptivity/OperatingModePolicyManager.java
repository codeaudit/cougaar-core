/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.component.ServiceBroker;

/** 
 * A PolicyManager that handles OperatingModePolicies
 * For now, it listens for OperatingModePolicies and uses the 
 * PlaybookConstrainService to constrain the playbook with the OMPolicies.
 * It also sets the values of OperatingModes for non-adaptive OMPolicies.
 * In the future, it will forward InterAgentOperatingModePolicies to
 * other entities.
 */

public class OperatingModePolicyManager extends ServiceUserPluginBase {
  private PlaybookConstrainService playbookConstrainService;
  private OperatingModeService operatingModeService;

  private static final Class[] requiredServices = {
    PlaybookConstrainService.class,
    OperatingModeService.class
  };


  private static UnaryPredicate policyPredicate = 
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof OperatingModePolicy) {
	    return true;
	  }
	  return false;
	}
      };

  private IncrementalSubscription policySubscription;

  public OperatingModePolicyManager() {
    super(requiredServices);
  }

  public void setupSubscriptions() {

    policySubscription = (IncrementalSubscription) blackboard.subscribe(policyPredicate);

  }

  private boolean haveServices() {
    if (playbookConstrainService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      playbookConstrainService = (PlaybookConstrainService)
        sb.getService(this, PlaybookConstrainService.class, null);
      operatingModeService = (OperatingModeService)
        getServiceBroker().getService(this, OperatingModeService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (policySubscription.hasChanged()) {
      removePolicies(policySubscription.getRemovedCollection());
      changePolicies(policySubscription.getChangedCollection());
      addPolicies(policySubscription.getAddedCollection());
    }
  }

  /**
   * Constrain the playbook with the new policies
   */
  private void addPolicies(Collection newPolicies) {
    if (logger.isInfoEnabled()) logger.info("Adding policy");
    if (haveServices()) {
      for (Iterator it = newPolicies.iterator(); it.hasNext();) {
	OperatingModePolicy omp = (OperatingModePolicy)it.next();
	if (nonAdaptive(omp)) {
	  playbookConstrainService.constrain(omp);
	}
      }
    }
  }

  /**
   * Unconstrain the playbook with the removed policies
   */
  private void removePolicies(Collection removedPolicies)  {
    if (logger.isInfoEnabled()) logger.info("Removing policy");
    if (haveServices()) {
      for (Iterator it = removedPolicies.iterator(); it.hasNext();) {
	playbookConstrainService.unconstrain((OperatingModePolicy)it.next());
      }
    }
  }
  
  /**
   * Unconstrain, then reconstrain the playbook with the
   * changed policies.
   */
  private void changePolicies(Collection changedPolicies) {
    if (logger.isInfoEnabled()) logger.info("Changing policy");
    if (haveServices()) {
      for (Iterator it = changedPolicies.iterator(); it.hasNext();) {
	OperatingModePolicy omp = (OperatingModePolicy)it.next();
	if (nonAdaptive(omp)) {
	  playbookConstrainService.unconstrain(omp);
	  playbookConstrainService.constrain(omp);
	}
      }
    }
  }

  /**
   * This methods sets the values of OperatingModes of NonAdaptive Policies.
   * A non adaptivie policies is defined as one that has a TRUE ifClause and
   * only single point ranges as values of the OperatingModeConstraints.
   * @return true if the policy always restricts its OMs to a single point and has a TRUE ifClause
   * 
   **/
  private boolean nonAdaptive(OperatingModePolicy policy) {
    PolicyKernel pk = policy.getPolicyKernel();
    ConstrainingClause ifClause = pk.getIfClause();
    ArrayList keepers = new ArrayList(13);

    if (ifClause.equals(ConstrainingClause.TRUE_CLAUSE)) {
      ConstraintPhrase[] cps = pk.getOperatingModeConstraints();

      for (int i=0; i<cps.length; i++) {
	OMCRangeList rl = cps[i].getAllowedValues();
	OMCRange[] ranges = rl.getAllowedValues();

	if (ranges.length > 1) {
	  return false;
	}
	if (!(ranges[0] instanceof OMCPoint)) {
	  return false;
	} 
	keepers.add(cps[i]);
      }
    }

    if (keepers.size() == 0) {
      return false;
    }

    for (Iterator it = keepers.iterator(); it.hasNext();) {
      ConstraintPhrase cp = (ConstraintPhrase)it.next();
      
      /* lookup and set operating mode corresponding to this phrase */
      OperatingMode om = operatingModeService.getOperatingModeByName(cp.getProxyName());
      if (om != null) {
	OMCRange theValue = cp.getAllowedValues().getAllowedValues()[0];
	om.setValue(theValue.getMin());
	blackboard.publishChange(om);
      }
    }
    return true;
  }
}

