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

// Perfect use for multiple inheritence, sigh.
package org.cougaar.core.plugin;

import org.cougaar.core.domain.*;

import org.cougaar.util.StateModelException;

/**
 * Just like SimplifiedPlugin except that it extends 
 * SingleThreadedPlugin instead of ThinPlugin.  
 * @deprecated Use SimplePlugin and call chooseThreadingModel(SINGLE_THREAD) 
 * from your constructor.
 **/

public abstract class SimplifiedFatPlugin extends SingleThreadedPlugin 
{
  /** */
  public SimplifiedFatPlugin() {}

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

