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

import java.util.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/** 
 * Package-private log4j class to initialize the logging
 * configuration.
 */
class Initializer {

  public static void configure(Properties props) {
    // flatten props to map
    if (props != null) {
      Map m = new HashMap();
      for (Enumeration en = props.propertyNames();
          en.hasMoreElements();
          ) {
        String name = (String) en.nextElement();
        String value = props.getProperty(name);
        m.put(name, value);
      }
      configure(m);
    }
  }

  /**
   * Configure the factory, which sets the initial
   * logging configuration (levels, destinations, etc).
   */
  public static void configure(Map m) {
    if (m instanceof Properties) {
      configure((Properties) m);
    } else {
      int n = ((m != null) ? m.size() : 0);
      if (n > 0) {
        // log4j uses properties
        Properties props = new Properties();
        props.putAll(m);
        PropertyConfigurator.configure(props);
      } else {
        BasicConfigurator.configure();
      }
    }
  }
}
