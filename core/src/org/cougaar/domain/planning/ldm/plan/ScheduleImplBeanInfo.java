/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
import org.cougaar.domain.planning.ldm.plan.ScheduleImpl;

/**
   Override the default property descriptors.
   A property descriptor contains:
   attribute name, bean class, read method name, write method name
   All other beaninfo is defaulted.
   This defines appropriate properties from the Schedule INTERFACE,
   but is actually used to introspect on the Schedule IMPLEMENTATION.
*/

public class ScheduleImplBeanInfo extends SimpleBeanInfo {

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[5];
    try {
      Class ScheduleClass = Class.forName("org.cougaar.domain.planning.ldm.plan.ScheduleImpl");
      int i = 0;
      pd[i++] = new PropertyDescriptor("scheduleType",
				     ScheduleClass,
				     "getScheduleType",
				     null);
      pd[i++] = new PropertyDescriptor("ScheduleElementType",
            ScheduleClass,
            "getScheduleElementType",
            null);
      //      pd[i++] = new PropertyDescriptor("simpleScheduleStartDate",
      //				     ScheduleClass,
      //				     "getSimpleScheduleStartDate",
      //				     null);
      //      pd[i++] = new PropertyDescriptor("simpleScheduleEndDate",
      //				     ScheduleClass,
      //				     "getSimpleScheduleEndDate",
      //				     null);
      pd[i++] = new IndexedPropertyDescriptor("scheduleElements",
					      ScheduleClass,
					      "getScheduleElements", null,
					      "getScheduleElement", null);
      pd[i++] = new PropertyDescriptor("startDate",
				       ScheduleClass,
				       "getStartDate",
				       null);
      pd[i++] = new PropertyDescriptor("endDate",
				       ScheduleClass,
				       "getEndDate",
				       null);
      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(ScheduleClass.getSuperclass()).getPropertyDescriptors();
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