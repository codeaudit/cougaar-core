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
