/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
