/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.logging;

import org.cougaar.util.log.LoggerAdapter;
import org.cougaar.util.log.Logger;
import org.cougaar.core.service.LoggingService;

public class LoggingServiceWithPrefix extends LoggerAdapter implements LoggingService {
  public static LoggingService add(LoggingService ls, String prefix) {
    return new LoggingServiceWithPrefix(ls, prefix);
  }

  private String prefix;
  private Logger logger;
  public LoggingServiceWithPrefix(Logger logger, String prefix) {
    this.logger = logger;
    this.prefix = prefix;
  }
  public boolean isEnabledFor(int level) { return logger.isEnabledFor(level); }
  public void log(int level, String message, Throwable t) { logger.log(level, prefix + message, t); }
}  
