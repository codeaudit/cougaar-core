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

import java.util.*;

/** ContainerAPI is the interface used by Binder and/or BinderFactory to
 * invoke Binder-privileged methods on the parent Container.  Note that
 * the object implementing ContainerAPI will usually not be the the Container
 * itself.
 * <p>
 * The API itself represents the minimal set of services which all binders need
 * from the container.
 **/
public interface ContainerAPI
  extends BindingSite
{
  /** Remove the component (presumably the child) from the container **/
  boolean remove(Object childComponent);
}
