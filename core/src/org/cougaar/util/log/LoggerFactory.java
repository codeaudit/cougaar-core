/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log;

import java.util.Map;
import java.util.Properties;

/** 
 * Factory to create Logger and LoggerController instances.
 * <p>
 * Typically the "requestor" classname is used to identify 
 * loggers.  A special "name" is "root", which is used
 * to specify the root (no-parent) logger.
 */
public abstract class LoggerFactory {

  public static LoggerFactory getInstance() {
    // only support log4j for now.
    //
    // this could easily be modified to load the log4j
    // factory by reflection, plus the "log4j/*"
    // classes could be moved to a separate module,
    // which would make "org.cougaar.util.log"
    // non-compile-dependent upon log4j.
    //
    return new org.cougaar.util.log.log4j.Log4jLoggerFactory();
  }

  /**
   * @see #configure(Map)
   */
  public abstract void configure(Properties props);

  /**
   * Configure the factory, which sets the initial
   * logging configuration (levels, destinations, etc).
   * <p>
   * This must be called prior to other "create*" methods.
   * <p>
   * Currently only "log4j" properties are used.  See
   * <a href="http://jakarta.apache.org/log4j/docs/manual.html">
   * the log4j manual</a> for details.
   */
  public abstract void configure(Map m);

  public abstract Logger createLogger(Object requestor);

  public abstract Logger createLogger(String name);

  public abstract LoggerController createLoggerController(
      Object requestor);

  public abstract LoggerController createLoggerController(
      String name);
}
