/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;
import java.net.URL;

public class TestPlugin 
  implements Plugin, TestAlarmService.Client
{
  public TestPlugin() {
    System.err.println("TestPlugin()");
  }
  private Object parameter = null;
  public void setParameter(Object o) {
    parameter = o;
    System.err.println("TestPlugin.setParameter("+o+")");
  }

  private PluginBindingSite binder;
  public void setBinder(Binder b) {
    binder = (PluginBindingSite) b;
    System.err.println("TestPlugin.setBinder("+b+")");
  }
  /*
    // we used setBinder above...
  public void initialize(Binder b) {
    System.err.println("TestPlugin.initialize("+b+")");    
  }
  */
  
  // example of getting a service via binder introspection **/
  private TestLogService logger = null;
  public void setTestLogService(TestLogService logger) {
    if (logger == null) {
      System.err.println("Warning: no logger service available!");
    } else {
      this.logger = logger;
    }
  }
  public void initialize() {
    System.err.println("TestPlugin.initialize()");    

    // example of getting a service from the binder dynamically
    Services services = binder.getServices();
    TestAlarmService alarmer = (TestAlarmService) services.getService(this, TestAlarmService.class, null);
    if (alarmer == null) {
      System.err.println("Warning: no alarm service available!");
    } else {
      System.err.println("WakeAfterDelay(10Sec) starting at: "+System.currentTimeMillis());
      alarmer.wakeAfterDelay(10*1000L); // wake in ten seconds
    }

    if (logger!= null) logger.log("Done with initialize()");
  }

  // implement TestAlarmService.Client
  public void wake() {
    System.err.println("TestPlugin.wake() at "+System.currentTimeMillis());
  }

}
