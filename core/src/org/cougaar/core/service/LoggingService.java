/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.util.log.Logger;

/** 
 * Abstract API for a Service that provides the
 * generic Logger API.
 *
 * @see Logger
 */

public interface LoggingService 
  extends Service, Logger
{
  LoggingService NULL = NullLoggingServiceImpl.getNullLoggingServiceImpl();

}  

