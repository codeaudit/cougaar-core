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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class PrepositionalPhraseImplBeanInfo extends SimpleBeanInfo {

   /**
    Override the default property descriptors.
    A property descriptor contains:
    attribute name, attribute return value, read method name, write method name.
    Property descriptors returned by this method are:
    indirectObject, Object, getIndirectObject, null
    preposition, String, getPreposition, null

    All other beaninfo is defaulted; that is,
    the Java Introspector is used to determine
    the rest of the information about this implementation.
   */

   public PropertyDescriptor[] getPropertyDescriptors() {
     PropertyDescriptor[] pd = new PropertyDescriptor[2];
     try {
       pd[0] = new PropertyDescriptor("preposition",
          Class.forName("org.cougaar.domain.planning.ldm.plan.PrepositionalPhraseImpl"),
				      "getPreposition", null);
       pd[1] = new PropertyDescriptor("indirectObject",
          Class.forName("org.cougaar.domain.planning.ldm.plan.PrepositionalPhraseImpl"),
				      "getIndirectObject", null);
     } catch (IntrospectionException ie) {
       System.out.println(ie);
     } catch (ClassNotFoundException ce) {
       System.out.println(ce);
     }
     return pd;
   }
}
