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

import org.cougaar.core.service.*;

import org.cougaar.core.component.*;
import java.util.Enumeration;

/*
 ** an interface for accessing the various
 ** logging device,type,logging level given 
 ** a particular node.   
 ** See {@link LoggingControlService#getOutputTypes}
 ** for when it is used.
 **
 ** @serialField CONSOLE int An output type for outputting to console.
 ** @serialField STREAM int An output type for outputting to stream.
 ** @serialField FILE int An output type for outputting to file.
 */

public interface LoggingOutputType {

  public static final int CONSOLE = LoggingControlService.CONSOLE;
  public static final int STREAM  = LoggingControlService.STREAM;
  public static final int FILE    = LoggingControlService.FILE;

    /* 
     ** @return The node name string for this output device
     */
     public String getNode();

    /* 
     ** @return The output type this one output at the node above.
     ** could be one of many.  Look at {@link LoggingControlService} either
     ** CONSOLE,STREAM, or FILE.
     */
  public int getOutputType();

    /** 
     ** @return The device related string for this output type.
     ** empty string for CONSOLE, path/filename for FILE, and identifier
     ** for a STREAM.
     **/
  public String getOutputDevice();

    /** 
     **   @return The logging level for this particular node.  Is the same
     **   for every other output at this node.   Node-wide variable.
     **   {@link LoggingService} for values (DEBUG,INFO,LEGACY,WARNING,
     **   ERROR, and FATAL).
     **/
  public int getLoggingLevel();

}




