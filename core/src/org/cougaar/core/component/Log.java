/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

public class Log {
  public static void logMethod(Object o, String m, Object a) {
    System.err.println(o.toString()+"."+m+"("+a+")");
  }
  public static void logResult(Object o, String m, Object a) {
    System.err.println(o.toString()+"."+m+" = "+a);
  }
  public static void logMessage(Object o, String m, Object a) {
    System.err.println(o.toString()+"."+m+": "+a);
  }
}
