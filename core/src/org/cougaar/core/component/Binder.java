/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

import org.cougaar.util.GenericStateModel;

/** A Binder is an implementation of a BindingSite: that is
 * an implementation of the Service-like relationship API
 * between a child component and its parent.  A Binder
 * is the only view of the Parent that a child component
 * will start with - any other services, methods, etc required
 * must be requested via the child's binder.
 * <p>
 * Most Binder implementations will know
 * both the plugin (client) that they are "Binding" and the 
 * ServiceProvider for which they are implementing the BindingSite.
 * <p>
 * Binders must implement whatever Binder control interface required by the
 * associated Container and implement or delegate a refinement of BindingSite
 * to be called by the bound component.
 **/

public interface Binder 
  extends GenericStateModel, StateObject // BindingSite 
{
}

