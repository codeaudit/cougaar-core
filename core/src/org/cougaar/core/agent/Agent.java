/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.agent;

import org.cougaar.core.component.Container;

/** 
 * This is the basic class required for
 * implementing an Agent.
 **/
public abstract class Agent implements Container {
  // Do we need state model stuff here???
  protected void initialize() {}

}
