/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.core.plugin.PlugInDelegate;
import org.cougaar.core.cluster.IncrementalSubscription;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import org.cougaar.domain.planning.ldm.plan.PlanElement;

import org.cougaar.util.UnaryPredicate;

/**
 * A TriggerPredicateBasedMonitor is a kind of monitor that generates a
 * subscription for objects
 */

public class TriggerPredicateBasedMonitor implements TriggerMonitor {
  
  transient private IncrementalSubscription my_subscription;
  private UnaryPredicate my_predicate;
  transient private List assobjects = null;

  public TriggerPredicateBasedMonitor(UnaryPredicate predicate) {
    my_predicate = predicate;
    my_subscription = null;
  }

  public UnaryPredicate getPredicate() { return my_predicate; }

  public void EstablishSubscription(IncrementalSubscription subscription) {
    my_subscription = subscription;
  }

  public IncrementalSubscription getSubscription() {
    return my_subscription;
  }

  public Object[] getAssociatedObjects() {
    if (assobjects == null) {
      assobjects = new ArrayList();
    }
    assobjects.clear();
    // Pull objects out of subscription
    if (my_subscription != null) {
      // check for changes
      Enumeration clist = my_subscription.getChangedList();
      while (clist.hasMoreElements()){
        PlanElement pe = (PlanElement) clist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(pe) ) {
          assobjects.add(pe);
        }
      }
      // check for additions
      Enumeration alist = my_subscription.getAddedList();
      while (alist.hasMoreElements()){
        PlanElement pe = (PlanElement) alist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(pe) ) {
          assobjects.add(pe);
        }
      }
       
    }
    //System.err.println("Returning "+assobjects.size()+" objects to be tested");      
    return assobjects.toArray();
  }

  public boolean ReadyToRun(PlugInDelegate pid) { 
    // Check if subscription has changes  (don't need pid for right now)
    if ( (my_subscription != null) && (my_subscription.hasChanged()) ) {
      return true;
    }
    return false;
  }

  public void IndicateRan(PlugInDelegate pid) {
    // Probably nothing to do in this case
  }

  

}
