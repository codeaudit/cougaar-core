/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;
import java.lang.reflect.*;

/** A Wrapper Binder contructed by ServiceFilter which 
 * watches service requests and has convenient overridable points
 * where extensions may monitor, edit, veto or wrap services
 * requested by the client.
 * <p>
 * Specific implementations still will need to implement getBinderProxy
 * so the right methods are presented
 **/
public abstract class ServiceFilterBinder 
  extends BinderSupport         // only uses part of it...
{
  protected ServiceFilterBinder(ContainerAPI parent, Component child) {
    super(parent, child);
  }

  /** Defines a pass-through insulation layer to ensure that the plugin cannot 
   * downcast the BindingSite to the Binder and gain control via introspection
   * and/or knowledge of the Binder class.  This is neccessary when Binders do
   * not have private channels of communication to the Container.
   **/
  protected abstract BindingSite getBinderProxy();


}
