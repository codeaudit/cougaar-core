/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.examples.component;

import java.util.*;
import java.net.URL;
import org.cougaar.core.component.*;

/** Test case for component model.
 * To Run: java org.cougaar.core.examples.component.Test
 **/
public class Test {
  public static void main(String[] args) {
    // create a pluginManager container
    PluginManager pm = new PluginManager();

    // add some services to the container
    Services services = pm.getChildContext();
    // add a log service
    services.addService(TestLogService.class,
                        new ServiceProvider() {
                            public Object getService(Services s, Object r, Class sc) {
                              return new LogService();
                            }
                            public void releaseService(Services s, Object r, Class sc, Object service) {
                            }
                          });
    // add the alarm service
    services.addService(TestAlarmService.class,
                        new ServiceProvider() {
                            public Object getService(Services s, Object r, Class sc) {
                              return new AlarmService((TestAlarmService.Client) r);
                            }
                            public void releaseService(Services s, Object r, Class sc, Object service) {
                            }
                          });
    

    ComponentDescription bd = 
      new ComponentDescription("agent.plugin",
                               "org.cougaar.core.examples.component.TestPlugin",
                               null, // codebase
                               "Foo", // parameter
                               null, // cert
                               null, // lease
                               null // policy
                               );
    pm.add(bd);
    System.err.println("Added "+bd);
    
    System.err.println("Contents of PluginManager:");
    for (Iterator i = pm.iterator(); i.hasNext();) {
      Object c = i.next();
      System.err.println("\t"+c);
    }


    System.err.println("Done");
  }

  private static class LogService implements TestLogService {
    public void log(String message) {
      System.err.println("Log: "+message);
    }
  }

  private static class AlarmService implements TestAlarmService {
    final TestAlarmService.Client client;
    public AlarmService(TestAlarmService.Client client) {
      this.client = client;
    }
    public void wakeAfterDelay(final long millis) {
      Runnable waiter = new Runnable() {
          public void run() {
            try {
              Thread.sleep(millis);
            } catch (InterruptedException ie) {}
            client.wake();
          }
        };
      (new Thread(waiter)).start();
    }
  }

}
