/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.core.plugin.PlugInDelegate;

/**
 * A Trigger is an object containing information indicating an action
 * to be taken if a particular state exists among a specified set of objects.
 * The trigger contains three pieces:
 *    MONITOR - Establishes the set of objects on which to test for the state
 *    TESTER - Determines if the state exists
 *    ACTION - Performs an action on the set of objects (or other class
 *                        state info)
 *
 * The Trigger contains an Execute method which captures the logic 
 * of executing trigger actions when the monitored state exists.
 *
 */

public class Trigger implements java.io.Serializable {
  
  private TriggerMonitor my_monitor;
  private TriggerTester my_tester;
  private TriggerAction my_action;

  /** Basic Constructor.
    * @param monitor
    * @param tester
    * @param action
    * @return Trigger
    */
  public Trigger(TriggerMonitor monitor, TriggerTester tester, TriggerAction action) {
    my_monitor = monitor;
    my_tester = tester;
    my_action = action;
  }

  /**
   * Is this trigger fully filled in, and if so, is the monitor ready to run?
   */
  public boolean ReadyToRun(PlugInDelegate pid) { 
    // note don't worry if the tester is null, we could have a monitor and an action.
    if ( (my_monitor != null) &&  (my_action != null) 
      && (my_monitor.ReadyToRun(pid)) ) {
      return true;
    } else {
      return false;
    }
  }
  
  /** @return TriggerMonitor  The monitor associated with this Trigger. */
  public TriggerMonitor getMonitor() {
    return my_monitor;
  }


  /**
   * Run the trigger : if the condition exists on the objects, fire the action
   */
  public void Execute(PlugInDelegate pid) {
    Object[] objects = my_monitor.getAssociatedObjects();
    if (my_tester != null) {
      if (my_tester.Test(objects)) {
        my_action.Perform(objects, pid);
      }
    } else {
      // if we don't have a tester go straight to the action
      // but make sure that objects is not empty since the monitor objects
      // returned end up being our tester
      if (objects.length > 0 ) {
        my_action.Perform(objects, pid);
      }
    }
    my_monitor.IndicateRan(pid);
  }
  
  
}






