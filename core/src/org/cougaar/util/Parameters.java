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

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * COUGAAR Parameter String utilities.
 * Parameters may be specifed in the form "NAME" or "NAME:DEFAULT"
 * The NAME will be looked up in a passed-in parameter table (if supplied),
 * then system properties and then the default.
 *
 **/
public class Parameters {

  private static HashMap parameterMap = new HashMap(89);

  static {
    String home = System.getProperty("user.home");
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(ConfigFileFinder.open(".alprc")));
      int l = 0;
      String line;
      while ((line = br.readLine()) != null) {
        l++;
        if (line.startsWith("#") ||
            line.startsWith(";") ||
            line.length() == 0)
          continue;
        try {
          int i = line.indexOf("=");
          String param = line.substring(0, i).trim();
          String value = line.substring(i+1).trim();
          parameterMap.put(param, value);
        } catch (RuntimeException re) {
          System.err.println("Badly formed line in \".alprc\" ("+l+"):\n"+line);
        }
      }
      br.close();
    } catch (Exception e) { 
      System.err.println("Warning: Could not find \".alprc\" anywhere in the Config Path.");
      // ignore
    }
  }

  private static String OPEN = "${";
  private static String CLOSE = "}";

  /** Replace occurances of ${PARAM} in the argument with the result
   * of calling findParameter("PARAM");
   * Note: this is ugly, slow, inflexible and wastes lots of memory.
   * This code handles recursive expansions, but not nested parameter references.
   * Example: "${FOO}" -> "This is a ${BAR}" -> "This is a test" 
   * but not "${FOO:${BAR}}"
   **/
  public static String replaceParameters(String arg, Map map) {
    if (arg == null) return null;
    if (arg.indexOf(OPEN) == -1) return arg; // bail out quickly

    StringBuffer buf = new StringBuffer(arg);
    int i;
    int l = buf.length();
    while ((i=sbIndexOf(buf, OPEN, 0, l)) != -1) {
      int j = sbIndexOf(buf, CLOSE, i+2, l);
      if (j == -1) {
        throw new RuntimeException("Unclosed Parameter in '"+buf+"'");
      }
      String name = buf.substring(i+2, j);
      String param = findParameter(name, map);
      if (param == null) {
        throw new RuntimeException("Cannot find value for parameter '"+name+"'");
      }
      buf.replace(i, j+1, param);
      l = buf.length();
    }
    return buf.toString();
  }

  public static String replaceParameters(String arg) {
    return replaceParameters(arg, null);
  }

  /** find a string pattern in the buffer, returning -1 if not found. **/
  private static int sbIndexOf(StringBuffer buf, String pat, int s, int e) {
    int pl = pat.length();
    int f = 0;
    for (int p=s; p<e; p++) {
      if (buf.charAt(p)==pat.charAt(f)) {
        f++;
        if (f==pl) return (p-f+1);
      } else {
        f=0;
      }
    }
    return -1;
  }

  /** Look in various places for the value of a parameter.
   * param is either of the form "PARAM" or "PARAM:defval"
   * If no parameter is found and no defval is supplied, will return null.
   * The places looked at to find values are (in order):
   *  the extra argument map (if supplied)
   *  the static parameter map (supplied by ConfigFileFinder/.alprc);
   *  the System properties
   *  any default value.
   **/
  public static String findParameter(String param, Map map) {
    String defval = null;
    int di = param.indexOf(":");
    if (di>-1) {
      defval = param.substring(di+1);
      param = param.substring(0, di);
    }
    
    // check our argument map
    if (map != null) {
      Object o = map.get(param);
      if (o != null) return o.toString();
    }

    if (parameterMap != null) {
      Object o = parameterMap.get(param);
      if (o != null) return o.toString();
    }

    // check the System properties
    String v = System.getProperty(param);
    if (v != null && v.length()>0) return v;

    // use defval if we've got it
    if (defval != null) return defval;

    return null;
  }

  public static String findParameter(String param) {
    return findParameter(param, null);
  }

  public static void main(String argv[]) {
    for (int i = 0; i <argv.length; i++) {
      String arg = argv[i];
      System.out.print(arg+" -> ");
      try {
        System.out.println(replaceParameters(arg));
      } catch (RuntimeException re) {
        System.out.println(re);
      }
    }
  }
}
