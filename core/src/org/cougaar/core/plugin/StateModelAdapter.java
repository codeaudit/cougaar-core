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

package org.cougaar.core.plugin;

import org.cougaar.util.StateModelException;

/**
 * implement the standard state model
 *
 **/
public abstract class StateModelAdapter
  implements PluginServesCluster {
  
  /** current reflection of Plugin run state **/
  private int runState = UNINITIALIZED;

  /** Plugin State model accessor.
   **/
  public final int getModelState() {
    return runState; 
  }

  /** simple initialize method. 
   * Transits the state to UNLOADED.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void initialize() throws StateModelException {
    transitState("initialize()", UNINITIALIZED, UNLOADED);
  }


  /** Notice which Cluster we are.
   * also transit to LOADED.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void load(Object obj) throws StateModelException {
    transitState("load()", UNLOADED, LOADED);
  }

  /** This version of start just transits to ACTIVE.
   * Daemon subclasses may want to start threads here.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void start() throws StateModelException {
    transitState("start()", LOADED, ACTIVE);
  }

  /** 
  *Just change the state to IDLE.
  **  @exception org.cougaar.util.StateModelException Cannot transition to new state.  
  **/
  public synchronized void suspend() throws StateModelException {
    transitState("suspend()", ACTIVE, IDLE);
  }

  /**
  *		Transit from IDLE to ACTIVE .
  *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.   
  **/
  public synchronized void resume() throws StateModelException {
    transitState("resume()", IDLE, ACTIVE);
  }

  /** 
  *	  Transit from IDLE to LOADED. 
  *	  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void stop() throws StateModelException {
    transitState("stop()", IDLE, LOADED);
  }

  /** Transit from ACTIVE to LOADED. 
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void halt() throws StateModelException {
    transitState("halt()", ACTIVE, LOADED);
  }

  /** Transit from LOADED to UNLOADED.
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void unload() throws StateModelException {
    transitState("unload()", LOADED, UNLOADED);
  }

  /** Accomplish the state transition.
  *   Be careful and complain if we are in an inappropriate starting state.
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.   
  **/
  private synchronized void transitState(String op, int expectedState, int endState) throws StateModelException {
    if (runState != expectedState) {
      throw new StateModelException(""+this+"."+op+" called in inappropriate state ("+runState+")");
    } else {
      runState = endState;
    }
  }


}  

  
  
