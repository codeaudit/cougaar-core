/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.logging;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.component.*;

import org.cougaar.util.log.*;

import org.cougaar.util.ConfigFinder;

/**
 * This LoggingServiceProvider is a ServiceProvider which provides
 * two services<ol><p>
 *   <li>LoggingService<br>
 *       Used by developers to write log statements</li><p>
 *   <li>LoggingControlService<br>
 *       Used by privledged Components to alter the logging levels
 *       and add/remove logging destinations</li>
 * </ol>.
 * <p>
 * System properties that start with "org.cougaar.core.logging."
 * are used to configure the logger.  The
 * "org.cougaar.core.logging." prefix is removed before the
 * properties are passed off to the LoggerFactory.
 * <p>
 * One special property is the
 * "org.cougaar.core.logging.config.filename",
 * which is used to (optionally) load a second properties
 * file.  The properties in this file should not be prefixed
 * with the "org.cougaar.core.logging." prefix.
 * <p>
 * The javadocs for <tt>LoggerFactory.configure(Map)</tt> define
 * the valid logging configuration properties.
 *
 * @see Logger
 * @see LoggerController
 *
 * <pre>
 * @property org.cougaar.core.logging.config.filename
 *    Load logging properties from the named file, which is
 *    found using the ConfigFinder.  Currently uses log4j-style
 *    properties; see
 *    <a href="http://jakarta.apache.org/log4j/docs/manual.html"
 *    >the log4j manual</a> for valid file contents.
 * @property org.cougaar.core.logging.*
 *    Non-"config.filename" properties are stripped of their 
 *    "org.cougaar.core.logging." prefix and passed to the
 *    logger configuration.  These properties override any 
 *    properties defined in the (optional) 
 *    "org.cougaar.core.logging.config.filename=STRING" 
 *    property.
 * </pre>
 */
public class LoggingServiceProvider implements ServiceProvider {

  private static final String PREFIX = "org.cougaar.core.logging.";
  private static final int PREFIX_LENGTH;
  static {
    PREFIX_LENGTH = PREFIX.length();
  }
  private static final String FILE_NAME_PROPERTY =
    PREFIX + "config.filename";

  private final LoggerFactory lf;

  /**
   * Create a LoggingServiceProvider and set the default logging
   * levels.
   *
   * @param props "org.cougaar.core.logging." system properties
   *    to configure the logger.  In particular,
   *    "org.cougaar.core.logging.config.filename=STRING"
   *    will read properties from a file.
   */
  public LoggingServiceProvider(
      Properties props) throws IOException {
    Map m = null;
    Enumeration en;
    if ((props != null) &&
        ((en = props.propertyNames()).hasMoreElements())) {
      m = new HashMap();
      // take filename property, load from file
      String filename = props.getProperty(FILE_NAME_PROPERTY);
      if (filename != null) {
        ConfigFinder configFinder = ConfigFinder.getInstance();
        InputStream in = configFinder.open(filename);
        Properties tmpP = new Properties();
        tmpP.load(in);
        m.putAll(tmpP);
      }
      // override with other properties
      while (en.hasMoreElements()) {
        String name = (String) en.nextElement();
        if ((name == null) ||
            (name.equals(FILE_NAME_PROPERTY))) {
          continue;
        }
        // assert (name.startsWith(PREFIX))
        String value = props.getProperty(name);
        name = name.substring(PREFIX_LENGTH);
        m.put(name, value);
      }
    }

    this.lf = LoggerFactory.getInstance();
    lf.configure(m);
  }

  /**
   * Used to obtain either LoggingService or LoggingControlService.
   *
   * @param sb service broker
   * @param requestor The object requesting the service used to mark
   *                  the object category
   * @param serviceClass The service requested. It will be either
   *                     LoggingService or LoggingControlService.
   */
  public Object getService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass) {
    if (LoggingService.class.isAssignableFrom(serviceClass)) {
      Logger l = lf.createLogger(requestor);
      return new LoggingServiceImpl(l);
    } else if (LoggingControlService.class.isAssignableFrom(serviceClass)) {
      LoggerController lc = lf.createLoggerController(requestor);
      return new LoggingControlServiceImpl(lc);
    } else {
      return null;
    }
  }

  /**
   * Used to release either LoggingService or LoggingControlService.
   *
   * Currently does nothing because no resources need to be released.
   * However, users should aways call "releaseService(..)" in case
   * this implementation is modified...
   *
   * @param sb The ServiceBroker controlling this service
   * @param requestor The object requesting the service used to mark
   *                  the object category
   * @param serviceClass The service requested. It will be either
   *                     LoggingService or LoggingControlService.
   * @param service The actual service being released
   */
  public void releaseService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass,
      Object service) {
  }

  private static class LoggingServiceImpl
    extends LoggerProxy
    implements LoggingService {
      public LoggingServiceImpl(Logger l) {
        super(l);
      }
  }

  private static class LoggingControlServiceImpl
    extends LoggerControllerProxy
    implements LoggingControlService {
      public LoggingControlServiceImpl(LoggerController lc) {
        super(lc);
      }
  }

}
