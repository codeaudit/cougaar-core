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

  /**
   * Default configuration only prints WARN or higher
   * statements.
   * <p>
   * A client with name "x.y.z" that calls:<pre>
   *   log.warn("test message");
   * </pre>
   * will generate a standard-output message similar to:</pre>
   *   2002-03-08 22:26:08,980 WARN [z] - test message
   * </pre>.
   * <p>
   * See the log4j docs for further details.
   */
  private static final String[][] DEFAULT_PROPS = {
    {"log4j.rootCategory", 
      "WARN,A1"
    },
    {"log4j.appender.A1", 
      "org.apache.log4j.ConsoleAppender"
    },
    {"log4j.appender.A1.layout", 
      "org.apache.log4j.PatternLayout"
    },
    {"log4j.appender.A1.layout.ConversionPattern", 
      "%d{ISO8601} %-5p [%c{1}] - %m%n"
    },
  };

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
      // log4j uses properties
      Properties props;
      if (n > 0) {
        props = new Properties();
        props.putAll(m);
      } else {
        props = getDefaultProperties();
      }
      PropertyConfigurator.configure(props);
    }
  }

  private static Properties DEFAULT_PROPERTIES;

  /**
   * convert "String[][] DEFAULT_PROPS" to Properties.
   */
  private static synchronized Properties getDefaultProperties() {
    if (DEFAULT_PROPERTIES == null) {
      Properties p = new Properties();
      try {
        for (int i = 0, n = DEFAULT_PROPS.length; i < n; i++) {
          p.put(DEFAULT_PROPS[i][0], DEFAULT_PROPS[i][1]);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      DEFAULT_PROPERTIES = p;
    }
    return DEFAULT_PROPERTIES;
  }

}
