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

/** GenericStateModel interface.
 *  This is the interface that defines state transitions for
 *  clusters, components and plugins.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: GenericStateModel.java,v 1.4 2001-08-08 22:13:29 twright Exp $
 */

public interface GenericStateModel {

  /** UNINITIALIZED state - should never be returned by getModelState() **/
  public static final int UNINITIALIZED = -1;
  /** initialized but not yet attached to an enclosing object **/
  public static final int UNLOADED = 1;
  /** attached to a cluster **/
  public static final int LOADED = 2;
  /** possibly doing work **/
  public static final int ACTIVE = 3;
  /** forbidden from doing new work, but may be reactivated **/
  public static final int IDLE = 4;

  /** Initialize.  Transition object from undefined to UNLOADED state.
   * Treat initialize() as an extended constructor.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void initialize() throws StateModelException;

  /**
   *  Object should transition to the LOADED state.
   * After initialize and before load, an object in notified about its
   * parents, services, etc.  After load, it should be ready to run (but not 
   * actually running). 
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void load() throws StateModelException;

  /** Called object should start any threads it requires.
   *  Called object should transition to the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void start() throws StateModelException;

  /** Called object should pause operations in such a way that they may
   *  be cleanly resumed or the object can be unloaded.
   *  Called object should transition from the ACTIVE state to
   *  the IDLE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void suspend() throws StateModelException;

  /** Called object should transition from the IDLE state back to
   *  the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void resume() throws StateModelException;

  /** Called object should transition from the IDLE state
   *  to the LOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void stop() throws StateModelException;

  /**  Called object should transition from ACTIVE or SERVING state
   *   to the LOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void halt() throws StateModelException;

  /** Called object should perform any cleanup operations and transition
   *  to the UNLOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void unload() throws StateModelException;

  /** Return the current state of the object: LOADED, UNLOADED, 
   * ACTIVE, or IDLE.
   * @return object state
   **/

  int getModelState();
}
 
