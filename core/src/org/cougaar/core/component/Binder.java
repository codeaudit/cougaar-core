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

