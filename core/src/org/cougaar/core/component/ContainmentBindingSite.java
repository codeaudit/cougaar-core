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

/** Containment Binders (e.g. PluginBinder, etc) must implement
 * support for Service publish and request.
 * @see BeanContextServices for the API.
 **/
public interface ContainmentBindingSite
  extends BindingSite, BeanContextServices 
{
  /** A Component may call this method to request that it be stopped **/
  void requestStop();

  // more component-to-parent requests go here

  // extending interfaces might offer all sorts of QUO Services
  // directly or via additional services.
}

