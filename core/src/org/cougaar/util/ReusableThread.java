/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
