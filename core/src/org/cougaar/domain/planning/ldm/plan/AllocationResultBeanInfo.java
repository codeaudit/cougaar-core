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

/**
   Override the default property descriptors.
   A property descriptor contains:
   attribute name, bean class, read method name, write method name
   All other beaninfo is defaulted.
   This defines appropriate properties from the Task INTERFACE,
   but is actually used to introspect on the Task IMPLEMENTATION.
*/

public class AllocationResultBeanInfo extends SimpleBeanInfo {

  // return appropriate properties from AllocationResult.java
  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[6];
    int i = 0;
    try {
      Class beanClass = Class.forName("org.cougaar.domain.planning.ldm.plan.AllocationResult");
      pd[i++] = new IndexedPropertyDescriptor("aspectTypes",
					      beanClass,
					      "getAspectTypesAsArray", null,
					      "getAspectTypeFromArray", null);
      pd[i++] = new IndexedPropertyDescriptor("results",
					      beanClass,
					      "getResultsAsArray", null,
					      "getResultFromArray", null);
      pd[i++] = new IndexedPropertyDescriptor("phasedResults",
					      beanClass,
					      "getPhasedResultsAsArray", null,
					      "getPhasedResultsFromArray", null);
      pd[i++] = new PropertyDescriptor("confidenceRating",
				       beanClass,
				       "getConfidenceRating",
				       null);

      pd[i++] = new PropertyDescriptor("success",
				       beanClass,
				       "isSuccess",
				       null);

      pd[i++] = new PropertyDescriptor("phased",
				       beanClass,
				       "isPhased",
				       null);

      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(beanClass.getSuperclass()).getPropertyDescriptors();
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
