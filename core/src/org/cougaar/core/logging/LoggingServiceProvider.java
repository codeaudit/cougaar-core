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

import java.util.Map;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.component.*;

import org.cougaar.util.log.*;

/**
 * This LoggingServiceProvider is a ServiceProvider which provides 
 * two services: the LoggingService and LoggingControlService.
 * 
 * The LoggingService is used by developers to write log statements.
 *
 * The LoggingControlService is used by privledged Components 
 * to alter the logging levels and add/remove logging destinations.
 *
 * @see Logger
 * @see LoggerController
 */
public class LoggingServiceProvider implements ServiceProvider {

  private final LoggerFactory lf;

  /**
   * Create a LoggingServiceProvider and set the default logging
   * levels.
   *
   * @param env The environment variables which can be used to 
   *            configure how logging is performed.  May be 
   *            passed as null.
   */
  public LoggingServiceProvider(Map env) {
    this.lf = LoggerFactory.getInstance();
    lf.configure(env);
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
