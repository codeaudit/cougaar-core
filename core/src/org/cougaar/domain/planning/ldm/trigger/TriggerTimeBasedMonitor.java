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

import java.util.List;
import java.util.Arrays;

/**
 * A TriggerTimeBasedMonitor is a kind of monitor that generates an
 * interrupt at regular intervals to check for a particular
 * condition on a fixed set of objects
 *
 * Uses system time
 */

public class TriggerTimeBasedMonitor implements TriggerMonitor {
  
  private Object[] my_objects;
  long my_last_ran;
  long my_msec_interval;

  public TriggerTimeBasedMonitor(long msec_interval, Object[] objects, PlugInDelegate pid) 
  {
    my_objects = objects;
    my_msec_interval = msec_interval;
    my_last_ran = System.currentTimeMillis();
  }
  
  public long getMsecInterval() {
    return my_msec_interval;
  }

  public Object[] getAssociatedObjects() {
    return my_objects;
  }

  public boolean ReadyToRun(PlugInDelegate pid) { 
    return (System.currentTimeMillis() - my_last_ran) > my_msec_interval;
  }

  public void IndicateRan(PlugInDelegate pid) { 
    my_last_ran = System.currentTimeMillis(); 
  }

  public long getRemainingTime() {
    return (my_msec_interval - (System.currentTimeMillis() - my_last_ran));
  }

}


