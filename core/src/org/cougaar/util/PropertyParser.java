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

import java.util.*;

/**
 * Utility for parsing Properties values of various types with defaults
 **/

public abstract class PropertyParser {
  public static final boolean getBoolean(Properties props, String prop, boolean def) {
    return (Boolean.valueOf(props.getProperty(prop, String.valueOf(def)))).booleanValue();
  }

  public static final boolean getBoolean(String prop, boolean def) {
    return getBoolean(System.getProperties(), prop, def);
  }

  public static final int getInt(Properties props, String prop, int def) {
    try {
      return Integer.parseInt(props.getProperty(prop, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }
  public static final int getInt(String prop, int def) {
    return getInt(System.getProperties(), prop, def);
  }

  public static final long getLong(Properties props, String prop, long def) {
    try {
      return Long.parseLong(props.getProperty(prop, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }

  }
  public static final long getLong(String prop, long def) {
    return getLong(System.getProperties(), prop, def);
  }

  public static final float getFloat(Properties props, String prop, float def) {
    try {
      return Float.parseFloat(props.getProperty(prop, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }
  public static final float getFloat(String prop, float def) {
    return getFloat(System.getProperties(), prop, def);
  }

  public static final double getDouble(Properties props, String prop, double def) {
    try {
      return Double.parseDouble(props.getProperty(prop, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }
  public static final double getDouble(String prop, double def) {
    return getDouble(System.getProperties(), prop, def);
  }
}
