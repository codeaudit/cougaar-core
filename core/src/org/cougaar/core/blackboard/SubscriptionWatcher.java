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

package org.cougaar.core.blackboard;


public class SubscriptionWatcher {
  // we use this as the wait/notify semaphore

  public final static int EXTERNAL = 1;
  public final static int INTERNAL = 2;
  public final static int CLIENT = 3;

  /** have the collections changed since we last looked? */
  protected boolean externalFlag = false;
  protected boolean internalFlag = false;
  protected boolean clientFlag = false;
  
  public synchronized void signalNotify(int event) {
    switch (event) {
    case EXTERNAL: externalFlag = true; break;
    case INTERNAL: internalFlag = true; break;
    case CLIENT: clientFlag = true; break;
    default: break;
    }
    notifyAll();
  }      

  /** Wait for a signal to continue.  
   * @return true iff the wake signal is unconditional.
   **/

  public synchronized boolean waitForSignal() {
    while (! test() ) {
      try {
        wait();
      } catch (InterruptedException ie) {}
    }
    return clearSignal();
  }

  public synchronized boolean clearSignal() {
    boolean retval = clientFlag || internalFlag;

    externalFlag = false;
    internalFlag = false;
    clientFlag = false;

    return retval;
  }

  /** @return true IFF it is time to wake up.
   * by default, this will return true when any of 
   * externalFlag, internalFlag and clientFlag are true.
   **/
  protected boolean test() {
    return (externalFlag || internalFlag || clientFlag);
    //return (externalFlag || clientFlag);
  }

}


    
    
  
