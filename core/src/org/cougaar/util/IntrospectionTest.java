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
 
package org.cougaar.util;

import java.beans.*;

/** Class for testing Bean introspection in general and 
 * Asset/PG introspection in particular.  Essentially
 * loads a class as specified by the arglist, introspects 
 * on the class, and prints the results.
 **/

public class IntrospectionTest {
  public static void main(String args[]) {
    for (int ci = 0; ci < args.length; ci++) {
      String cname = args[ci];
      System.out.println(cname+": ");
      try {
        Class c = Class.forName(cname);
        BeanInfo bi = Introspector.getBeanInfo(c);
        PropertyDescriptor pds[] = bi.getPropertyDescriptors();
        for (int pi = 0; pi < pds.length; pi++) {
          PropertyDescriptor pd = pds[pi];
          System.out.println("\t"+pd.getReadMethod());
        }
      } catch (Exception e) {
        System.out.println("Caught "+e);
        e.printStackTrace();
      } 
    }
  }
}
