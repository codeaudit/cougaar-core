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
