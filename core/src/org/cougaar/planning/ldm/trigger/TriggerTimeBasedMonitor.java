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

import org.cougaar.planning.plugin.legacy.PluginDelegate;

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

  public TriggerTimeBasedMonitor(long msec_interval, Object[] objects, PluginDelegate pid) 
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

  public boolean ReadyToRun(PluginDelegate pid) { 
    return (System.currentTimeMillis() - my_last_ran) > my_msec_interval;
  }

  public void IndicateRan(PluginDelegate pid) { 
    my_last_ran = System.currentTimeMillis(); 
  }

  public long getRemainingTime() {
    return (my_msec_interval - (System.currentTimeMillis() - my_last_ran));
  }

}


