/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.logging;

import java.util.Properties;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.LoggerControllerProxy;
import org.cougaar.util.log.LoggerFactory;
import org.cougaar.util.log.LoggerProxy;

import java.util.Map;
import java.util.WeakHashMap;

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
  /**
   * Create a LoggingServiceProvider and set the default logging
   * levels.
   */
  public LoggingServiceProvider() { }

  /**
   * @param props Ignored. Retained for backwards compatability.
   * @deprecated Use the no-argument constructor.
   */
  public LoggingServiceProvider(Properties props) { }

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
      return getLoggingService(requestor);
    } else if (LoggingControlService.class.isAssignableFrom(serviceClass)) {
      LoggerController lc = Logging.getLoggerController(requestor);
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

  /** The cache for getLogger() **/
  private static Map loggerCache = new WeakHashMap(11);

  LoggingService getLoggingService(Object requestor) {
    String key = Logging.getKey(requestor);
    LoggingService ls;
    synchronized (loggerCache) {
      ls = (LoggingService) loggerCache.get(key);
      if (ls == null) {
        ls = new LoggingServiceImpl(Logging.getLogger(key));
        // new String(key) keeps the entry GCable
        loggerCache.put(new String(key),ls);
      }
    }
    return ls;
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
