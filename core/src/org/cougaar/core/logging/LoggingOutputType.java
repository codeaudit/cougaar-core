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

import org.cougaar.core.component.*;
import java.util.Enumeration;

/** a Service for controlling logging
 **/

public interface LoggingOutputType {

  public static final int CONSOLE = LoggingControlService.CONSOLE;
  public static final int STREAM  = LoggingControlService.STREAM;
  public static final int FILE    = LoggingControlService.FILE;

  public String getNode();
  public int getOutputType();
  public String getOutputDevice();
  public int getLoggingLevel();

}




