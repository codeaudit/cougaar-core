/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.io.PrintStream;
import org.cougaar.util.PropertyParser;

/** implement a low-priority heartbeat function which
 * just prints '.'s every few seconds when nothing else
 * is happening.
 *
 * @property org.cougaar.core.agent.heartbeat Unless disabled, the node
 * will provide a heartbeat to the vm.
 *
 * @property org.cougaar.core.agent.idleInterval 
 * How long between idle detection and heartbeat cycles (prints '.');
 *
 * @property org.cougaar.core.agent.idle.verbose
 *   If <em>true</em>, will print elapsed time (seconds) since
 *   cluster start every idle.interval millis.
 *
 * @property org.cougaar.core.agent.idle.verbose.interval=60000
 *   The number of milliseconds between verbose idle reports.
 *
 * @property org.cougaar.core.agent quiet Makes standard output as quiet as possible.  
 * If Heartbeat is running, will not print dots.
 **/

public final class Heartbeat 
{

  private static int idleInterval = 5*1000;
  private static boolean idleVerbose = false; // don't be verbose
  private static long idleVerboseInterval = 60*1000L; // 1 minute
  private static long maxIdleInterval;

  static {
    idleInterval=PropertyParser.getInt("org.cougaar.core.agent.idleInterval", idleInterval);
    maxIdleInterval = (idleInterval+(idleInterval/10));
    idleVerbose = PropertyParser.getBoolean("org.cougaar.core.agent.idle.verbose", idleVerbose);
    idleVerboseInterval = PropertyParser.getInt("org.cougaar.core.agent.idle.verbose.interval",
                                                (int)idleVerboseInterval);
  }


  private long firstTime;
  private long lastVerboseTime;

  private static long lastHeartbeat = 0L;
  private static long idleTime = 0L;

  private Thread thread = null;

  /** Only node can construct a Heartbeat **/
  Heartbeat() {
    firstTime = System.currentTimeMillis();
    lastVerboseTime = firstTime;
  }


  synchronized void start() {
    if (thread != null) throw new RuntimeException("Attempted to restart Heartbeat!");

    thread = new Thread(new Beater(), "Heartbeat");
    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }
  
  synchronized void stop() throws SecurityException {
    if (thread == null) throw new RuntimeException("Attempted to stop a stopped Heartbeat!");
    thread.interrupt();
  }

  private class Beater implements Runnable {
    public void run() {
      // initialize the values
      firstTime = System.currentTimeMillis();
      lastVerboseTime = firstTime;

      // if heartbeat actually gets to run at least every 5.5 seconds,
      // we'll consider the VM idle.
      while (true) {
        try {
          Thread.sleep(idleInterval); // sleep for (at least) 5s
        } catch (InterruptedException ie) {
          Thread.interrupted();
          return;               // exit
        }
        showProgress(".");
        long t = System.currentTimeMillis();
        if (lastHeartbeat!=0) {
          long delta = t-lastHeartbeat;
          if (delta <= maxIdleInterval) {
            // we're pretty much idle
            idleTime += delta;
          } else {
            idleTime = 0;
          }
        }
        lastHeartbeat = t;
        
        if (idleVerbose) {
          long delta = t-lastVerboseTime;
          if (delta >= idleVerboseInterval) {
            showProgress("("+Long.toString(((t-firstTime)+500)/1000)+")");
            lastVerboseTime=t;
          }
        }
      }
    }
  }

  private static void showProgress(String p) {
    System.out.print(p);
  }

  /** @return an estimate of how long in milliseconds the VM has been 
   * approximately idle.
   **/
  public long getIdleTime() { 
    long delta = System.currentTimeMillis() - lastHeartbeat;
    if (delta <= maxIdleInterval) {
      return idleTime+delta;
    } else {
      return 0;
    }
  }

}

