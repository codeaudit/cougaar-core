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

/** GenericStateModel interface.
 *  This is the interface that defines state transitions for
 *  clusters, components and plugins.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 *
 */

public interface GenericStateModel {

  /** UNINITIALIZED state - should never be returned by getModelState() **/
  int UNINITIALIZED = -1;
  /** initialized but not yet attached to an enclosing object **/
  int UNLOADED = 1;
  /** attached to a parent container **/
  int LOADED = 2;
  /** possibly doing work **/
  int ACTIVE = 3;
  /** forbidden from doing new work, but may be reactivated **/
  int IDLE = 4;

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
 
