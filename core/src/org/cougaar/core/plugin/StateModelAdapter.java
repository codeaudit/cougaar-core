/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.util.StateModelException;

/**
 * implement the standard state model
 *
 **/
public abstract class StateModelAdapter
  implements PlugInServesCluster {
  
  /** current reflection of PlugIn run state **/
  private int runState = UNINITIALIZED;

  /** PlugIn State model accessor.
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

  
  
