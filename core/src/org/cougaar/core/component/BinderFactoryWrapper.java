package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** An idea for the future:
 * A BinderFactory which encapsulates all BinderFactories known
 * to a component of a lower priority.  Introduces a hierarchy
 * of BinderFactories rather than a prioritized list.
 **/
public interface BinderFactoryWrapper
  extends BinderFactory
{
}
