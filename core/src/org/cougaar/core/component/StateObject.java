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
 * An <code>Object</code> that contains internal state.
 * <p>
 * Component mobility and persistence requires the saving and
 * restoration of internal Component state.  The typical lifecycle
 * for both scenarios is:<ol>
 *    <li>The Component is asked for it's state</li>
 *    <li>The Component is destroyed</li>
 *    <li>A new instance of the Component is created from a 
 *        <code>ComponentDescription</code>.  The new location
 *        might be on a different host.</li>
 *    <li>The state is passed to the StateComponent<li>
 * </ol><br>
 * <p>
 * All <code>Container</code>s are StateComponents because (minimally)
 * they contain a tree of child Components.
 * <p>
 * <code>Binder</code>s act as proxies for the child state-saving.
 * <p>
 * This interface might be removed in the future and replaced
 * with reflective method-lookup, similar to <code>BindingUtility</code>.
 */
public interface StateObject
{

  /**
   * Get the current state of the Component that is sufficient to
   * reload the Component from a ComponentDescription.
   *
   * @return null if this Component currently has no state
   */
  public Object getState();

  /**
   * Set-state is called by the parent Container if the state
   * is non-null.
   * <p>
   * The state Object is whatever this StateComponent provided
   * in it's <tt>getState()</tt> implementation.
   */
  public void setState(Object o);

}
