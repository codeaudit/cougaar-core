/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

/** A marker class and interface for persisting the state of an
 * object which is not itself persistable is such a way that the
 * object can be reset.  This could be done with Serializable directly
 * but implies a rather different semantics.
 **/
public interface StatePersistable
{
  /** called by persistence to get a snapshot of the state. **/
  PersistenceState getPersistenceState();

  /** called during rehydration to set/reset the state of the 
   * object to the stored state.
   **/
  void setPersistenceState(PersistenceState state);
}
