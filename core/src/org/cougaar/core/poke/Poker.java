/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.poke;

import org.cougaar.core.component.*;

/**
 *  An interface that pokes Pokables when they have something to do. For example, a scheduler.
 **/
public interface Poker {

  /**
   * Tells poker to handle poking this object
   * @param managedItem the object to be put in the Poker's control.
   * @return a handle that the Pokable can use to tell the poker that it wants to be run.
   **/
  Pokable register(Pokable managedItem);

  /**
   * Do we want this?
   **/
  void unregister(Pokable removeMe);
}
