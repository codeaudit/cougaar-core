/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

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


    
    
  
