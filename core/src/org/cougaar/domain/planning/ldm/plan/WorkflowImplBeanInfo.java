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
      pd[i++] = new PropertyDescriptor("constraints",
				       workflowClass,
				       "getConstraintsAsArray", 
				       null);
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
