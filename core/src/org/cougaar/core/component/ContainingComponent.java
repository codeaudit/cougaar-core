package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/**
 * A Component which can itself contain other Components.
 **/
public interface ContainingComponent 
  extends Component, BeanContext
{
}
