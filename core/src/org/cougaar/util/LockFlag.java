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

/** LockFlag class adapted from sample in   "Java Threads"
 * -- Scott Oaks and Henry Wong.  O'Reilly, Jan 1997
 **/

public class LockFlag {
  private Thread busyflag = null;
  private int busycount = 0;

  private static LockFlag defaultLock = new LockFlag();

  /** Returns a single VM-scoped LockFlag instance which can
   * be shared amongst many threads
   **/
  public static LockFlag getDefaultLock()
  {
    return defaultLock;
  }

  public synchronized void getBusyFlag()
  {
    while (tryGetBusyFlag() == false) {
      try {
        wait();
      } catch (Exception e) {}
    }
  }

  public synchronized boolean tryGetBusyFlag()
  {
    if ( busyflag == null ) {
      busyflag = Thread.currentThread();
      busycount = 1;
      return true;
    }
    if ( busyflag == Thread.currentThread() ) {
      busycount++;
      return true;
    }
    return false;
  }

  /** @return true on success.
   **/
  public synchronized boolean freeBusyFlag()
  {
    if ( getBusyFlagOwner() == Thread.currentThread()) {
      busycount--;
      if( busycount == 0 ) {
        busyflag = null;
        notify();
      }
      return true;
    } else {
      return false;
    }
  }

  public synchronized Thread getBusyFlagOwner()
  {
    return busyflag;
  }

  public int getBusyCount()
  {
    return busycount;
  }

}
