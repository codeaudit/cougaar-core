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

import java.util.Map;
import java.util.Properties;

import org.cougaar.util.log.*;

/** 
 * Log4j implementation of LoggerFactory, which is used to
 * create Logger and LoggerController instances.
 * <p>
 * Typically the "requestor" classname is used to identify 
 * loggers.  A special "name" is "root", which is used
 * to specify the root (no-parent) logger.
 */
public class Log4jLoggerFactory extends LoggerFactory {

  public void configure(Properties props) {
    Initializer.configure(props);
  }

  public void configure(Map m) {
    Initializer.configure(m);
  }

  public Logger createLogger(Object requestor) {
    return createLogger(requestor.getClass().getName());
  }

  public Logger createLogger(String name) {
    return new LoggerImpl(name);
  }

  public LoggerController createLoggerController(
      Object requestor) {
    return createLoggerController(requestor.getClass().toString());
  }

  public LoggerController createLoggerController(
      String name) {
    return new LoggerControllerImpl(name);
  }
}
