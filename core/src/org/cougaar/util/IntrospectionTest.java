/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
