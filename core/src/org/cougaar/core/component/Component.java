package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A Component is the base class of the Component and
 * Service Model.  Components form a strict hierarchy
 * via Container/Contained relationships and may have
 * any number of additional (possibly mediated) client/server 
 * relationships.
 **/
public interface Component 
  extends BeanContextChild
{
  /** Published Policy/techspec of the component for negotiation
   * with a ContainingComponent.
   **/
  Object getPolicy();
}

