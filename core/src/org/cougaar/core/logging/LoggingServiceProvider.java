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
 * This service provider provides the {@link LoggingService}, which
 * is backed by the {@link LoggerFactory}.
 */
public class LoggingServiceProvider implements ServiceProvider {

  /** The cache for getLogger() */
  private static Map loggerCache = new WeakHashMap(11);

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

  public void releaseService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass,
      Object service) {
  }

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
