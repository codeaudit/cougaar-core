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

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.NullLogger;

/**
 * LoggingService where all "is*()" methods return
 * false, and all "log()" methods are ignored.
 * <p>
 * This is handle if<pre> 
 *   serviceBroker.getService(.., LoggingService.class, ..);
 * </pre>
 * returns null.
 */
public final class NullLoggingServiceImpl 
  extends NullLogger
  implements LoggingService 
{
  // singleton:
  private static final NullLoggingServiceImpl SINGLETON = new NullLoggingServiceImpl();

  /** @deprecated old version of getLoggingService() **/
  public static NullLoggingServiceImpl getNullLoggingServiceImpl() {
    return SINGLETON;
  }

  /** @return a singleton instance of the NullLoggingService **/
  public static LoggingService getLoggingService() { return SINGLETON; }
}
