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
 * @version $Id: GenericStateModel.java,v 1.2 2001-04-05 19:27:26 mthome Exp $
 */

public interface GenericStateModel {

  /** UNINITIALIZED state - should never be returned by getState() **/
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
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void initialize() throws StateModelException;

  /** Notify object about its "parent"
   *  Object should transition to the LOADED state.
   *  Called object should check that caller is an instanceof
   *  the appropriate class
   *  @param o the "parent" object of the object being loaded
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  void load(Object o) throws StateModelException;

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

  int getState();
}
 
