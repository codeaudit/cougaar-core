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

import org.cougaar.util.log.LoggerController;
import org.cougaar.core.component.Service;

/** 
 * Marker interface for a Service that provides the
 * generic LoggerController API.
 *
 * @see LoggerController
 */
public interface LoggingControlService extends LoggerController, Service {
}
