/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

// Perfect use for multiple inheritence, sigh.
package org.cougaar.core.plugin;

import org.cougaar.util.StateModelException;

/**
 * Just like SimplifiedPlugIn except that it extends 
 * SingleThreadedPlugIn instead of ThinPlugIn.  
 * @deprecated Use SimplePlugIn and call chooseThreadingModel(SINGLE_THREAD) 
 * from your constructor.
 **/

public abstract class SimplifiedFatPlugIn extends SingleThreadedPlugIn 
{
  /** */
  public SimplifiedFatPlugIn() {}

  //
  // final all the important state model functions.
  //

  public final void initialize() throws StateModelException {
    super.initialize();
  }
  public void load(Object object) throws StateModelException {
    super.load(object);
  }
  public final void start() throws StateModelException {
    super.start();
  }
  public final void suspend() throws StateModelException { 
    super.suspend();
  }
  public final void resume() throws StateModelException {  
    super.resume();
  }
  public final void stop() throws StateModelException {
    super.stop();
  }

  /** call initialize within an open transaction. **/
  protected final void prerun() {
    try {
      openTransaction();
      setupSubscriptions();
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println("Caught "+e);
        e.printStackTrace();
      }
    } finally {
      closeTransaction(false);
    }
  }    

  /** Called during initialization to set up subscriptions.
   * More precisely, called in the plugin's Thread of execution
   * inside of a transaction before execute will ever be called.
   **/
  protected abstract void setupSubscriptions();
  
  /** Call execute in the right context.  
   * Note that this transaction boundary does NOT reset
   * any subscription changes.
   * @See execute() documentation for details 
   **/
  protected final void cycle() {
    try {
      openTransaction();
      if (wasAwakened() || (getBlackboardService().haveCollectionsChanged())) {
        execute();
      }
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println("Caught "+e);
        e.printStackTrace();
      }
    } finally {
      closeTransaction();
    }
  }

  
  /**
   * Called inside of an open transaction whenever the plugin was
   * explicitly told to run or when there are changes to any of
   * our subscriptions.
   **/
  protected abstract void execute();

}

