/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

/** 
 * A tuple containing a Component description and the Component's state.
 *
 * @see StateObject
 */
public final class StateTuple implements java.io.Serializable
{
  private final ComponentDescription cd;
  private final Object state;

  public StateTuple(
      ComponentDescription cd,
      Object state) {
    this.cd = cd;
    this.state = state;
  }

  /**
   * Get the ComponentDescription.
   */
  public ComponentDescription getComponentDescription() {
    return cd;
  }

  /**
   * Get the state for the Component.
   * <p>
   * The state refers to the state of the Component specified
   * in the descriptor, including any child Components.
   */
  public Object getState() {
    return state;
  }

  public String toString() {
    return 
      "Tuple {"+cd+", "+
      ((state != null) ? 
       state.getClass().getName() : 
       "null")+
      "}";
  }
}
