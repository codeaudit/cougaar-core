package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A Binder is an abstract notion of an implementation of
 * a BindingSite as provided by some other actor, usually
 * the Component's ContainingComponent for the enclosing
 * context or the ServiceProvider for requested services.
 * Sometimes, the ContainingComponent may interpose a
 * proxy between the actual service and the requesting component.
 * <p>
 **/

public interface Binder extends BindingSite 
{
  /** return the plugin (component) bound by this binder **/
  Object getPlugin();
}

