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
import org.cougaar.domain.planning.ldm.plan.WorkflowImpl;

/**
   Override the default property descriptors.
   A property descriptor contains:
   attribute name, bean class, read method name, write method name
   All other beaninfo is defaulted.
   This defines appropriate properties from the Workflow INTERFACE,
   but is actually used to introspect on the Workflow IMPLEMENTATION.
*/

public class WorkflowImplBeanInfo extends SimpleBeanInfo {

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[5];
    int i = 0;
    try {
      Class workflowClass = Class.forName("org.cougaar.domain.planning.ldm.plan.WorkflowImpl");
      pd[i++] = new PropertyDescriptor("parentTask",
				       workflowClass,
				       "getParentTaskID",
				       null);
      pd[i++] = new IndexedPropertyDescriptor("tasks",
					      workflowClass,
					      "getTaskIDs", null,
					      "getTaskID", null);
      pd[i++] = new IndexedPropertyDescriptor("constraints",
					      workflowClass,
					      "getConstraintsAsArray", null,
					      "getConstraintFromArray", null);
      pd[i++] = new PropertyDescriptor("allocationResult",
				       workflowClass,
				       "getAllocationResult",
				       null);
      pd[i++] = new PropertyDescriptor("constraintViolation",
				       workflowClass,
				       "constraintViolation",
				       null);
      //      pd[i++] = new IndexedPropertyDescriptor("violatedConstraints",
      //					      workflowClass,
      //					      "getViolatedConstraintsAsArray", null,
      //					      "getViolatedConstraintFromArray", null);

      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(workflowClass.getSuperclass()).getPropertyDescriptors();
      PropertyDescriptor[] finalPDs = new PropertyDescriptor[additionalPDs.length + pd.length];
      System.arraycopy(pd, 0, finalPDs, 0, pd.length);
      System.arraycopy(additionalPDs, 0, finalPDs, pd.length, additionalPDs.length);
      //      for (i = 0; i < finalPDs.length; i++)
      //	System.out.println("WorkflowImplBeanInfo:" + finalPDs[i].getName());
      return finalPDs;
    } catch (Exception e) {
      System.out.println("Exception:" + e);
    }
    return null;
  }

}
