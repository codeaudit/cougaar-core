/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.planning.ldm.plan;

import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Enumeration;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.RoleScheduleImpl;

/**
   Override the default property descriptors.
   A property descriptor contains:
   attribute name, bean class, read method name, write method name
   All other beaninfo is defaulted.
   This defines appropriate properties from the RoleSchedule INTERFACE,
   but is actually used to introspect on the RoleSchedule IMPLEMENTATION.
*/

public class RoleScheduleImplBeanInfo extends SimpleBeanInfo {

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[2];
    try {
      Class RoleScheduleClass = Class.forName("org.cougaar.planning.ldm.plan.RoleScheduleImpl");
      pd[0] = new PropertyDescriptor("availableSchedule",
				     RoleScheduleClass,
				     "getAvailableSchedule",
				     null);
      pd[1] = new IndexedPropertyDescriptor("roleSchedule",
					    RoleScheduleClass,
					    "getRoleScheduleIDs", null,
					    "getRoleScheduleID", null);
      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(RoleScheduleClass.getSuperclass()).getPropertyDescriptors();
      PropertyDescriptor[] finalPDs = new PropertyDescriptor[additionalPDs.length + pd.length];
      System.arraycopy(pd, 0, finalPDs, 0, pd.length);
      System.arraycopy(additionalPDs, 0, finalPDs, pd.length, additionalPDs.length);
      return finalPDs;
    } catch (Exception e) {
      System.out.println("Exception:" + e);
    }
    return null;
  }

}
