package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** Convenience interface for Components which wish to advertise
 * one or more Services to other components in the form of BindingSites.
 **/
public interface ServiceProvider
  extends BeanContextServiceProvider 
{
}

