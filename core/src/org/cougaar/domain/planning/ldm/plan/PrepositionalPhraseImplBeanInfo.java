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
