/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.logging;

import java.util.Properties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.LoggerControllerProxy;
import org.cougaar.util.log.LoggerFactory;
import org.cougaar.util.log.LoggerProxy;

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
 * @see org.cougaar.util.log.log4j.Log4jLoggerFactory
 */
public class LoggingServiceProvider implements ServiceProvider {
  private final LoggerFactory lf;

  /**
   * Create a LoggingServiceProvider and set the default logging
   * levels.
   */
  public LoggingServiceProvider() 
  {
    lf = LoggerFactory.getInstance();
  }

  /**
   * @param props Ignored. Retained for backwards compatability.
   * @deprecated Use the no-argument constructor.
   */
  public LoggingServiceProvider(Properties props) 
  {
    lf = LoggerFactory.getInstance();
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
      String name = requestor.getClass().getName();
      LoggerController lc = lf.createLoggerController(name);
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
