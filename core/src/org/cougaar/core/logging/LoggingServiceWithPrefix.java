/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerAdapter;

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
