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

package org.cougaar.util;

/**
 * A Thread class which can be restarted to rerun it's (settable)
 * runnable object.
 **/

public class ReusableThread extends Thread {
  /** reference to our thread pool so we can return when we die **/
  private ReusableThreadPool pool;
  
  /** our runnable object, or null if we haven't been assigned one **/
  private Runnable runnable = null;
  
  /** Has this thread already be actually started yet?
   * access needs to be guarded by runLock.
   **/
  private boolean isStarted = false;

  /** are we actively running the runnable? **/
  private boolean isRunning = false;

  /** guards isRunning, synced while actually executing and waits when
   * suspended.
   **/
  private Object runLock = new Object();

  public void setRunnable(Runnable r) {
    runnable = r;
  }
  protected Runnable getRunnable() {
    return runnable;
  }

  /** The only constructor. **/
  public ReusableThread(ReusableThreadPool p) {
    super(p.getThreadGroup(), null, "ReusableThread");
    setDaemon(true);
    pool = p;
  }

  public final void run() {
    while (true) {
      synchronized (runLock) {
        Runnable r = getRunnable();
        if (r != null)
          r.run();
        isRunning = false;

        reclaim();

        try {
          runLock.wait();       // suspend
        } catch (InterruptedException ie) {}
      }
    }
  }

  public void start() throws IllegalThreadStateException {
    synchronized (runLock) {
      if (isRunning) 
        throw new IllegalThreadStateException("ReusableThread already started: "+
                                              this);
      isRunning = true;

      if (!isStarted) {
        isStarted=true;
        super.start();
      } else {
        runLock.notify();     // resume
      }
    }
  }

  protected synchronized void reclaim() {
    pool.reclaimReusableThread(this);
    notifyAll();
  }
}
