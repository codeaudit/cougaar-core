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

/** A Service which offers the ability to add new components to the
 * containment hierarchy.  All ContainingComponents should offer this service 
 * (i.e. to their superiors)
 * Note that PluginService is not a BindingSite, so is not generally 
 * available to other (sibling) plugins.
 **/
public interface PluginService 
{
  /** add an object to the component 
   * @return true IFF successful 
   **/
  boolean add(Object o);

  /** remove an object from the component.
   * @return true IFF successful
   **/
  boolean remove(Object o);
}
  
