/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.beans.*;
import java.util.*;

/** A useful base class which allows an object to be it's own BeanInfo
 * class.  The defaults just implement the BeanInfo interface with 
 * null return values except that we also provides a simple API for 
 * recursive construction of PropertyDescriptor arrays.
 **/

public class SelfDescribingBeanInfo implements BeanInfo {
  public BeanDescriptor getBeanDescriptor() { return null; }
  public int getDefaultPropertyIndex() { return -1; }
  public EventSetDescriptor[] getEventSetDescriptors() { return null; }
  public int getDefaultEventIndex() { return -1; }
  public MethodDescriptor[] getMethodDescriptors() { return null; }
  public BeanInfo[] getAdditionalBeanInfo() { return null; }
  public java.awt.Image getIcon(int iconKind) { return null; }

  private static final PropertyDescriptor[] _emptyPD = new PropertyDescriptor[0];
  public PropertyDescriptor[] getPropertyDescriptors() { 
    Collection pds = new ArrayList();
    try {
      addPropertyDescriptors(pds);
    } catch (IntrospectionException ie) {
      System.err.println("Warning: Caught exception while introspecting on "+this.getClass());
      ie.printStackTrace();
    }
    return (PropertyDescriptor[]) pds.toArray(_emptyPD);
  }

  /** Override this to add class-local PropertyDescriptor instances
   * to the collection c.  Make sure that overridden methods call super 
   * as appropriate.
   **/
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
  }

}
