/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Enumeration;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.RoleScheduleImpl;

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
      Class RoleScheduleClass = Class.forName("org.cougaar.domain.planning.ldm.plan.RoleScheduleImpl");
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
