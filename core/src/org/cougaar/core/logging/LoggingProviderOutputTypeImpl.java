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

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.FileAppender;
import org.cougaar.core.component.*;
import java.util.Enumeration;

/*
 ** a Service for controlling logging
 **
 **
 ** An implemenation of a LoggingOutputType (See {@link LoggingOutputType}).
 ** Used by LoggingControlService @see LoggingControlService#getOutputTypes.
 **
 */

public class LoggingProviderOutputTypeImpl implements LoggingOutputType {

    private String nodeName;
    private int    outputType;
    private String outputDevice;
    private int    loggingLevel;

    public LoggingProviderOutputTypeImpl(String aNodeName,
					 int    anOutputType,
					 String anOutputDevice,
					 int    aLoggingLevel) {
      nodeName = aNodeName;
      outputType = anOutputType;
      outputDevice = anOutputDevice;
      loggingLevel = aLoggingLevel;
    }
    
    /*
     ** constructor takes a Log4j Appender for deriving information
     */
    public LoggingProviderOutputTypeImpl(String aNodeName,
					 int    aLoggingLevel,
					 Appender appender) {
	this(aNodeName,0,null,aLoggingLevel);
	
	if(appender instanceof FileAppender) {
	    outputType = FILE;
	    outputDevice = ((FileAppender) appender).getFile();
	}
	else if(appender instanceof ConsoleAppender) {
	    outputType = CONSOLE;
	    outputDevice = null;
	}
	else if(appender.getClass() ==  WriterAppender.class) {
	    outputType = STREAM;
	    outputDevice = appender.getName();
	}
	else {
	    throw new RuntimeException("Unknown Appender type");
	}
    }


    /** 
     ** @return The node name string for this output/device
     **/

    public String getNode() {return nodeName;}

    /** 
     ** @return The output type this one output at the node above.
     ** could be one of many.  See {@link LoggingControlService} either
     ** CONSOLE,STREAM, or FILE.
     **/

    public int getOutputType() {return outputType;}

    /** 
     ** @return The device related string for this output type.
     ** empty string for CONSOLE, path/filename for FILE, and identifier
     ** for a STREAM.
     **/

    public String getOutputDevice() {return outputDevice;}

    /** 
     **   @return The logging level for this particular node.  Is the same
     **   for every other output at this node.   Node-wide variable.
     **   See {@link LoggingService} for values (DEBUG,INFO,LEGACY,WARNING,
     **   ERROR, and FATAL).
     **/
    public int getLoggingLevel() {return loggingLevel;}
    
}




