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


package org.cougaar.planning.ldm.trigger;

import org.cougaar.core.plugin.PluginDelegate;
import org.cougaar.core.blackboard.IncrementalSubscription;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

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
        Object subobj =  clist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(subobj) ) {
          assobjects.add(subobj);
        }
      }
      // check for additions
      Enumeration alist = my_subscription.getAddedList();
      while (alist.hasMoreElements()){
        Object subobj = alist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(subobj) ) {
          assobjects.add(subobj);
        }
      }
       
    }
    //System.err.println("Returning "+assobjects.size()+" objects to be tested");      
    return assobjects.toArray();
  }

  public boolean ReadyToRun(PluginDelegate pid) { 
    // Check if subscription has changes  (don't need pid for right now)
    if ( (my_subscription != null) && (my_subscription.hasChanged()) ) {
      return true;
    }
    return false;
  }

  public void IndicateRan(PluginDelegate pid) {
    // Probably nothing to do in this case
  }

  

}
