/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log.log4j;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import java.util.Enumeration;

/** 
 * Package-private log4j class to initialize the logging
 * configuration.
 */
class Initializer {

  // configure map's entry names will be prefixed by:
  public static final String PROPERTY_NAME_PREFIX = 
    "org.cougaar.core.logging.";
  public static final int PROPERTY_NAME_PREFIX_LENGTH;
  static {
    PROPERTY_NAME_PREFIX_LENGTH = PROPERTY_NAME_PREFIX.length();
  }
  public static final String PROPERTY_NEW_PREFIX = "log4j.";

  /**
   * Configure the factory, which sets the initial
   * logging configuration (levels, destinations, etc).
   */
  public static void configure(Map env) {
    if (env == null) {
      BasicConfigurator.configure();
    } else {
      // strip off "org.cougaar.core.logging." name prefix
      Properties props = new Properties();

      for (Iterator iter = env.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        String value = (String) me.getValue();
        // assert (name.startsWith(PROPERTY_NAME_PREFIX);
        name = name.substring(PROPERTY_NAME_PREFIX_LENGTH);
        name = PROPERTY_NEW_PREFIX + name;
        props.put(name, value);
      }
      // configure with properties
      PropertyConfigurator.configure(props);
    }
  }
}
